/**
 * SLGAN order confirmation and status email service.
 *
 * Deploy as a SEPARATE Apps Script Web App (execute as owner; access: Anyone).
 * Script Properties required:
 *   FIREBASE_SERVICE_ACCOUNT_JSON = service-account JSON for slgan-a364e
 *   FIREBASE_WEB_API_KEY = Firebase project's Web API key
 *
 * Never place the service-account JSON in Android or public web code.
 * This service uses Firestore REST with OAuth credentials kept in Script Properties.
 */
const PROJECT_ID = "slgan-a364e";
const FIRESTORE_ROOT = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents";
const TOKEN_INFO_URL = "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=";

function doGet() {
  return jsonOut({ ok: true, service: "SLGAN order email service", version: 1 });
}

function doPost(e) {
  try {
    const input = JSON.parse((e && e.postData && e.postData.contents) || "{}");
    const user = verifyFirebaseUser(String(input.idToken || ""));
    const action = String(input.action || "orderPlaced");
    if (action === "orderPlaced") {
      const orderId = clean(input.orderId || input.documentId, 80);
      if (!orderId) throw new Error("Missing order ID.");
      const order = firestoreGet("orders/" + encodeURIComponent(orderId));
      if (order.customerId !== user.localId) throw new Error("Order does not belong to the signed-in customer.");
      const result = sendOrderConfirmation(orderId, order);
      let adminNotification = "Admin notification not configured.";
      try {
        adminNotification = sendAdminOrderNotification(orderId, order);
        firestorePatch("orders/" + encodeURIComponent(orderId), {
          adminEmailStatus: { stringValue: "sent" },
          adminEmailSentAt: { timestampValue: new Date().toISOString() },
          updatedAt: { timestampValue: new Date().toISOString() }
        });
      } catch (adminErr) {
        adminNotification = "Customer email sent; admin notification failed: " + String(adminErr && adminErr.message || adminErr).slice(0, 180);
        try {
          firestorePatch("orders/" + encodeURIComponent(orderId), {
            adminEmailStatus: { stringValue: "failed" },
            adminEmailError: { stringValue: String(adminErr && adminErr.message || adminErr).slice(0, 300) },
            updatedAt: { timestampValue: new Date().toISOString() }
          });
        } catch (ignored) {}
        console.error("SLGAN admin order email failed: " + (adminErr && adminErr.stack || adminErr));
      }
      return jsonOut({ success: true, ok: true, message: result + " " + adminNotification });
    }
    if (action === "statusUpdated") {
      if (!isAdmin(user.localId)) throw new Error("Only an authorized administrator may send order status updates.");
      const orderId = clean(input.orderId || input.documentId, 80);
      const status = clean(input.status, 40).toLowerCase();
      if (!orderId || !["accepted", "rejected"].includes(status)) throw new Error("Invalid order ID or status.");
      const order = firestoreGet("orders/" + encodeURIComponent(orderId));
      if (String(order.orderStatus || "").toLowerCase() !== status) throw new Error("The order status has not been saved yet.");
      const message = status === "accepted"
        ? "Your order has been accepted by Sri Lakshmi Ganapathi Ayurvedic Nilayam. We will contact you about dispatch and delivery."
        : "Your order was not accepted. Please contact Sri Lakshmi Ganapathi Ayurvedic Nilayam if you need help.";
      const statusHtml = emailFrame("Order " + (status === "accepted" ? "accepted" : "update"),
        "Your SLGAN order status has changed.",
        '<p style="margin:0 0 16px">Dear ' + escapeHtml(order.customerName || "Customer") + ',</p>' +
        '<p style="margin:0 0 18px">' + escapeHtml(message) + '</p>' +
        emailInfoTable([["Order ID", orderId], ["Current status", status.toUpperCase()]]) +
        emailButton("Visit SLGAN", "https://slgan-a364e.web.app/"));
      GmailApp.sendEmail(order.email, "SLGAN order status: " + status.toUpperCase() + " — " + orderId,
        "Dear " + (order.customerName || "Customer") + ",\\n\\n" + message +
        "\\n\\nOrder ID: " + orderId + "\\nCurrent status: " + status.toUpperCase() +
        "\\n\\nSri Lakshmi Ganapathi Ayurvedic Nilayam",
        { htmlBody: statusHtml, name: "Sri Lakshmi Ganapathi Ayurveda Nilayam" });
      return jsonOut({ success: true, ok: true, message: "Order status email sent." });
    }
    if (action === "retryInvoice") {
      if (!isAdmin(user.localId)) throw new Error("Only an authorized administrator may retry invoice emails.");
      const orderId = clean(input.orderId || input.documentId, 80);
      const order = firestoreGet("orders/" + encodeURIComponent(orderId));
      return jsonOut({ success: true, ok: true, message: sendOrderConfirmation(orderId, order) });
    }
    throw new Error("Unsupported action.");
  } catch (err) {
    console.error("SLGAN order email failed: " + (err && err.stack || err));
    return jsonOut({ success: false, ok: false, message: String(err && err.message || "Email request failed.").slice(0, 300) });
  }
}

