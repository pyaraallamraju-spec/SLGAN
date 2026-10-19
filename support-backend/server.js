import express from "express";
import { initializeApp, cert, getApps } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";

const app = express();
app.disable("x-powered-by");
app.use(express.json({ limit: "8kb" }));

const port = Number(process.env.PORT || 8080);
const apiKey = process.env.OPENAI_API_KEY;
const model = process.env.OPENAI_MODEL || "gpt-6-luna";

function initFirebaseAdmin() {
  if (getApps().length) return;
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!raw) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON is required");
  const serviceAccount = JSON.parse(raw);
  initializeApp({ credential: cert(serviceAccount) });
}
initFirebaseAdmin();

const windows = new Map();
function rateLimit(req, res, next) {
  const uid = req.user.uid;
  const now = Date.now();
  const recent = (windows.get(uid) || []).filter((t) => now - t < 60_000);
  if (recent.length >= 12) return res.status(429).json({ error: "Please wait before sending more messages." });
  recent.push(now);
  windows.set(uid, recent);
  next();
}

async function requireFirebaseUser(req, res, next) {
  const header = req.get("authorization") || "";
  const match = header.match(/^Bearer\s+(.+)$/i);
  if (!match) return res.status(401).json({ error: "Sign-in required." });
  try {
    req.user = await getAuth().verifyIdToken(match[1], true);
    next();
  } catch {
    return res.status(401).json({ error: "Session expired. Please sign in again." });
  }
}

app.get("/health", (_req, res) => res.json({ ok: true, service: "slgan-support" }));

app.post("/chat", requireFirebaseUser, rateLimit, async (req, res) => {
  const message = typeof req.body?.message === "string" ? req.body.message.trim() : "";
  const language = req.body?.language === "English" ? "English" : "Telugu and English";
  if (!message || message.length > 1000) return res.status(400).json({ error: "Message must contain 1–1000 characters." });
  if (!apiKey) return res.status(503).json({ error: "AI is not configured yet." });

  const instructions = `You are SLGAN Assistant for Sri Lakshmi Ganapathi Ayurveda Nilayam. Reply in the language used by the user (Telugu or English; mixed language is fine). Help with general app navigation, product catalogue, orders, invoices, and delivery. Never invent order status, product prices, ingredients, availability, or customer-specific information; say to check the app or contact support when you do not have that data. Do not diagnose, prescribe, recommend medicine dosages, claim an Ayurvedic product cures a disease, or advise stopping prescribed treatment. For medical questions, encourage consulting a qualified healthcare professional. Be concise, polite, and clear. Do not request passwords, OTPs, payment details, or unnecessary personal data. Preferred language setting: ${language}.`;

  try {
    const upstream = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model,
        instructions,
        input: message,
        max_output_tokens: 350,
        store: false
      }),
      signal: AbortSignal.timeout(25_000)
    });
    const data = await upstream.json().catch(() => ({}));
    if (!upstream.ok) {
      console.error("AI provider request failed:", upstream.status);
      return res.status(502).json({ error: "AI support is temporarily unavailable." });
    }
    const reply = (data.output || [])
      .flatMap((item) => item.content || [])
      .filter((item) => item.type === "output_text")
      .map((item) => item.text || "")
      .join("\n")
      .trim();
    if (!reply) return res.status(502).json({ error: "AI returned an empty reply." });
    return res.json({ reply });
  } catch (error) {
    console.error("AI support request failed:", error?.name || "unknown");
    return res.status(502).json({ error: "AI support is temporarily unavailable." });
  }
});

app.listen(port, "0.0.0.0", () => console.log(`SLGAN support backend listening on ${port}`));
