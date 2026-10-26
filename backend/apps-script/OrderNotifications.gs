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
      return jsonOut({ success: true, ok: true, message: result });
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
      GmailApp.sendEmail(order.email, "SLGAN order status: " + status.toUpperCase() + " — " + orderId,
        "Dear " + (order.customerName || "Customer") + ",\n\n" + message +
        "\n\nOrder ID: " + orderId + "\nCurrent status: " + status.toUpperCase() +
        "\n\nSri Lakshmi Ganapathi Ayurvedic Nilayam");
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
    GmailApp.sendEmail({
      to: String(order.email),
      subject: "SLGAN order confirmation and invoice — " + orderId,
      body: "Thank you for placing an order with Sri Lakshmi Ganapathi Ayurvedic Nilayam. Your order confirmation PDF is attached. Order status: " + (order.orderStatus || "pending verification") + ". No online payment has been collected.",
      htmlBody: "<p>Thank you for your order.</p><p><b>Order ID:</b> " + escapeHtml(orderId) + "<br><b>Status:</b> " + escapeHtml(order.orderStatus || "pending verification") + "</p><p>Your order confirmation PDF is attached. No online payment has been collected.</p><p>Sri Lakshmi Ganapathi Ayurvedic Nilayam</p>",
      attachments: [pdf],
      name: "Sri Lakshmi Ganapathi Ayurvedic Nilayam"
    });
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
  const doc = DocumentApp.create("SLGAN " + orderId + " invoice");
  const body = doc.getBody();
  body.appendParagraph("Sri Lakshmi Ganapathi Ayurvedic Nilayam").setHeading(DocumentApp.ParagraphHeading.HEADING1);
  body.appendParagraph("ORDER CONFIRMATION / PROVISIONAL INVOICE").setHeading(DocumentApp.ParagraphHeading.HEADING2);
  body.appendParagraph("Order ID: " + orderId);
  body.appendParagraph("Date: " + Utilities.formatDate(new Date(), "Asia/Kolkata", "dd MMM yyyy, hh:mm a"));
  body.appendParagraph("Customer: " + (order.customerName || ""));
  body.appendParagraph("Email: " + (order.email || ""));
  body.appendParagraph("Phone: " + (order.phone || order.deliveryPhone || ""));
  body.appendParagraph("Delivery address: " + (order.shippingAddress || order.deliveryAddress || ""));
  const tableRows = [["Product", "Qty", "Unit price", "Amount"]];
  items.forEach(function(it) {
    const qty = Number(it.quantity) || 0;
    const unit = Number(it.unitPrice != null ? it.unitPrice : it.price) || 0;
    const line = Number(it.lineTotal != null ? it.lineTotal : unit * qty) || 0;
    tableRows.push([String(it.productName || it.name || "Product"), String(qty), "₹" + unit.toFixed(2), "₹" + line.toFixed(2)]);
  });
  body.appendTable(tableRows);
  body.appendParagraph("Product total: ₹" + total.toFixed(2)).setHeading(DocumentApp.ParagraphHeading.HEADING2);
  body.appendParagraph("Order status: " + (order.orderStatus || "pending verification"));
  body.appendParagraph("No online payment was collected. This document confirms receipt of the order request; it is not a final tax invoice. Delivery charges, applicable taxes and the final tax invoice, if required, will be confirmed by the seller.");
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