function sendAdminOrderNotification(orderId, order) {
  const adminEmail = String(PropertiesService.getScriptProperties().getProperty("SLGAN_ADMIN_EMAIL") || "").trim();
  if (!adminEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(adminEmail)) throw new Error("SLGAN_ADMIN_EMAIL is missing or invalid in Script Properties.");
  const items = Array.isArray(order.items) ? order.items : [];
  const rows = items.map(function(item) {
    const qty = Number(item.quantity) || 0;
    const unit = Number(item.unitPrice != null ? item.unitPrice : item.price) || 0;
    const line = Number(item.lineTotal != null ? item.lineTotal : unit * qty) || 0;
    return emailTableRow(item.productName || item.name || "Product", qty, unit, line);
  }).join("");
  const total = Number(order.grandTotal != null ? order.grandTotal : order.subtotal) || 0;
  const details = emailInfoTable([
    ["Order ID", orderId], ["Order date", Utilities.formatDate(new Date(), "Asia/Kolkata", "dd MMM yyyy, hh:mm a")],
    ["Customer", order.customerName || "Customer"], ["Email", order.email || ""],
    ["Mobile", order.phone || order.deliveryPhone || ""], ["Delivery address", order.shippingAddress || order.deliveryAddress || ""],
    ["Total amount", "₹" + total.toFixed(2)], ["Status", order.orderStatus || "Pending verification"]
  ]);
  const html = emailFrame("New order received", "A customer has placed an order on SLGAN.",
    '<p style="margin:0 0 16px">Hello Admin,</p><p style="margin:0 0 18px">Please review this new order.</p>' +
    details + emailItemsTable(rows, total) + emailButton("Open Admin Panel", "https://slgan-a364e.web.app/"));
  GmailApp.sendEmail(adminEmail, "New SLGAN order received — " + orderId,
    "A new SLGAN order was placed. Order ID: " + orderId + ". Total: ₹" + total.toFixed(2),
    { htmlBody: html, name: "Sri Lakshmi Ganapathi Ayurveda Nilayam" });
  return "Admin notification sent.";
}

