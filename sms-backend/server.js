const express = require("express");
const admin = require("firebase-admin");

const app = express();
app.use(express.json({ limit: "10kb" }));

const requiredEnv = [
  "EXOTEL_API_KEY",
  "EXOTEL_API_TOKEN",
  "EXOTEL_ACCOUNT_SID",
  "EXOTEL_REGION",
  "EXOTEL_SENDER_ID",
  "DLT_ENTITY_ID",
  "DLT_TEMPLATE_ID",
  "FIREBASE_SERVICE_ACCOUNT_JSON",
  "ADMIN_UIDS"
];

const missingEnv = requiredEnv.filter((name) => !process.env[name]);
if (missingEnv.length) {
  console.warn("Missing required environment variables:", missingEnv.join(", "));
}

if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
  try {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
  } catch (error) {
    console.error("FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON.");
    process.exit(1);
  }
}

const adminUids = new Set(
  (process.env.ADMIN_UIDS || "")
    .split(",")
    .map((uid) => uid.trim())
    .filter(Boolean)
);

const regionHosts = {
  mumbai: "api.in.exotel.com",
  singapore: "api.exotel.com"
};

app.get("/", (_req, res) => {
  res.json({
    service: "SLGAN SMS backend",
    status: "running",
    smsConfigured: requiredEnv.every((name) => Boolean(process.env[name]))
  });
});

async function requireAdmin(req, res, next) {
  try {
    if (!admin.apps.length) {
      return res.status(503).json({ error: "Firebase authentication is not configured." });
    }
    const header = req.headers.authorization || "";
    const match = header.match(/^Bearer (.+)$/i);
    if (!match) {
      return res.status(401).json({ error: "Missing Firebase ID token." });
    }
    const decoded = await admin.auth().verifyIdToken(match[1]);
    if (!adminUids.has(decoded.uid)) {
      return res.status(403).json({ error: "Admin access required." });
    }
    req.authUser = decoded;
    next();
  } catch (_error) {
    return res.status(401).json({ error: "Invalid or expired Firebase ID token." });
  }
}

app.post("/send-sms", requireAdmin, async (req, res) => {
  const { to, body, orderId } = req.body || {};

  if (typeof to !== "string" || !/^\+?[1-9]\d{9,14}$/.test(to.replace(/[\s()-]/g, ""))) {
    return res.status(400).json({ error: "Provide a valid destination phone number in international format." });
  }
  if (typeof body !== "string" || body.trim().length < 1 || body.length > 1000) {
    return res.status(400).json({ error: "SMS body must be between 1 and 1000 characters." });
  }
  if (!orderId || typeof orderId !== "string" || orderId.length > 100) {
    return res.status(400).json({ error: "A valid orderId is required for audit logging." });
  }

  const missing = requiredEnv.filter((name) => !process.env[name]);
  if (missing.length) {
    return res.status(503).json({ error: "SMS service is not configured yet." });
  }

  const host = regionHosts[(process.env.EXOTEL_REGION || "").toLowerCase()];
  if (!host) {
    return res.status(500).json({ error: "EXOTEL_REGION must be mumbai or singapore." });
  }

  try {
    const url = `https://${encodeURIComponent(process.env.EXOTEL_API_KEY)}:${encodeURIComponent(process.env.EXOTEL_API_TOKEN)}@${host}/v1/Accounts/${encodeURIComponent(process.env.EXOTEL_ACCOUNT_SID)}/Sms/send.json`;
    const form = new URLSearchParams({
      From: process.env.EXOTEL_SENDER_ID,
      To: to.replace(/[\s()-]/g, ""),
      Body: body.trim(),
      DltEntityId: process.env.DLT_ENTITY_ID,
      DltTemplateId: process.env.DLT_TEMPLATE_ID,
      SmsType: "transactional",
      CustomField: orderId
    });

    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: form
    });

    const responseText = await response.text();
    if (!response.ok) {
      console.error("Exotel SMS request failed:", response.status, responseText.slice(0, 500));
      return res.status(502).json({ error: "Exotel rejected the SMS request.", providerStatus: response.status });
    }

    console.log("SMS request accepted", {
      orderId,
      adminUid: req.authUser.uid,
      providerStatus: response.status
    });
    return res.status(200).json({ status: "accepted", message: "Exotel accepted the SMS request." });
  } catch (error) {
    console.error("SMS request failed:", error.message);
    return res.status(502).json({ error: "Unable to reach Exotel." });
  }
});

const port = Number(process.env.PORT || 3000);
app.listen(port, "0.0.0.0", () => {
  console.log(`SLGAN SMS backend listening on port ${port}`);
});