const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getStorage } = require("firebase-admin/storage");
const nodemailer = require("nodemailer");
const PDFDocument = require("pdfkit");
const path = require("path");

initializeApp();
const db = getFirestore();
const GMAIL_ADDRESS = defineSecret("GMAIL_ADDRESS");
const GMAIL_APP_PASSWORD = defineSecret("GMAIL_APP_PASSWORD");

const clean = (v, max = 200) => typeof v === "string" ? v.trim().slice(0, max) : "";
const money = v => Math.round((Number(v) + Number.EPSILON) * 100) / 100;

function makePdf(order) {
  return new Promise((resolve, reject) => {
    const pdf = new PDFDocument({ size: "A4", margins: { top: 42, bottom: 48, left: 42, right: 42 }, bufferPages: true });
    const chunks = [];
    const pageWidth = 511;
    const left = 42;
    const ink = "#252B2A";
    const muted = "#69736F";
    const green = "#244F43";
    const pale = "#F1F5F2";
    const line = "#DCE4DF";
    const rupees = value => "₹" + money(value).toFixed(2);
    const safe = value => String(value ?? "").replace(/[\r\n]+/g, " ").trim();
    pdf.on("data", c => chunks.push(c));
    pdf.on("end", () => resolve(Buffer.concat(chunks)));
    pdf.on("error", reject);

    // Brand header
    pdf.rect(0, 0, 595.28, 142).fill(green);
    pdf.fillColor("#FFFFFF").font("Helvetica-Bold").fontSize(20)
      .text("SRI LAKSHMI GANAPATHI", left, 38, { width: pageWidth });
    pdf.font("Helvetica").fontSize(11).fillColor("#E4EEE8")
      .text("AYURVEDA NILAYAM  |  ESTD. 1900", left, 66);
    pdf.font("Helvetica-Bold").fontSize(10).fillColor("#FFFFFF")
      .text("ORDER CONFIRMATION", left, 101);
    pdf.font("Helvetica").fontSize(9).fillColor("#DCE9E1")
      .text("PROVISIONAL INVOICE", left + 150, 102);

    // Invoice reference panel
    pdf.roundedRect(382, 31, 171, 78, 9).fill("#FFFFFF");
    pdf.fillColor(green).font("Helvetica-Bold").fontSize(8)
      .text("ORDER REFERENCE", 395, 43);
    pdf.fontSize(12).text(safe(order.orderId), 395, 58, { width: 145 });
    pdf.font("Helvetica").fontSize(8).fillColor(muted)
      .text("DATE", 395, 81);
    pdf.font("Helvetica-Bold").fontSize(9).fillColor(ink)
      .text(new Date().toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }), 430, 80);

    let y = 164;
    // Customer and delivery cards
    const cardGap = 12;
    const cardW = (pageWidth - cardGap) / 2;
    const drawInfoCard = (x, title, lines) => {
      pdf.roundedRect(x, y, cardW, 104, 8).fill(pale);
      pdf.fillColor(green).font("Helvetica-Bold").fontSize(8).text(title.toUpperCase(), x + 13, y + 13);
      let ty = y + 31;
      lines.filter(Boolean).forEach((value, index) => {
        pdf.fillColor(index === 0 ? ink : muted)
          .font(index === 0 ? "Helvetica-Bold" : "Helvetica")
          .fontSize(index === 0 ? 10 : 8.5)
          .text(safe(value), x + 13, ty, { width: cardW - 26, height: 22, ellipsis: true });
        ty += 17;
      });
    };
    drawInfoCard(left, "Billed to", [order.customerName, order.email, order.phone || ""]);
    drawInfoCard(left + cardW + cardGap, "Delivery address", [order.shippingAddress, ""]);
    y += 124;

    const drawTableHeader = () => {
      pdf.roundedRect(left, y, pageWidth, 29, 5).fill(green);
      pdf.fillColor("#FFFFFF").font("Helvetica-Bold").fontSize(8);
      pdf.text("ITEM DESCRIPTION", left + 12, y + 10, { width: 225 });
      pdf.text("QTY", left + 250, y + 10, { width: 35, align: "center" });
      pdf.text("UNIT PRICE", left + 305, y + 10, { width: 78, align: "right" });
      pdf.text("AMOUNT", left + 400, y + 10, { width: 99, align: "right" });
      y += 29;
    };
    drawTableHeader();

    for (const [index, item] of (order.items || []).entries()) {
      if (y > 690) {
        pdf.addPage();
        y = 48;
        drawTableHeader();
      }
      const rowH = 35;
      if (index % 2 === 0) pdf.rect(left, y, pageWidth, rowH).fill("#F7F9F7");
      pdf.fillColor(ink).font("Helvetica-Bold").fontSize(9)
        .text(safe(item.name), left + 12, y + 12, { width: 225, ellipsis: true });
      pdf.font("Helvetica").fontSize(9).fillColor(ink)
        .text(String(item.quantity), left + 250, y + 12, { width: 35, align: "center" })
        .text(rupees(item.unitPrice), left + 305, y + 12, { width: 78, align: "right" })
        .font("Helvetica-Bold").text(rupees(item.lineTotal), left + 400, y + 12, { width: 99, align: "right" });
      pdf.moveTo(left, y + rowH).lineTo(left + pageWidth, y + rowH).strokeColor(line).lineWidth(0.5).stroke();
      y += rowH;
    }

    y += 18;
    const summaryX = 320;
    pdf.fillColor(muted).font("Helvetica").fontSize(9).text("Product total", summaryX, y);
    pdf.fillColor(ink).font("Helvetica-Bold").fontSize(10)
      .text(rupees(order.subtotal ?? order.grandTotal), summaryX + 115, y, { width: 76, align: "right" });
    y += 24;
    pdf.roundedRect(summaryX - 10, y - 7, 201, 39, 6).fill(pale);
    pdf.fillColor(green).font("Helvetica-Bold").fontSize(10)
      .text("ORDER TOTAL", summaryX, y + 6);
    pdf.fontSize(13).text(rupees(order.grandTotal), summaryX + 90, y + 4, { width: 91, align: "right" });
    y += 54;

    const status = safe(order.orderStatus || "received").replace(/_/g, " ").toUpperCase();
    pdf.fillColor(muted).font("Helvetica").fontSize(8).text("ORDER STATUS", left, y);
    pdf.fillColor(green).font("Helvetica-Bold").fontSize(9).text(status, left + 88, y - 1);
    y += 25;
    if (y > 650) { pdf.addPage(); y = 52; }
    pdf.moveTo(left, y).lineTo(left + pageWidth, y).strokeColor(line).lineWidth(1).stroke();
    y += 13;
    pdf.fillColor(ink).font("Helvetica-Bold").fontSize(8).text("IMPORTANT INFORMATION", left, y);
    y += 14;
    pdf.fillColor(muted).font("Helvetica").fontSize(8)
      .text("This document confirms receipt of your order request and is not a final tax invoice. No online payment is recorded as collected by this document. Delivery charges, applicable taxes and the final tax invoice, if required, will be confirmed by the seller.", left, y, { width: pageWidth, lineGap: 3 });
    y = pdf.y + 18;

    // Company seal and authorized signature area
    if (y > 665) { pdf.addPage(); y = 70; }
    pdf.fillColor(ink).font("Helvetica-Bold").fontSize(9)
      .text("For Sri Lakshmi Ganapathi Ayurveda Nilayam", left, y, { width: 300 });
    pdf.moveTo(left, y + 62).lineTo(left + 230, y + 62).strokeColor(muted).lineWidth(0.8).stroke();
    pdf.fillColor(muted).font("Helvetica").fontSize(8)
      .text("Authorized Signatory", left, y + 68);
    pdf.image(path.join(__dirname, "slgan_signature_stamp.png"), 405, y - 4, { fit: [112, 112], align: "center", valign: "center" });
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