function sendOrderConfirmation(orderId, order) {
  if (!order.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(order.email))) throw new Error("The order has no valid customer email.");
  const props = PropertiesService.getScriptProperties();
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const docPath = "orders/" + encodeURIComponent(orderId);
    const latest = firestoreGet(docPath);
    if (latest.invoiceEmailStatus === "sent" && latest.invoiceStatus === "generated") {
      return "Invoice email was already sent.";
    }
    const items = Array.isArray(order.items) ? order.items : [];
    if (!items.length) throw new Error("The order contains no items.");
    const total = Number(order.grandTotal != null ? order.grandTotal : order.subtotal) || 0;
    const pdf = buildInvoicePdf(orderId, order, items, total);
    GmailApp.sendEmail(
      String(order.email),
      "SLGAN order confirmation and invoice — " + orderId,
      "Thank you for your order with Sri Lakshmi Ganapathi Ayurvedic Nilayam. Your order confirmation PDF is attached. Order status: " + (order.orderStatus || "pending verification") + ". No online payment has been collected.",
      {
        htmlBody: emailFrame("Order confirmed", "Thank you for choosing Sri Lakshmi Ganapathi Ayurveda Nilayam.",
          '<p style="margin:0 0 16px">Dear ' + escapeHtml(order.customerName || "Customer") + ',</p>' +
          '<p style="margin:0 0 18px">Your order has been successfully received. Your order confirmation PDF is attached.</p>' +
          emailInfoTable([["Order ID", orderId], ["Order status", order.orderStatus || "Pending verification"], ["Order total", "₹" + total.toFixed(2)]]) +
          emailButton("View SLGAN", "https://slgan-a364e.web.app/") +
          '<p style="font-size:13px;color:#687565;margin:18px 0 0">No online payment has been collected. We will contact you regarding order verification and dispatch.</p>'),
        attachments: [pdf],
        name: "Sri Lakshmi Ganapathi Ayurvedic Nilayam"
      }
    );
    firestorePatch(docPath, {
      invoiceStatus: { stringValue: "generated" },
      invoiceEmailStatus: { stringValue: "sent" },
      invoiceGeneratedAt: { timestampValue: new Date().toISOString() },
      invoiceEmailSentAt: { timestampValue: new Date().toISOString() },
      updatedAt: { timestampValue: new Date().toISOString() }
    });
    return "Order confirmation and PDF attachment emailed.";
  } catch (err) {
    try {
      firestorePatch("orders/" + encodeURIComponent(orderId), {
        invoiceStatus: { stringValue: "failed" },
        invoiceEmailStatus: { stringValue: "failed" },
        invoiceEmailError: { stringValue: String(err && err.message || err).slice(0, 300) },
        updatedAt: { timestampValue: new Date().toISOString() }
      });
    } catch (ignored) {}
    throw err;
  } finally {
    lock.releaseLock();
  }
}

