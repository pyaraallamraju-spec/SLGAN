const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getStorage } = require("firebase-admin/storage");
const nodemailer = require("nodemailer");
const PDFDocument = require("pdfkit");

initializeApp();
const db = getFirestore();
const GMAIL_ADDRESS = defineSecret("GMAIL_ADDRESS");
const GMAIL_APP_PASSWORD = defineSecret("GMAIL_APP_PASSWORD");

const clean = (v, max = 200) => typeof v === "string" ? v.trim().slice(0, max) : "";
const money = v => Math.round((Number(v) + Number.EPSILON) * 100) / 100;

function makePdf(order) {
  return new Promise((resolve, reject) => {
    const pdf = new PDFDocument({ size: "A4", margin: 48 });
    const chunks = [];
    pdf.on("data", c => chunks.push(c));
    pdf.on("end", () => resolve(Buffer.concat(chunks)));
    pdf.on("error", reject);
    pdf.fontSize(18).fillColor("#762A24").text("Sri Lakshmi Ganapathi Ayurvedic Nilayam");
    pdf.moveDown().fontSize(12).fillColor("#38251F").text("ORDER CONFIRMATION / PROVISIONAL INVOICE");
    pdf.moveDown().fontSize(10).text("Order: " + order.orderId);
    pdf.text("Customer: " + order.customerName);
    pdf.text("Email: " + order.email);
    if (order.phone) pdf.text("Phone: " + order.phone);
    pdf.text("Delivery address: " + order.shippingAddress);
    pdf.moveDown().fontSize(12).text("Items");
    for (const item of order.items) {
      pdf.fontSize(10).text(item.name + " | Qty " + item.quantity + " | Rs. " +
        item.unitPrice.toFixed(2) + " | Rs. " + item.lineTotal.toFixed(2));
    }
    pdf.moveDown().fontSize(12).text("Product total: Rs. " + order.grandTotal.toFixed(2));
    pdf.moveDown().fontSize(9).text("No online payment was collected. This document confirms receipt of the order request. Delivery charges, applicable taxes and the final tax invoice, if required, will be confirmed by the seller.");
    pdf.end();
  });
}

exports.placeOrder = onCall({
  region: "asia-south1",
  secrets: [GMAIL_ADDRESS, GMAIL_APP_PASSWORD],
  timeoutSeconds: 120,
  memory: "512MiB"
}, async request => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Please sign in before placing an order.");
  const input = request.data || {};
  const customerName = clean(input.customerName, 120);
  const email = clean(input.email, 254).toLowerCase();
  const phone = clean(input.phone, 30);
  const shippingAddress = clean(input.shippingAddress, 600);
  const rawItems = Array.isArray(input.items) ? input.items : [];
  if (!customerName || !email.includes("@") || !shippingAddress)
    throw new HttpsError("invalid-argument", "Name, valid email and delivery address are required.");
  if (!rawItems.length || rawItems.length > 50)
    throw new HttpsError("invalid-argument", "Cart must contain 1 to 50 products.");

  const items = [];
  for (const raw of rawItems) {
    const productId = clean(raw && raw.productId, 150);
    const quantity = Number(raw && raw.quantity);
    if (!productId || !Number.isInteger(quantity) || quantity < 1 || quantity > 100)
      throw new HttpsError("invalid-argument", "A cart item has an invalid product ID or quantity.");
    const snap = await db.collection("products").doc(productId).get();
    if (!snap.exists) throw new HttpsError("failed-precondition", "A product is no longer available.");
    const product = snap.data();
    const unitPrice = Number(String(product.product_price ?? "").replace(/,/g, ""));
    const name = clean(product.product_name, 180);
    if (!name || !Number.isFinite(unitPrice) || unitPrice < 0)
      throw new HttpsError("failed-precondition", "A product has invalid catalogue data.");
    items.push({ productId, name, category: clean(product.product_category, 80),
      quantity, unitPrice: money(unitPrice), lineTotal: money(unitPrice * quantity) });
  }

  const subtotal = money(items.reduce((s, i) => s + i.lineTotal, 0));
  const ref = db.collection("orders").doc();
  const orderId = "SLGAN-" + ref.id.slice(0, 10).toUpperCase();
  const order = {
    orderId, customerId: request.auth.uid, customerName, email, phone, shippingAddress,
    items, subtotal, deliveryCharges: null, grandTotal: subtotal,
    paymentMethod: "none", paymentStatus: "not_applicable", orderStatus: "received",
    invoiceStatus: "generating", invoiceStoragePath: null, invoiceEmailStatus: "pending",
    createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()
  };
  await ref.set(order);

  try {
    const pdf = await makePdf(order);
    const storagePath = "invoices/" + ref.id + ".pdf";
    await getStorage().bucket().file(storagePath).save(pdf, {
      resumable: false, contentType: "application/pdf",
      metadata: { cacheControl: "private, max-age=0, no-transform" }
    });
    const [signedInvoiceUrl] = await getStorage().bucket().file(storagePath).getSignedUrl({
      action: "read",
      expires: Date.now() + 7 * 24 * 60 * 60 * 1000
    });
    const gmail = GMAIL_ADDRESS.value();
    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: { user: gmail, pass: GMAIL_APP_PASSWORD.value().replace(/\\s/g, "") }
    });
    await transporter.sendMail({
      from: '"Sri Lakshmi Ganapathi Ayurvedic Nilayam" <' + gmail + '>',
      to: email,
      subject: "SLGAN order confirmation " + orderId,
      text: "Thank you for placing your order. Your order confirmation PDF is attached. No online payment was collected. We will confirm delivery charges and dispatch details separately.",
      attachments: [{ filename: orderId + ".pdf", content: pdf, contentType: "application/pdf" }]
    });
    await ref.update({ invoiceStatus: "generated", invoiceStoragePath: storagePath,
      invoiceUrl: signedInvoiceUrl, invoiceEmailStatus: "sent", updatedAt: FieldValue.serverTimestamp() });
    return { success: true, orderId, invoiceUrl: signedInvoiceUrl, message: "Order placed and confirmation emailed." };
  } catch (err) {
    console.error("Invoice/email processing failed for order", orderId, err.message);
    await ref.update({ invoiceStatus: "failed", invoiceEmailStatus: "failed",
      updatedAt: FieldValue.serverTimestamp() });
    return { success: true, orderId,
      message: "Order saved. Invoice email failed; the shop must retry it." };
  }
});
