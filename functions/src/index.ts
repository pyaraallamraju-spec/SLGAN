import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import nodemailer from "nodemailer";
import PDFDocument from "pdfkit";

initializeApp();
const db = getFirestore();

const CASHFREE_APP_ID = defineSecret("CASHFREE_APP_ID");
const CASHFREE_SECRET_KEY = defineSecret("CASHFREE_SECRET_KEY");
const GMAIL_USER = defineSecret("GMAIL_USER");
const GMAIL_APP_PASSWORD = defineSecret("GMAIL_APP_PASSWORD");

type CartLine = { productId: string; quantity: number };
type Address = {
  fullName: string; phone: string; line1: string; line2?: string;
  city: string; state: string; postalCode: string;
};

const money = (value: number) => Math.round(value * 100) / 100;
const requiredText = (value: unknown, label: string, max = 200): string => {
  if (typeof value !== "string" || !value.trim() || value.trim().length > max) {
    throw new HttpsError("invalid-argument", `Valid ${label} is required.`);
  }
  return value.trim();
};

async function findProduct(productId: string) {
  const direct = await db.collection("products").doc(productId).get();
  if (direct.exists) return direct;
  const matches = await db.collection("products").where("id", "==", productId).limit(1).get();
  return matches.empty ? null : matches.docs[0];
}

async function makeInvoicePdf(order: any): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ size: "A4", margin: 48 });
    const chunks: Buffer[] = [];
    doc.on("data", (chunk: Buffer) => chunks.push(chunk));
    doc.on("end", () => resolve(Buffer.concat(chunks)));
    doc.on("error", reject);
    doc.fontSize(20).text("SLGAN Ayurvedic Nilayam");
    doc.moveDown(0.3).fontSize(10).text("Invoice / Order summary");
    doc.moveDown().text(`Invoice: ${order.invoiceNumber}`);
    doc.text(`Order: ${order.orderId}`);
    doc.text(`Date: ${new Date().toISOString()}`);
    doc.moveDown().fontSize(12).text("Bill to");
    doc.fontSize(10).text(order.customerName);
    doc.text(order.customerEmail);
    const a = order.shippingAddress;
    doc.text([a.line1, a.line2, a.city, a.state, a.postalCode].filter(Boolean).join(", "));
    doc.moveDown().fontSize(12).text("Items", { underline: true });
    doc.moveDown(0.5).fontSize(10);
    for (const item of order.items) {
      doc.text(`${item.name}  x ${item.quantity}  @ ₹${item.unitPrice.toFixed(2)}  = ₹${item.lineTotal.toFixed(2)}`);
    }
    doc.moveDown().text(`Subtotal: ₹${order.subtotal.toFixed(2)}`);
    doc.text(`Shipping: ₹${order.shippingCharges.toFixed(2)}`);
    doc.text(`Discount: ₹${order.discount.toFixed(2)}`);
    doc.fontSize(13).text(`Total payable: ₹${order.totalAmount.toFixed(2)}`);
    doc.moveDown().fontSize(10).text(`Payment status: ${order.paymentStatus}`);
    doc.moveDown().text("This document is an order invoice/summary. It is not proof of payment unless the payment status is PAID.");
    doc.end();
  });
}

async function emailInvoice(order: any, pdf: Buffer, paid: boolean) {
  const user = GMAIL_USER.value();
  const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: { user, pass: GMAIL_APP_PASSWORD.value() },
  });
  await transporter.sendMail({
    from: `SLGAN Ayurvedic Nilayam <${user}>`,
    to: order.customerEmail,
    subject: paid ? `Payment confirmed — ${order.orderId}` : `Order received — ${order.orderId} (payment pending)`,
    text: paid
      ? `Thank you. Payment for order ${order.orderId} is confirmed. The invoice/receipt is attached.`
      : `We received order ${order.orderId}. Payment is currently pending. Please complete payment in the app. The order summary is attached; it is not proof of payment.`,
    attachments: [{ filename: `${order.invoiceNumber}.pdf`, content: pdf, contentType: "application/pdf" }],
  });
}