function buildInvoicePdf(orderId, order, items, total) {
  // Keep the Apps Script email workflow visually aligned with the approved
  // forest-green Firebase invoice design, including the SLGAN rubber stamp.
  const doc = DocumentApp.create("SLGAN " + orderId + " invoice");
  const body = doc.getBody();
  body.clear();
  body.setMarginTop(34).setMarginBottom(36).setMarginLeft(38).setMarginRight(38);

  const green = "#244F43";
  const pale = "#F1F5F2";
  const ink = "#252B2A";
  const muted = "#69736F";
  const white = "#FFFFFF";
  const dateText = Utilities.formatDate(new Date(), "Asia/Kolkata", "dd MMM yyyy, hh:mm a");
  const moneyText = function(value) { return "₹" + (Number(value) || 0).toFixed(2); };

  const brand = body.appendParagraph("SRI LAKSHMI GANAPATHI");
  brand.setHeading(DocumentApp.ParagraphHeading.TITLE);
  brand.setForegroundColor(white).setBackgroundColor(green).setBold(true)
    .setFontFamily("Arial").setFontSize(20).setSpacingBefore(0).setSpacingAfter(0)
    .setIndentStart(12).setIndentEnd(12);
  const subBrand = body.appendParagraph("AYURVEDA NILAYAM  |  ESTD. 1900");
  subBrand.setForegroundColor(white).setBackgroundColor(green).setFontFamily("Arial")
    .setFontSize(10).setBold(true).setIndentStart(12).setIndentEnd(12)
    .setSpacingBefore(0).setSpacingAfter(12);
  const title = body.appendParagraph("ORDER CONFIRMATION  |  PROVISIONAL INVOICE");
  title.setForegroundColor(green).setBold(true).setFontFamily("Arial").setFontSize(13)
    .setSpacingBefore(10).setSpacingAfter(8);

  const refTable = body.appendTable([
    ["ORDER REFERENCE", "DATE"],
    [String(orderId), dateText]
  ]);
  refTable.setBorderColor("#DCE4DF").setBorderWidth(1);
  for (let r = 0; r < refTable.getNumRows(); r++) {
    for (let c = 0; c < refTable.getRow(r).getNumCells(); c++) {
      const cell = refTable.getCell(r).getCell(c);
      cell.setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(8).setPaddingRight(8);
      cell.setBackgroundColor(r === 0 ? pale : white);
      cell.getChild(0).asParagraph().setFontFamily("Arial").setFontSize(r === 0 ? 8 : 9)
        .setBold(r === 0 || c === 0).setForegroundColor(r === 0 ? green : ink);
    }
  }

  body.appendParagraph("CUSTOMER & DELIVERY DETAILS").setForegroundColor(green)
    .setBold(true).setFontFamily("Arial").setFontSize(9).setSpacingBefore(14).setSpacingAfter(5);
  const details = body.appendTable([
    ["BILLED TO", "DELIVERY ADDRESS"],
    [String(order.customerName || "Customer") + "\\n" + String(order.email || "") + "\\n" + String(order.phone || order.deliveryPhone || ""),
     String(order.shippingAddress || order.deliveryAddress || "")]
  ]);
  details.setBorderColor("#DCE4DF").setBorderWidth(1);
  for (let r = 0; r < details.getNumRows(); r++) {
    for (let c = 0; c < details.getRow(r).getNumCells(); c++) {
      const cell = details.getCell(r).getCell(c);
      cell.setPaddingTop(7).setPaddingBottom(7).setPaddingLeft(8).setPaddingRight(8)
        .setBackgroundColor(r === 0 ? pale : white);
      cell.getChild(0).asParagraph().setFontFamily("Arial").setFontSize(r === 0 ? 8 : 9)
        .setBold(r === 0).setForegroundColor(r === 0 ? green : ink);
    }
  }

  body.appendParagraph("ORDER ITEMS").setForegroundColor(green).setBold(true)
    .setFontFamily("Arial").setFontSize(9).setSpacingBefore(14).setSpacingAfter(5);
  const rows = [["ITEM DESCRIPTION", "QTY", "UNIT PRICE", "AMOUNT"]];
  items.forEach(function(it) {
    const qty = Number(it.quantity) || 0;
    const unit = Number(it.unitPrice != null ? it.unitPrice : it.price) || 0;
    const line = Number(it.lineTotal != null ? it.lineTotal : unit * qty) || 0;
    rows.push([String(it.productName || it.name || "Product"), String(qty), moneyText(unit), moneyText(line)]);
  });
  const table = body.appendTable(rows);
  table.setBorderColor("#DCE4DF").setBorderWidth(0.5);
  for (let r = 0; r < table.getNumRows(); r++) {
    for (let c = 0; c < table.getRow(r).getNumCells(); c++) {
      const cell = table.getCell(r).getCell(c);
      cell.setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(6).setPaddingRight(6);
      cell.setBackgroundColor(r === 0 ? green : (r % 2 === 0 ? "#F7F9F7" : white));
      const p = cell.getChild(0).asParagraph();
      p.setFontFamily("Arial").setFontSize(r === 0 ? 8 : 9)
        .setBold(r === 0 || c === 0).setForegroundColor(r === 0 ? white : ink);
      if (c > 0) p.setAlignment(DocumentApp.HorizontalAlignment.RIGHT);
    }
  }

  const totalTable = body.appendTable([
    ["PRODUCT TOTAL", moneyText(total)],
    ["ORDER TOTAL", moneyText(total)]
  ]);
  totalTable.setBorderColor("#DCE4DF").setBorderWidth(0);
  for (let r = 0; r < totalTable.getNumRows(); r++) {
    for (let c = 0; c < 2; c++) {
      const cell = totalTable.getCell(r).getCell(c);
      cell.setPaddingTop(7).setPaddingBottom(7).setPaddingLeft(8).setPaddingRight(8)
        .setBackgroundColor(r === 1 ? pale : white);
      cell.getChild(0).asParagraph().setFontFamily("Arial").setFontSize(r === 1 ? 11 : 9)
        .setBold(true).setForegroundColor(r === 1 ? green : muted)
        .setAlignment(c === 0 ? DocumentApp.HorizontalAlignment.LEFT : DocumentApp.HorizontalAlignment.RIGHT);
    }
  }

  const status = String(order.orderStatus || "pending verification").replace(/_/g, " ").toUpperCase();
  body.appendParagraph("ORDER STATUS: " + status).setForegroundColor(green).setBold(true)
    .setFontFamily("Arial").setFontSize(9).setSpacingBefore(10).setSpacingAfter(8);
  body.appendParagraph("IMPORTANT INFORMATION").setForegroundColor(ink).setBold(true)
    .setFontFamily("Arial").setFontSize(8).setSpacingBefore(8).setSpacingAfter(4);
  body.appendParagraph("This document confirms receipt of your order request and is not a final tax invoice. No online payment is recorded as collected by this document. Delivery charges, applicable taxes and the final tax invoice, if required, will be confirmed by the seller.")
    .setForegroundColor(muted).setFontFamily("Arial").setFontSize(8).setLineSpacing(115);

  const signatory = body.appendTable([["", ""]]);
  signatory.setBorderColor("#FFFFFF").setBorderWidth(0);
  const signatoryText = signatory.getCell(0, 0);
  signatoryText.setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(0).setPaddingRight(8);
  signatoryText.appendParagraph("For Sri Lakshmi Ganapathi Ayurveda Nilayam")
    .setForegroundColor(ink).setBold(true).setFontFamily("Arial").setFontSize(9);
  signatoryText.appendParagraph("______________________________")
    .setForegroundColor(muted).setFontFamily("Arial").setFontSize(9);
  signatoryText.appendParagraph("Authorized Signatory")
    .setForegroundColor(muted).setFontFamily("Arial").setFontSize(8);

  // The stamp is a public, versioned image in this repository; fetch it at
  // document-generation time so the email PDF carries the same approved seal.
  const stampUrl = "https://raw.githubusercontent.com/pyaraallamraju-spec/SLGAN/main/functions/slgan_signature_stamp.png";
  const stampResponse = UrlFetchApp.fetch(stampUrl, { muteHttpExceptions: true });
  if (stampResponse.getResponseCode() >= 200 && stampResponse.getResponseCode() < 300) {
    const stampCell = signatory.getCell(0, 1);
    stampCell.setPaddingTop(0).setPaddingBottom(0).setPaddingLeft(8).setPaddingRight(0);
    const stamp = stampCell.appendImage(stampResponse.getBlob().setName("slgan_signature_stamp.png"));
    stamp.setWidth(112).setHeight(112);
  } else {
    console.error("SLGAN signature stamp could not be fetched; HTTP " + stampResponse.getResponseCode());
  }

  doc.saveAndClose();
  const file = DriveApp.getFileById(doc.getId());
  const pdf = file.getAs(MimeType.PDF).setName(orderId + ".pdf");
  file.setTrashed(true);
  return pdf;
}

