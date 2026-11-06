const FIREBASE_PROJECT_ID = "slgan-a364";
const CASHFREE_API_VERSION = "2025-01-01";
const CASHFREE_BASES = {
  sandbox: "https://sandbox.cashfree.com/pg",
  production: "https://api.cashfree.com/pg",
};

export default {
  async fetch(request, env) {
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization, x-webhook-signature, x-webhook-timestamp",
    };
    if (request.method === "OPTIONS") return new Response(null, { headers: cors });
    const url = new URL(request.url);
    try {
      if (request.method !== "POST") return json({ error: "Method not allowed" }, 405, cors);
      if (url.pathname === "/cashfree-webhook") return await handleWebhook(request, env, cors);
      const uid = await authenticate(request, env);
      if (url.pathname === "/create-order") return await createOrder(request, env, uid, cors);
      if (url.pathname === "/verify-payment") return await verifyPayment(request, env, uid, cors);
      return json({ error: "Not found" }, 404, cors);
    } catch (error) {
      console.error("Worker error:", error?.message || error);
      return json({ error: error?.message || "Internal server error" }, 500, cors);
    }
  },
};

function json(data, status = 200, headers = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json", ...headers },
  });
}

function normalizeEnvironment(value) {
  const environment = value || "sandbox";
  if (environment !== "sandbox" && environment !== "production") {
    throw new Error("Invalid payment environment");
  }
  return environment;
}

function cashfreeCredentials(env, environment) {
  if (environment === "production") {
    if (!env.CASHFREE_LIVE_APP_ID || !env.CASHFREE_LIVE_SECRET_KEY) {
      throw new Error("Cashfree Live backend is not configured");
    }
    return {
      base: CASHFREE_BASES.production,
      appId: env.CASHFREE_LIVE_APP_ID,
      secret: env.CASHFREE_LIVE_SECRET_KEY,
    };
  }
  if (!env.CASHFREE_APP_ID || !env.CASHFREE_SECRET_KEY) {
    throw new Error("Cashfree Sandbox backend is not configured");
  }
  return {
    base: CASHFREE_BASES.sandbox,
    appId: env.CASHFREE_APP_ID,
    secret: env.CASHFREE_SECRET_KEY,
  };
}

async function authenticate(request, env) {
  const authorization = request.headers.get("Authorization") || "";
  const idToken = authorization.replace(/^Bearer\s+/i, "");
  if (!idToken || idToken === authorization) throw new Error("Firebase login token required");

  const response = await fetch(
    "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=" + encodeURIComponent(env.FIREBASE_WEB_API_KEY),
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idToken }),
    }
  );
  const data = await response.json();
  if (!response.ok || !data.users?.[0]?.localId) throw new Error("Invalid or expired Firebase login");
  return data.users[0].localId;
}

async function getGoogleAccessToken(env) {
  const account = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT);
  const now = Math.floor(Date.now() / 1000);
  const encode = value => btoa(String.fromCharCode(...new TextEncoder().encode(value)))
    .replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
  const header = encode(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = encode(JSON.stringify({
    iss: account.client_email,
    scope: "https://www.googleapis.com/auth/datastore",
    aud: account.token_uri,
    iat: now,
    exp: now + 3600,
  }));
  const unsigned = header + "." + claim;
  const pem = account.private_key
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s/g, "");
  const binary = Uint8Array.from(atob(pem), c => c.charCodeAt(0));
  const privateKey = await crypto.subtle.importKey(
    "pkcs8",
    binary,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    privateKey,
    new TextEncoder().encode(unsigned)
  );
  const signed = unsigned + "." + btoa(String.fromCharCode(...new Uint8Array(signature)))
    .replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
  const response = await fetch(account.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: signed,
    }),
  });
  const data = await response.json();
  if (!response.ok || !data.access_token) throw new Error("Could not authenticate with Firebase");
  return data.access_token;
}

async function firestoreRequest(env, path, options = {}) {
  const token = await getGoogleAccessToken(env);
  const response = await fetch(
    "https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID +
      "/databases/(default)/documents/" + path,
    {
      ...options,
      headers: {
        Authorization: "Bearer " + token,
        "Content-Type": "application/json",
        ...(options.headers || {}),
      },
    }
  );
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error?.message || "Firestore request failed");
  return data;
}

function decodeFields(fields = {}) {
  const result = {};
  for (const [key, value] of Object.entries(fields)) {
    if ("stringValue" in value) result[key] = value.stringValue;
    else if ("integerValue" in value) result[key] = Number(value.integerValue);
    else if ("doubleValue" in value) result[key] = Number(value.doubleValue);
    else if ("booleanValue" in value) result[key] = value.booleanValue;
    else if ("timestampValue" in value) result[key] = value.timestampValue;
    else if ("nullValue" in value) result[key] = null;
    else if ("mapValue" in value) result[key] = decodeFields(value.mapValue.fields);
    else if ("arrayValue" in value) {
      result[key] = (value.arrayValue.values || []).map(item => decodeFields({ value: item }).value);
    }
  }
  return result;
}