async function cashfreeRequest(path: string, init: RequestInit = {}) {
  const response = await fetch(`https://sandbox.cashfree.com/pg${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "x-api-version": "2023-08-01",
      "x-client-id": CASHFREE_APP_ID.value(),
      "x-client-secret": CASHFREE_SECRET_KEY.value(),
      ...(init.headers || {}),
    },
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    console.error("Cashfree API error", response.status, JSON.stringify(body));
    throw new HttpsError("failed-precondition", "Cashfree could not process this request. Please try again.");
  }
  return body as any;
}

/**
 * Creates a pending Firestore order, creates a Cashfree Sandbox payment session,
 * and emails a clearly-labelled payment-pending PDF summary.
 * Client supplies only product IDs/quantities and delivery address; prices are read server-side.
 */
export const createCheckoutOrder = onCall(
  { secrets: [CASHFREE_APP_ID, CASHFREE_SECRET_KEY, GMAIL_USER, GMAIL_APP_PASSWORD], timeoutSeconds: 60 },
  async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Please sign in before checkout.");
    const uid = request.auth.uid;
    const email = request.auth.token.email;
    if (typeof email !== "string" || !email.includes("@")) {
      throw new HttpsError("failed-precondition", "Add a verified email address to your account before checkout.");
    }

    const rawItems = request.data?.items;
    if (!Array.isArray(rawItems) || rawItems.length < 1 || rawItems.length > 50) {
      throw new HttpsError("invalid-argument", "Your cart is empty or invalid.");
    }
    const seen = new Set<string>();
    const items: any[] = [];
    for (const raw of rawItems as CartLine[]) {
      const productId = requiredText(raw?.productId, "product ID", 160);
      const quantity = Number(raw?.quantity);
      if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99 || seen.has(productId)) {
        throw new HttpsError("invalid-argument", "Cart quantities are invalid.");
      }
      seen.add(productId);
      const snap = await findProduct(productId);
      if (!snap || !snap.exists) throw new HttpsError("not-found", `A product in your cart is no longer available.`);
      const data = snap.data()!;
      const name = requiredText(data.product_name ?? data.name, "product name", 200);
      const price = Number(String(data.product_price ?? data.price ?? "").replace(/,/g, ""));
      if (!Number.isFinite(price) || price <= 0) {
        throw new HttpsError("failed-precondition", `Price is unavailable for ${name}.`);
      }
      items.push({ productId, name, quantity, unitPrice: money(price), lineTotal: money(price * quantity) });
    }

    const rawAddress = request.data?.shippingAddress || {};
    const shippingAddress: Address = {
      fullName: requiredText(rawAddress.fullName, "recipient name"),
      phone: requiredText(rawAddress.phone, "phone number", 30),
      line1: requiredText(rawAddress.line1, "address line"),
      line2: typeof rawAddress.line2 === "string" ? rawAddress.line2.trim().slice(0, 200) : "",
      city: requiredText(rawAddress.city, "city"),
      state: requiredText(rawAddress.state, "state"),
      postalCode: requiredText(rawAddress.postalCode, "postal code", 12),
    };
    const subtotal = money(items.reduce((sum, item) => sum + item.lineTotal, 0));
    // Configure actual shipping/tax/discount rules here before production.
    const shippingCharges = 0;
    const discount = 0;
    const totalAmount = money(subtotal + shippingCharges - discount);
    if (totalAmount < 1 || totalAmount > 1000000) {
      throw new HttpsError("failed-precondition", "Order total is outside the allowed payment range.");
    }

    const orderRef = db.collection("orders").doc();
    const orderId = `SLGAN_${Date.now()}_${orderRef.id.slice(0, 8)}`;
    const invoiceNumber = `SLGAN-INV-${Date.now()}-${orderRef.id.slice(0, 6).toUpperCase()}`;
    const order = {
      orderId, userId: uid, customerName: shippingAddress.fullName, customerEmail: email,
      phone: shippingAddress.phone, shippingAddress, items, subtotal, shippingCharges,
      discount, totalAmount, currency: "INR", paymentStatus: "PENDING",
      orderStatus: "PLACED", cashfreeOrderId: orderId, invoiceNumber,
      invoiceEmailStatus: "PENDING", createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    };
    await orderRef.set(order);

    try {
      const payment = await cashfreeRequest("/orders", {
        method: "POST",
        body: JSON.stringify({
          order_id: orderId,
          order_amount: totalAmount,
          order_currency: "INR",
          customer_details: {
            customer_id: uid.slice(0, 50),
            customer_name: shippingAddress.fullName.slice(0, 100),
            customer_email: email.slice(0, 100),
            customer_phone: shippingAddress.phone.replace(/\D/g, "").slice(-10) || "9999999999",
          },
          order_meta: { return_url: "https://www.cashfree.com/devstudio/preview/pg/mobile/android?order_id={order_id}" },
          order_note: `SLGAN order ${orderId}`,
        }),
      });
      await orderRef.update({
        cashfreePaymentSessionId: payment.payment_session_id,
        cashfreeOrderStatus: payment.order_status || "ACTIVE",
        updatedAt: FieldValue.serverTimestamp(),
      });
      const emailOrder = { ...order, createdAt: new Date().toISOString() };
      try {
        const pdf = await makeInvoicePdf(emailOrder);
        await emailInvoice(emailOrder, pdf, false);
        await orderRef.update({ invoiceEmailStatus: "SENT", updatedAt: FieldValue.serverTimestamp() });
      } catch (emailError) {
        console.error("Invoice email failed", emailError);
        await orderRef.update({ invoiceEmailStatus: "FAILED", updatedAt: FieldValue.serverTimestamp() });
      }
      return {
        orderId, firestoreOrderId: orderRef.id,
        paymentSessionId: payment.payment_session_id,
        paymentStatus: "PENDING",
        message: "Order saved. Complete payment using Cashfree Sandbox.",
      };
    } catch (error) {
      await orderRef.update({ paymentStatus: "PENDING", orderStatus: "PAYMENT_SETUP_FAILED", updatedAt: FieldValue.serverTimestamp() });
      throw error;
    }
  }
);

/** Verify the order status against Cashfree's server API; never trust client success callbacks. */
export const verifyCheckoutPayment = onCall(
  { secrets: [CASHFREE_APP_ID, CASHFREE_SECRET_KEY, GMAIL_USER, GMAIL_APP_PASSWORD], timeoutSeconds: 60 },
  async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Please sign in.");
    const orderId = requiredText(request.data?.orderId, "order ID", 100);
    const query = await db.collection("orders").where("orderId", "==", orderId).limit(1).get();
    if (query.empty) throw new HttpsError("not-found", "Order not found.");
    const ref = query.docs[0].ref;
    const order = query.docs[0].data();
    if (order.userId !== request.auth.uid) throw new HttpsError("permission-denied", "This order does not belong to your account.");

    const payment = await cashfreeRequest(`/orders/${encodeURIComponent(orderId)}`, { method: "GET" });
    const paid = payment.order_status === "PAID";
    await ref.update({
      cashfreeOrderStatus: payment.order_status || "UNKNOWN",
      paymentStatus: paid ? "PAID" : "PENDING",
      orderStatus: paid ? "CONFIRMED" : (order.orderStatus || "PLACED"),
      paidAt: paid ? FieldValue.serverTimestamp() : (order.paidAt || null),
      updatedAt: FieldValue.serverTimestamp(),
    });

    if (paid && order.invoiceEmailStatus !== "PAID_EMAIL_SENT") {
      const paidOrder = { ...order, paymentStatus: "PAID", createdAt: order.createdAt?.toDate?.()?.toISOString?.() || new Date().toISOString() };
      try {
        const pdf = await makeInvoicePdf(paidOrder);
        await emailInvoice(paidOrder, pdf, true);
        await ref.update({ invoiceEmailStatus: "PAID_EMAIL_SENT", updatedAt: FieldValue.serverTimestamp() });
      } catch (emailError) {
        console.error("Paid confirmation email failed", emailError);
        await ref.update({ invoiceEmailStatus: "PAID_EMAIL_FAILED", updatedAt: FieldValue.serverTimestamp() });
      }
    }
    return { orderId, paymentStatus: paid ? "PAID" : "PENDING", cashfreeOrderStatus: payment.order_status };
  }
);