function invoiceHtml(orderId, order, items, total) {
  const rows = items.map(function(it) {
    const name = it.productName || it.name || "Product";
    const qty = Number(it.quantity) || 0;
    const unit = Number(it.unitPrice != null ? it.unitPrice : it.price) || 0;
    const line = Number(it.lineTotal != null ? it.lineTotal : unit * qty) || 0;
    return "<tr><td>" + escapeHtml(name) + "</td><td>" + qty + "</td><td>₹" + unit.toFixed(2) + "</td><td>₹" + line.toFixed(2) + "</td></tr>";
  }).join("");
  return '<html><head><meta charset="UTF-8"><style>body{font-family:Arial,sans-serif;color:#38251f;padding:26px}h1{color:#762a24;font-size:22px}table{width:100%;border-collapse:collapse;margin-top:18px}th,td{border:1px solid #ddd;padding:8px;text-align:left}th{background:#fff2d7}.total{text-align:right;font-size:17px;font-weight:bold;margin-top:20px}.note{margin-top:25px;font-size:11px;color:#655}</style></head><body><h1>Sri Lakshmi Ganapathi Ayurvedic Nilayam</h1><h2>Order Confirmation / Provisional Invoice</h2><p><b>Order ID:</b> ' + escapeHtml(orderId) + '<br><b>Date:</b> ' + escapeHtml(new Date().toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })) + '<br><b>Customer:</b> ' + escapeHtml(order.customerName || "") + '<br><b>Email:</b> ' + escapeHtml(order.email || "") + '<br><b>Phone:</b> ' + escapeHtml(order.phone || order.deliveryPhone || "") + '<br><b>Delivery address:</b> ' + escapeHtml(order.shippingAddress || order.deliveryAddress || "") + '</p><table><thead><tr><th>Product</th><th>Qty</th><th>Unit price</th><th>Amount</th></tr></thead><tbody>' + rows + '</tbody></table><div class="total">Product total: ₹' + total.toFixed(2) + '</div><p><b>Order status:</b> ' + escapeHtml(order.orderStatus || "pending verification") + '</p><p class="note">No online payment was collected. This document confirms receipt of the order request; it is not a final tax invoice. Delivery charges, applicable taxes and the final tax invoice, if required, will be confirmed by the seller.</p></body></html>';
}