function encodeValue(value) {
  if (value === null) return { nullValue: null };
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "boolean") return { booleanValue: value };
  if (Number.isInteger(value)) return { integerValue: String(value) };
  if (typeof value === "number") return { doubleValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encodeValue) } };
  if (typeof value === "object") return { mapValue: { fields: encodeFields(value) } };
  throw new Error("Unsupported Firestore value");
}

function encodeFields(fields) {
  const result = {};
  for (const [key, value] of Object.entries(fields)) result[key] = encodeValue(value);
  return result;
}

async function readOrder(env, documentId) {
  if (!documentId || !/^[A-Za-z0-9_-]{1,150}$/.test(documentId)) {
    throw new Error("Invalid order document ID");
  }
  const doc = await firestoreRequest(env, "orders/" + documentId);
  return { doc, data: decodeFields(doc.fields || {}) };
}

async function patchOrder(env, documentId, fields) {
  const mask = Object.keys(fields)
    .map(key => "updateMask.fieldPaths=" + encodeURIComponent(key))
    .join("&");
  const token = await getGoogleAccessToken(env);
  const response = await fetch(
    "https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID +
      "/databases/(default)/documents/orders/" + documentId + "?" + mask,
    {
      method: "PATCH",
      headers: {
        Authorization: "Bearer " + token,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ fields: encodeFields(fields) }),
    }
  );
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error?.message || "Could not update order");
  return data;
}

async function cashfreeRequest(env, environment, path, options = {}) {
  const credentials = cashfreeCredentials(env, environment);
  const response = await fetch(credentials.base + path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "x-client-id": credentials.appId,
      "x-client-secret": credentials.secret,
      "x-api-version": CASHFREE_API_VERSION,
      ...(options.headers || {}),
    },
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    console.error("Cashfree error:", JSON.stringify(data));
    throw new Error(data.message || data.type || "Cashfree request failed");
  }
  return data;
}

function orderCashfreeId(order, environment) {
  return environment === "production" ? order.cashfreeLiveOrderId : order.cashfreeOrderId;
}

async function createOrder(request, env, uid, cors) {
  const body = await request.json();
  const environment = normalizeEnvironment(body.environment);
  const { data: order } = await readOrder(env, body.documentId);
  if (order.customerId !== uid) return json({ error: "You do not own this order" }, 403, cors);
  if (!["not_collected", "unpaid"].includes(order.paymentStatus)) {
    return json({ error: "This order is not awaiting payment" }, 409, cors);
  }
  if (order.paymentGateway && order.paymentGateway !== "cashfree") {
    return json({ error: "This order is not configured for Cashfree" }, 409, cors);
  }
  if (order.paymentEnvironment && order.paymentEnvironment !== environment) {
    return json({ error: "Order payment environment does not match checkout" }, 409, cors);
  }
  const amount = Number(order.grandTotal);
  if (!Number.isFinite(amount) || amount <= 0) throw new Error("Invalid order amount");

  let cashfreeOrderId = orderCashfreeId(order, environment);
  if (!cashfreeOrderId) {
    const prefix = environment === "production" ? "slgan_live_" : "slgan_test_";
    cashfreeOrderId = (prefix + body.documentId + "_" + Date.now()).slice(0, 45);
    await patchOrder(env, body.documentId, environment === "production"
      ? { cashfreeLiveOrderId: cashfreeOrderId, paymentGateway: "cashfree", paymentEnvironment: "production" }
      : { cashfreeOrderId, paymentGateway: "cashfree", paymentEnvironment: "sandbox" });
  }

  const customerId = String(uid).replace(/[^A-Za-z0-9_-]/g, "").slice(0, 50);
  const cashfreeOrder = await cashfreeRequest(env, environment, "/orders", {
    method: "POST",
    headers: { "x-idempotency-key": cashfreeOrderId },
    body: JSON.stringify({
      order_id: cashfreeOrderId,
      order_amount: Number(amount.toFixed(2)),
      order_currency: order.currency || "INR",
      customer_details: {
        customer_id: customerId,
        customer_name: String(order.customerName || "Customer").slice(0, 100),
        customer_email: order.email || "customer@example.com",
        customer_phone: String(order.phone || "").replace(/\D/g, "").slice(-10),
      },
      order_note: "SLGAN order " + (order.orderId || body.documentId),
      order_meta: { notify_url: new URL(request.url).origin + "/cashfree-webhook" },
    }),
  });

  return json({
    success: true,
    order_id: cashfreeOrder.order_id,
    payment_session_id: cashfreeOrder.payment_session_id,
    order_amount: cashfreeOrder.order_amount,
    order_currency: cashfreeOrder.order_currency,
    environment,
  }, 200, cors);
}

