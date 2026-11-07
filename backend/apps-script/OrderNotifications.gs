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
      const documentId = clean(input.documentId || input.orderId, 120);
      if (!documentId) throw new Error("Missing Firestore order document ID.");
      const order = firestoreGet("orders/" + encodeURIComponent(documentId));
      if (order.customerId !== user.localId) throw new Error("Order does not belong to the signed-in customer.");
      const displayOrderId = clean(order.orderId || input.orderId || documentId, 80);
      const result = sendOrderConfirmation(documentId, displayOrderId, order);
      let adminNotification = "Admin notification not configured.";
      try {
        adminNotification = sendAdminOrderNotification(displayOrderId, order);
        firestorePatch("orders/" + encodeURIComponent(documentId), {
          adminEmailStatus: { stringValue: "sent" },
          adminEmailSentAt: { timestampValue: new Date().toISOString() },
          updatedAt: { timestampValue: new Date().toISOString() }
        });
      } catch (adminErr) {
        adminNotification = "Customer email sent; admin notification failed: " + String(adminErr && adminErr.message || adminErr).slice(0, 180);
        try {
          firestorePatch("orders/" + encodeURIComponent(documentId), {
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
      const documentId = clean(input.documentId || input.orderId, 120);
      const status = clean(input.status, 40).toLowerCase();
      if (!documentId || !["accepted", "rejected"].includes(status)) throw new Error("Invalid order document ID or status.");
      const order = firestoreGet("orders/" + encodeURIComponent(documentId));
      const displayOrderId = clean(order.orderId || documentId, 80);
      if (String(order.orderStatus || "").toLowerCase() !== status) throw new Error("The order status has not been saved yet.");
      const message = status === "accepted"
        ? "Your order has been accepted by Sri Lakshmi Ganapathi Ayurvedic Nilayam. We will contact you about dispatch and delivery."
        : "Your order was not accepted. Please contact Sri Lakshmi Ganapathi Ayurvedic Nilayam if you need help.";
      const statusHtml = emailFrame("Order " + (status === "accepted" ? "accepted" : "update"),
        "Your SLGAN order status has changed.",
        '<p style="margin:0 0 16px">Dear ' + escapeHtml(order.customerName || "Customer") + ',</p>' +
        '<p style="margin:0 0 18px">' + escapeHtml(message) + '</p>' +
        emailInfoTable([["Order ID", displayOrderId], ["Current status", status.toUpperCase()]]) +
        emailButton("Visit SLGAN", "https://slgan-a364e.web.app/"));
      GmailApp.sendEmail(order.email, "SLGAN order status: " + status.toUpperCase() + " — " + displayOrderId,
        "Dear " + (order.customerName || "Customer") + ",\\n\\n" + message +
        "\\n\\nOrder ID: " + displayOrderId + "\\nCurrent status: " + status.toUpperCase() +
        "\\n\\nSri Lakshmi Ganapathi Ayurvedic Nilayam",
        { htmlBody: statusHtml, name: "Sri Lakshmi Ganapathi Ayurveda Nilayam" });
      return jsonOut({ success: true, ok: true, message: "Order status email sent." });
    }
    if (action === "retryInvoice") {
      if (!isAdmin(user.localId)) throw new Error("Only an authorized administrator may retry invoice emails.");
      const documentId = clean(input.documentId || input.orderId, 120);
      if (!documentId) throw new Error("Missing Firestore order document ID.");
      const order = firestoreGet("orders/" + encodeURIComponent(documentId));
      const displayOrderId = clean(order.orderId || documentId, 80);
      return jsonOut({ success: true, ok: true, message: sendOrderConfirmation(documentId, displayOrderId, order) });
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

function sendOrderConfirmation(documentId, displayOrderId, order) {
  if (!order.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(order.email))) throw new Error("The order has no valid customer email.");
  const props = PropertiesService.getScriptProperties();
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const docPath = "orders/" + encodeURIComponent(documentId);
    const latest = firestoreGet(docPath);
    if (latest.invoiceEmailStatus === "sent" && latest.invoiceStatus === "generated") {
      return "Invoice email was already sent.";
    }
    const items = Array.isArray(order.items) ? order.items : [];
    if (!items.length) throw new Error("The order contains no items.");
    const total = Number(order.grandTotal != null ? order.grandTotal : order.subtotal) || 0;
    const pdf = buildInvoicePdf(displayOrderId, order, items, total);
    GmailApp.sendEmail(
      String(order.email),
      "SLGAN order confirmation and invoice — " + displayOrderId,
      "Thank you for your order with Sri Lakshmi Ganapathi Ayurvedic Nilayam. Your order confirmation PDF is attached. Order status: " + (order.orderStatus || "pending verification") + ". No online payment has been collected.",
      {
        htmlBody: emailFrame("Order confirmed", "Thank you for choosing Sri Lakshmi Ganapathi Ayurveda Nilayam.",
          '<p style="margin:0 0 16px">Dear ' + escapeHtml(order.customerName || "Customer") + ',</p>' +
          '<p style="margin:0 0 18px">Your order has been successfully received. Your order confirmation PDF is attached.</p>' +
          emailInfoTable([["Order ID", displayOrderId], ["Order status", order.orderStatus || "Pending verification"], ["Order total", "₹" + total.toFixed(2)]]) +
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
      firestorePatch("orders/" + encodeURIComponent(documentId), {
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
  // Branded provisional order invoice. Email, authentication and Firestore workflows are unchanged.
  var doc = DocumentApp.create("SLGAN " + orderId + " invoice");
  var body = doc.getBody();
  var green = "#174B35", deep = "#103D2B", pale = "#EEF4EF";
  var ink = "#26332B", muted = "#68766D", line = "#D9E3DB", white = "#FFFFFF";
  var money = function(value) { return "₹" + (Number(value) || 0).toFixed(2); };
  var dateValue = order.createdAt ? new Date(order.createdAt) : new Date();
  if (isNaN(dateValue.getTime())) dateValue = new Date();
  var dateText = Utilities.formatDate(dateValue, "Asia/Kolkata", "dd MMM yyyy, hh:mm a");
  var cleanText = function(value) {
    if (value == null) return "";
    if (Array.isArray(value)) return value.map(cleanText).filter(Boolean).join(", ");
    if (typeof value === "object") {
      var preferred = ["fullAddress","addressLine1","addressLine2","houseNo","flatNo","building","street","landmark","area","locality","village","city","district","state","postalCode","pincode","pinCode"];
      var parts = [];
      preferred.forEach(function(key) {
        if (value[key] != null && String(value[key]).trim()) parts.push(String(value[key]).trim());
      });
      if (!parts.length) Object.keys(value).forEach(function(key) {
        if (value[key] != null && typeof value[key] !== "object" && String(value[key]).trim()) parts.push(String(value[key]).trim());
      });
      return parts.join(", ");
    }
    return String(value).trim();
  };
  var address = cleanText(order.shippingAddress || order.deliveryAddress || order.address);
  if (!address) address = "Full delivery address not provided in order record";
  var customerLines = [
    cleanText(order.customerName) || "Customer",
    cleanText(order.email),
    cleanText(order.phone || order.deliveryPhone)
  ].filter(Boolean);
  body.clear();
  body.setMarginTop(30); body.setMarginBottom(32); body.setMarginLeft(34); body.setMarginRight(34);

  // Header: logo on the left, contact information aligned on the right.
  var header = body.appendTable([["", ""]]);
  header.setBorderWidth(0);
  var logoCell = header.getCell(0,0), contactCell = header.getCell(0,1);
  logoCell.setWidth(118); contactCell.setWidth(365);
  [logoCell,contactCell].forEach(function(cell) {
    cell.setBorderWidth(0); cell.setPaddingTop(2); cell.setPaddingBottom(8);
    cell.setPaddingLeft(3); cell.setPaddingRight(3); cell.setVerticalAlignment(DocumentApp.VerticalAlignment.CENTER);
  });
  try {
    var logoUrl = "https://raw.githubusercontent.com/pyaraallamraju-spec/SLGAN/main/app/src/main/res/drawable-nodpi/slgan_logo_true_transparent.png";
    var logoResponse = UrlFetchApp.fetch(logoUrl, {muteHttpExceptions:true});
    if (logoResponse.getResponseCode() >= 200 && logoResponse.getResponseCode() < 300) {
      var logoP = logoCell.getChild(0).asParagraph();
      logoP.appendInlineImage(logoResponse.getBlob()).setWidth(92);
    } else {
      logoCell.getChild(0).asParagraph().appendText("SLGAN").setBold(true).setForegroundColor(green).setFontSize(18);
    }
  } catch (logoError) {
    logoCell.getChild(0).asParagraph().appendText("SLGAN").setBold(true).setForegroundColor(green).setFontSize(18);
    console.warn("Invoice logo unavailable: " + logoError);
  }
  var contactP = contactCell.getChild(0).asParagraph();
  contactP.setAlignment(DocumentApp.HorizontalAlignment.RIGHT);
  contactP.appendText("SRI LAKSHMI GANAPATHI\n").setBold(true).setForegroundColor(green).setFontSize(15);
  contactP.appendText("AYURVEDA NILAYAM  |  ESTD. 1900\n").setBold(true).setForegroundColor(muted).setFontSize(9);
  contactP.appendText("U. Kothapalli – 533447, Andhra Pradesh\n").setForegroundColor(muted).setFontSize(8);
  contactP.appendText("Phone: 9908355501\n").setForegroundColor(ink).setFontSize(9);
  contactP.appendText("Email: info.slgan@gmail.com").setForegroundColor(ink).setFontSize(9);

  var rule = body.appendParagraph(" ");
  rule.setBackgroundColor(green); rule.setSpacingBefore(2); rule.setSpacingAfter(12);

  var title = body.appendParagraph("ORDER CONFIRMATION");
  title.setForegroundColor(deep).setFontSize(17).setBold(true);
  title.setSpacingAfter(3);
  var subtitle = body.appendParagraph("PROVISIONAL INVOICE  •  NOT A FINAL TAX INVOICE");
  subtitle.setForegroundColor(muted).setFontSize(8).setBold(true).setSpacingAfter(12);

  var ref = body.appendTable([["ORDER REFERENCE","ORDER DATE"],[String(orderId),dateText]]);
  ref.setBorderColor(line).setBorderWidth(0.7);
  for (var r=0;r<2;r++) for (var c=0;c<2;c++) {
    var cell=ref.getCell(r,c); cell.setPaddingTop(7); cell.setPaddingBottom(7); cell.setPaddingLeft(8); cell.setPaddingRight(8);
    cell.setBackgroundColor(r===0?pale:white);
    var p=cell.getChild(0).asParagraph(); p.setFontFamily("Arial").setFontSize(r===0?8:10).setForegroundColor(r===0?green:ink).setBold(r===0);
  }

  var section = function(label) {
    var p=body.appendParagraph(label); p.setForegroundColor(green).setFontSize(9).setBold(true);
    p.setSpacingBefore(13).setSpacingAfter(5);
  };
  section("CUSTOMER & DELIVERY DETAILS");
  var details=body.appendTable([["CUSTOMER DETAILS","COMPLETE DELIVERY ADDRESS"],[customerLines.join("\n"),address]]);
  details.setBorderColor(line).setBorderWidth(0.7);
  for (var dr=0;dr<2;dr++) for (var dc=0;dc<2;dc++) {
    var cell=details.getCell(dr,dc); cell.setPaddingTop(8); cell.setPaddingBottom(8); cell.setPaddingLeft(8); cell.setPaddingRight(8);
    cell.setBackgroundColor(dr===0?pale:white);
    cell.setVerticalAlignment(DocumentApp.VerticalAlignment.TOP);
    var p=cell.getChild(0).asParagraph(); p.setFontFamily("Arial").setFontSize(dr===0?8:9).setForegroundColor(dr===0?green:ink).setBold(dr===0);
    p.setLineSpacing(115);
  }

  section("ITEM DETAILS");
  var rows=[["PRODUCT DESCRIPTION","QTY","UNIT PRICE","LINE TOTAL"]];
  items.forEach(function(item) {
    var qty=Number(item.quantity)||0;
    var unit=Number(item.unitPrice!=null?item.unitPrice:item.price)||0;
    var amount=Number(item.lineTotal!=null?item.lineTotal:unit*qty)||0;
    rows.push([cleanText(item.productName||item.name)||"Product",String(qty),money(unit),money(amount)]);
  });
  var table=body.appendTable(rows); table.setBorderColor(line).setBorderWidth(0.6);
  for (var ir=0;ir<table.getNumRows();ir++) for (var ic=0;ic<4;ic++) {
    var cell=table.getCell(ir,ic); cell.setPaddingTop(7); cell.setPaddingBottom(7); cell.setPaddingLeft(6); cell.setPaddingRight(6);
    cell.setBackgroundColor(ir===0?green:(ir%2===0?pale:white));
    var p=cell.getChild(0).asParagraph(); p.setFontFamily("Arial").setFontSize(ir===0?8:9).setForegroundColor(ir===0?white:ink).setBold(ir===0||ic===0);
    if(ic>0) p.setAlignment(DocumentApp.HorizontalAlignment.RIGHT);
  }

  var subtotal=Number(order.subtotal!=null?order.subtotal:total)||0;
  var delivery=Number(order.deliveryCharges)||0;
  var discount=Number(order.discountAmount!=null?order.discountAmount:order.discount)||0;
  var tax=Number(order.taxAmount!=null?order.taxAmount:order.tax)||0;
  var totalRows=[["Product subtotal",money(subtotal)]];
  if(order.deliveryCharges!=null) totalRows.push(["Delivery charges",money(delivery)]);
  if(discount) totalRows.push(["Discount","−"+money(discount)]);
  if(tax) totalRows.push(["Tax",money(tax)]);
  totalRows.push(["ORDER TOTAL",money(total)]);
  var totals=body.appendTable(totalRows); totals.setBorderColor(line).setBorderWidth(0.5);
  for(var tr=0;tr<totals.getNumRows();tr++) for(var tc=0;tc<2;tc++) {
    var cell=totals.getCell(tr,tc); cell.setPaddingTop(6); cell.setPaddingBottom(6); cell.setPaddingLeft(8); cell.setPaddingRight(8);
    cell.setBackgroundColor(tr===totals.getNumRows()-1?pale:white);
    var p=cell.getChild(0).asParagraph(); p.setFontFamily("Arial").setFontSize(tr===totals.getNumRows()-1?11:9);
    p.setForegroundColor(tr===totals.getNumRows()-1?green:muted).setBold(tr===totals.getNumRows()-1||tc===1);
    p.setAlignment(tc===0?DocumentApp.HorizontalAlignment.LEFT:DocumentApp.HorizontalAlignment.RIGHT);
  }

  var status=body.appendParagraph("ORDER STATUS: "+cleanText(order.orderStatus||"Pending verification").replace(/_/g," ").toUpperCase());
  status.setForegroundColor(green).setFontSize(9).setBold(true).setSpacingBefore(12);
  var payment=body.appendParagraph("PAYMENT: "+cleanText(order.paymentStatus||"Not collected").replace(/_/g," ").toUpperCase());
  payment.setForegroundColor(muted).setFontSize(8).setSpacingAfter(8);

  var note=body.appendParagraph("IMPORTANT NOTE");
  note.setForegroundColor(ink).setFontSize(8).setBold(true).setSpacingBefore(8);
  var disclaimer=body.appendParagraph("This document confirms receipt of the order request and is not a final tax invoice. No online payment is recorded as collected. Delivery charges and applicable taxes are shown only where confirmed in the order record. The seller will confirm final details before dispatch.");
  disclaimer.setForegroundColor(muted).setFontSize(8).setLineSpacing(115);

  // Stamp and signatory share one row, with stamp to the left of the signatory.
  var sign=body.appendTable([["",""]]); sign.setBorderWidth(0);
  var stampCell=sign.getCell(0,0), signCell=sign.getCell(0,1);
  [stampCell,signCell].forEach(function(cell){cell.setBorderWidth(0);cell.setPaddingTop(8);cell.setPaddingBottom(2);cell.setPaddingLeft(4);cell.setPaddingRight(4);});
  try {
    var stampUrl="https://raw.githubusercontent.com/pyaraallamraju-spec/SLGAN/main/functions/slgan_signature_stamp.png";
    var stampResponse=UrlFetchApp.fetch(stampUrl,{muteHttpExceptions:true});
    if(stampResponse.getResponseCode()>=200&&stampResponse.getResponseCode()<300) stampCell.getChild(0).asParagraph().appendInlineImage(stampResponse.getBlob()).setWidth(76);
  } catch(stampError) { console.warn("Invoice stamp unavailable: "+stampError); }
  var signP=signCell.getChild(0).asParagraph(); signP.setAlignment(DocumentApp.HorizontalAlignment.RIGHT);
  signP.appendText("For Sri Lakshmi Ganapathi\n").setForegroundColor(ink).setFontSize(9).setBold(true);
  signP.appendText("Authorised Signatory").setForegroundColor(muted).setFontSize(8);

  var footer=body.appendParagraph("Thank you for choosing Sri Lakshmi Ganapathi Ayurveda Nilayam.");
  footer.setAlignment(DocumentApp.HorizontalAlignment.CENTER).setForegroundColor(green).setFontSize(8).setItalic(true).setSpacingBefore(12);
  doc.saveAndClose();
  var file=DriveApp.getFileById(doc.getId());
  try { return file.getAs(MimeType.PDF).setName(orderId+".pdf"); }
  finally { file.setTrashed(true); }
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