const SLGAN_LOGO_URL = "https://raw.githubusercontent.com/pyaraallamraju-spec/SLGAN/main/app/src/main/res/drawable-nodpi/slgan_logo_true_transparent.png";
function emailFrame(title, subtitle, content) {
  return '<!doctype html><html><body style="margin:0;padding:24px 10px;background:#f3f5ed;font-family:Arial,Helvetica,sans-serif;color:#203b2b">' +
    '<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:620px;margin:0 auto;background:#fffdf6;border:1px solid #e8dfc5;border-radius:18px;overflow:hidden">' +
    '<tr><td style="padding:22px 24px;background:#fffdf2;text-align:center;border-bottom:3px solid #c9a84f"><img src="' + SLGAN_LOGO_URL + '" alt="SLGAN logo" width="76" height="76" style="display:block;margin:0 auto 10px;object-fit:contain">' +
    '<div style="font-size:23px;line-height:1.25;font-weight:700;color:#173d28">Sri Lakshmi Ganapathi</div><div style="font-size:16px;color:#365641;margin-top:3px">Ayurveda Nilayam</div><div style="font-size:11px;color:#9b792e;margin-top:7px">Estd. 1900 by Perraju Sharma</div></td></tr>' +
    '<tr><td style="padding:25px 24px;background:#174b2d;color:#fffdf2;border-bottom:4px solid #d5b65d"><div style="font-size:25px;font-weight:700;line-height:1.25">' + escapeHtml(title) + '</div><div style="font-size:14px;line-height:1.5;margin-top:7px;color:#f6efd9">' + escapeHtml(subtitle) + '</div></td></tr>' +
    '<tr><td style="padding:26px 24px 30px;font-size:15px;line-height:1.6;color:#26392d">' + content + '</td></tr>' +
    '<tr><td style="padding:20px 24px;background:#174b2d;color:#fffdf2;text-align:center;border-top:4px solid #d5b65d"><div style="font-weight:700;font-size:14px">Sri Lakshmi Ganapathi Ayurveda Nilayam</div><div style="font-size:12px;color:#e9e0c8;margin-top:6px">U. Kothapalli – 533447, Andhra Pradesh, India</div><div style="font-size:11px;color:#e9e0c8;margin-top:10px">Authentic Ayurvedic Formulas &nbsp; • &nbsp; Traditional Heritage &nbsp; • &nbsp; Quality Assured</div></td></tr></table></body></html>';
}
function emailInfoTable(rows) {
  return '<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border:1px solid #e7d79e;border-radius:10px;background:#fffdf6;margin:16px 0 20px">' +
    rows.map(function(row) { return '<tr><td style="padding:8px 12px;color:#687565;font-size:13px;border-bottom:1px solid #f0ead8;width:38%">' + escapeHtml(row[0]) + '</td><td style="padding:8px 12px;color:#203b2b;font-size:13px;font-weight:600;border-bottom:1px solid #f0ead8">' + escapeHtml(row[1]) + '</td></tr>'; }).join("") + '</table>';
}
function emailTableRow(name, qty, unit, line) {
  return '<tr><td style="padding:9px;border-bottom:1px solid #e9e2ce">' + escapeHtml(name) + '</td><td style="padding:9px;text-align:center;border-bottom:1px solid #e9e2ce">' + qty + '</td><td style="padding:9px;text-align:right;border-bottom:1px solid #e9e2ce">₹' + unit.toFixed(2) + '</td><td style="padding:9px;text-align:right;border-bottom:1px solid #e9e2ce">₹' + line.toFixed(2) + '</td></tr>';
}
function emailItemsTable(rows, total) {
  return '<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;font-size:12px;margin:18px 0"><tr style="background:#e8edcf;color:#203b2b"><th align="left" style="padding:9px">Product</th><th>Qty</th><th align="right">Unit</th><th align="right">Amount</th></tr>' + rows + '<tr><td colspan="3" align="right" style="padding:11px;font-weight:700;color:#203b2b">Total</td><td align="right" style="padding:11px;font-weight:700;color:#174b2d">₹' + total.toFixed(2) + '</td></tr></table>';
}
function emailButton(label, url) {
  return '<table role="presentation" cellspacing="0" cellpadding="0" style="margin:22px auto"><tr><td bgcolor="#17602f" style="border-radius:8px"><a href="' + escapeHtml(url) + '" style="display:inline-block;padding:13px 25px;color:#fffdf2;text-decoration:none;font-weight:700;font-size:14px">' + escapeHtml(label) + '</a></td></tr></table>';
}