async function verifyPayment(request, env, uid, cors) {
  const body = await request.json();
  const environment = normalizeEnvironment(body.environment);
  const { data: order } = await readOrder(env, body.documentId);
  if (order.customerId !== uid) return json({ error: "You do not own this order" }, 403, cors);
  if (order.paymentStatus === "paid") return json({ success: true, status: "PAID" }, 200, cors);
  if (order.paymentEnvironment !== environment) {
    return json({ error: "Order payment environment does not match verification" }, 409, cors);
  }
  const cashfreeOrderId = orderCashfreeId(order, environment);
  if (!cashfreeOrderId) throw new Error("Payment order has not been created");

  const payments = await cashfreeRequest(
    env, environment, "/orders/" + encodeURIComponent(cashfreeOrderId) + "/payments"
  );
  const successful = Array.isArray(payments)
    ? payments.find(payment => payment.payment_status === "SUCCESS")
    : null;
  if (!successful) {
    const pending = Array.isArray(payments) &&
      payments.some(payment => ["PENDING", "NOT_ATTEMPTED"].includes(payment.payment_status));
    return json({ success: false, status: pending ? "PENDING" : "NOT_PAID" }, 200, cors);
  }

  const expectedAmount = Number(order.grandTotal);
  const paidAmount = Number(successful.payment_amount);
  const expectedCurrency = order.currency || "INR";
  if (Math.abs(paidAmount - expectedAmount) > 0.01 ||
      successful.payment_currency !== expectedCurrency) {
    throw new Error("Payment amount or currency mismatch");
  }
  await markOrderPaid(env, body.documentId, successful, environment);
  return json({ success: true, status: "PAID" }, 200, cors);
}

async function markOrderPaid(env, documentId, payment, environment) {
  await patchOrder(env, documentId, {
    paymentStatus: "paid",
    paymentGateway: "cashfree",
    cashfreePaymentId: String(payment.cf_payment_id || ""),
    paymentMethod: String(payment.payment_group || ""),
    paymentCompletedAt: new Date().toISOString(),
    orderStatus: "received",
    paymentEnvironment: environment,
  });
}

async function verifyWebhookSignature(rawBody, timestamp, signature, secret) {
  if (!secret) return false;
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signed = await crypto.subtle.sign(
    "HMAC", key, new TextEncoder().encode(timestamp + rawBody)
  );
  const expected = btoa(String.fromCharCode(...new Uint8Array(signed)));
  return constantTimeEqual(signature, expected);
}

async function handleWebhook(request, env, cors) {
  const signature = request.headers.get("x-webhook-signature") || "";
  const timestamp = request.headers.get("x-webhook-timestamp") || "";
  if (!signature || !timestamp) return json({ error: "Missing webhook signature" }, 400, cors);

  const rawBody = await request.text();
  let environment = null;
  if (await verifyWebhookSignature(rawBody, timestamp, signature, env.CASHFREE_SECRET_KEY)) {
    environment = "sandbox";
  } else if (await verifyWebhookSignature(rawBody, timestamp, signature, env.CASHFREE_LIVE_SECRET_KEY)) {
    environment = "production";
  }
  if (!environment) return json({ error: "Invalid webhook signature" }, 401, cors);

  const event = JSON.parse(rawBody);
  const orderId = event.data?.order?.order_id;
  const payment = event.data?.payment;
  if (!orderId || !payment || payment.payment_status !== "SUCCESS") {
    return json({ received: true }, 200, cors);
  }

  const field = environment === "production" ? "cashfreeLiveOrderId" : "cashfreeOrderId";
  const query = await firestoreRequest(env, ":runQuery", {
    method: "POST",
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: "orders" }],
        where: {
          fieldFilter: {
            field: { fieldPath: field },
            op: "EQUAL",
            value: { stringValue: orderId },
          },
        },
        limit: 1,
      },
    }),
  });
  const found = query.find(item => item.document);
  if (!found) return json({ error: "Order not found" }, 404, cors);

  const documentId = found.document.name.split("/").pop();
  const order = decodeFields(found.document.fields || {});
  if (order.paymentStatus === "paid") {
    return json({ received: true, alreadyProcessed: true }, 200, cors);
  }
  if (order.paymentGateway !== "cashfree" || order.paymentEnvironment !== environment) {
    return json({ error: "Payment environment mismatch" }, 400, cors);
  }
  const expectedAmount = Number(order.grandTotal);
  const paidAmount = Number(payment.payment_amount);
  const expectedCurrency = order.currency || "INR";
  if (Math.abs(paidAmount - expectedAmount) > 0.01 ||
      payment.payment_currency !== expectedCurrency) {
    return json({ error: "Payment details mismatch" }, 400, cors);
  }
  await markOrderPaid(env, documentId, payment, environment);
  return json({ received: true }, 200, cors);
}

function constantTimeEqual(a, b) {
  if (a.length !== b.length) return false;
  let result = 0;
  for (let index = 0; index < a.length; index++) {
    result |= a.charCodeAt(index) ^ b.charCodeAt(index);
  }
  return result === 0;
}