function verifyFirebaseUser(idToken) {
  if (!idToken) throw new Error("Sign in again before requesting an order email.");
  const key = PropertiesService.getScriptProperties().getProperty("FIREBASE_WEB_API_KEY");
  if (!key) throw new Error("FIREBASE_WEB_API_KEY is missing from Script Properties.");
  const response = UrlFetchApp.fetch(TOKEN_INFO_URL + encodeURIComponent(key), {
    method: "post", contentType: "application/json",
    payload: JSON.stringify({ idToken: idToken }), muteHttpExceptions: true
  });
  const body = JSON.parse(response.getContentText() || "{}");
  if (response.getResponseCode() < 200 || response.getResponseCode() >= 300 ||
      !body.users || !body.users.length || !body.users[0].localId) {
    throw new Error("Firebase sign-in token is invalid or expired.");
  }
  return body.users[0];
}

function isAdmin(uid) {
  const profile = firestoreGet("customers/" + encodeURIComponent(uid));
  return profile.is_admin === true;
}

function firestoreGet(path) {
  const url = FIRESTORE_ROOT + "/" + path;
  const response = UrlFetchApp.fetch(url, {
    headers: { Authorization: "Bearer " + getAccessToken() },
    muteHttpExceptions: true
  });
  if (response.getResponseCode() < 200 || response.getResponseCode() >= 300) {
    throw new Error("Could not read Firestore document (" + response.getResponseCode() + "). Check service-account permissions and document ID.");
  }
  const doc = JSON.parse(response.getContentText() || "{}");
  const out = {};
  Object.keys(doc.fields || {}).forEach(function(k) { out[k] = decodeValue(doc.fields[k]); });
  return out;
}

function firestorePatch(path, fields) {
  const mask = Object.keys(fields).map(function(k) { return "updateMask.fieldPaths=" + encodeURIComponent(k); }).join("&");
  const response = UrlFetchApp.fetch(FIRESTORE_ROOT + "/" + path + "?" + mask, {
    method: "patch",
    contentType: "application/json",
    headers: { Authorization: "Bearer " + getAccessToken() },
    payload: JSON.stringify({ fields: fields }),
    muteHttpExceptions: true
  });
  if (response.getResponseCode() < 200 || response.getResponseCode() >= 300) {
    throw new Error("Could not update invoice email status in Firestore (" + response.getResponseCode() + ").");
  }
}

function decodeValue(v) {
  if (!v) return null;
  if (v.stringValue !== undefined) return v.stringValue;
  if (v.integerValue !== undefined) return Number(v.integerValue);
  if (v.doubleValue !== undefined) return Number(v.doubleValue);
  if (v.booleanValue !== undefined) return v.booleanValue;
  if (v.timestampValue !== undefined) return v.timestampValue;
  if (v.nullValue !== undefined) return null;
  if (v.arrayValue) return (v.arrayValue.values || []).map(decodeValue);
  if (v.mapValue) {
    const out = {};
    Object.keys(v.mapValue.fields || {}).forEach(function(k) { out[k] = decodeValue(v.mapValue.fields[k]); });
    return out;
  }
  return null;
}

function getAccessToken() {
  const cache = CacheService.getScriptCache();
  const cached = cache.get("FIRESTORE_OAUTH_ACCESS_TOKEN");
  if (cached) return cached;
  const raw = PropertiesService.getScriptProperties().getProperty("FIREBASE_SERVICE_ACCOUNT_JSON");
  if (!raw) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON is missing from Script Properties.");
  const sa = JSON.parse(raw);
  if (sa.project_id !== PROJECT_ID || !sa.client_email || !sa.private_key) {
    throw new Error("Service-account JSON is incomplete or belongs to a different Firebase project.");
  }
  const now = Math.floor(Date.now() / 1000);
  const enc = function(obj) { return Utilities.base64EncodeWebSafe(JSON.stringify(obj)).replace(/=+$/g, ""); };
  const unsigned = enc({ alg: "RS256", typ: "JWT" }) + "." + enc({
    iss: sa.client_email, sub: sa.client_email, aud: "https://oauth2.googleapis.com/token",
    iat: now, exp: now + 3600, scope: "https://www.googleapis.com/auth/datastore"
  });
  const signature = Utilities.computeRsaSha256Signature(unsigned, sa.private_key);
  const assertion = unsigned + "." + Utilities.base64EncodeWebSafe(signature).replace(/=+$/g, "");
  const tokenResponse = UrlFetchApp.fetch("https://oauth2.googleapis.com/token", {
    method: "post", payload: { grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: assertion },
    muteHttpExceptions: true
  });
  const tokenBody = JSON.parse(tokenResponse.getContentText() || "{}");
  if (tokenResponse.getResponseCode() < 200 || tokenResponse.getResponseCode() >= 300 || !tokenBody.access_token) {
    throw new Error("Could not obtain Firestore access token. Check the service-account key.");
  }
  cache.put("FIRESTORE_OAUTH_ACCESS_TOKEN", tokenBody.access_token, Math.min(3300, Number(tokenBody.expires_in || 3600) - 60));
  return tokenBody.access_token;
}

function clean(v, max) { return typeof v === "string" ? v.trim().slice(0, max || 200) : ""; }
function escapeHtml(v) {
  return String(v == null ? "" : v).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}
function jsonOut(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
