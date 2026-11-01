/**
 * SLGAN email OTP endpoint.
 *
 * Required Script Property:
 *   FIREBASE_SERVICE_ACCOUNT_JSON = full JSON for a Firebase service account
 *     from the same Firebase project as the Android app.
 *
 * Deploy as a Web App (execute as the deploying account; access: Anyone).
 * Keep the service-account JSON in Script Properties only. Never put it in
 * Android, GitHub, or the public deployment source.
 */
const OTP_EXPIRY_MS = 5 * 60 * 1000;
const OTP_RESEND_WAIT_MS = 60 * 1000;
const OTP_MAX_ATTEMPTS = 5;
const OTP_DAILY_SEND_LIMIT = 5;
const FIREBASE_CUSTOM_TOKEN_AUDIENCE =
  "https://identitytoolkit.googleapis.com/google.identity.identitytoolkit.v1.IdentityToolkit";

function doGet() {
  return json({ ok: true, service: "SLGAN EMAIL OTP AUTH" });
}

function doPost(e) {
  try {
    const data = JSON.parse((e && e.postData && e.postData.contents) || "{}");
    const action = String(data.action || "");
    const email = String(data.email || "").trim().toLowerCase();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return json({ ok: false, message: "Enter a valid email address." });
    }
    if (action === "send") return sendOtp(email);
    if (action === "verify") return verifyOtp(email, String(data.code || "").trim());
    return json({ ok: false, message: "Unknown action." });
  } catch (err) {
    console.error("OTP request failed: " + err);
    return json({ ok: false, message: "Request could not be processed." });
  }
}

function sendOtp(email) {
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const props = PropertiesService.getScriptProperties();
    const key = emailKey(email);
    const now = Date.now();
    const old = JSON.parse(props.getProperty(key) || "{}");
    if (old.lastSent && now - old.lastSent < OTP_RESEND_WAIT_MS) {
      return json({ ok: false, message: "Please wait 60 seconds before requesting another code." });
    }

    const dayKey = "DAY_" + key;
    const daily = JSON.parse(props.getProperty(dayKey) || '{"date":"","count":0}');
    const today = Utilities.formatDate(new Date(), "UTC", "yyyy-MM-dd");
    if (daily.date !== today) { daily.date = today; daily.count = 0; }
    if (daily.count >= OTP_DAILY_SEND_LIMIT) {
      return json({ ok: false, message: "Daily email code limit reached. Try again tomorrow." });
    }

    // Generate the OTP from UUID entropy and keep only decimal digits.
    const code = Utilities.getUuid().replace(/[^0-9]/g, "").slice(0, 6).padEnd(6, "7");
    const salt = getOtpSecret();
    const digest = hashCode(email, code, salt);
    props.setProperty(key, JSON.stringify({
      digest: digest,
      expiresAt: now + OTP_EXPIRY_MS,
      lastSent: now,
      attempts: 0
    }));
    daily.count++;
    props.setProperty(dayKey, JSON.stringify(daily));

    try {
      GmailApp.sendEmail(
        email,
        "Your SLGAN verification code",
        "Your SLGAN verification code is: " + code +
          "\\n\\nThis code expires in 5 minutes. Do not share it with anyone.",
        {
          htmlBody: otpEmailHtml(code),
          name: "Sri Lakshmi Ganapathi Ayurveda Nilayam"
        }
      );
    } catch (err) {
      props.deleteProperty(key);
      return json({ ok: false, message: "Email could not be sent. Check Gmail authorization." });
    }
    return json({ ok: true, message: "Verification code sent. Check your inbox." });
  } finally {
    lock.releaseLock();
  }
}

const SLGAN_OTP_LOGO_URL = "https://raw.githubusercontent.com/pyaraallamraju-spec/SLGAN/main/app/src/main/res/drawable-nodpi/slgan_logo_true_transparent.png";
function otpEmailHtml(code) {
  return '<!doctype html><html><body style="margin:0;padding:24px 10px;background:#f3f5ed;font-family:Arial,Helvetica,sans-serif;color:#203b2b">' +
    '<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:560px;margin:0 auto;background:#fffdf6;border:1px solid #e8dfc5;border-radius:18px;overflow:hidden">' +
    '<tr><td style="padding:22px;text-align:center;border-bottom:3px solid #c9a84f"><img src="' + SLGAN_OTP_LOGO_URL + '" alt="SLGAN logo" width="76" height="76" style="display:block;margin:0 auto 10px">' +
    '<div style="font-size:22px;font-weight:700;color:#173d28">Sri Lakshmi Ganapathi</div><div style="font-size:15px;color:#365641">Ayurveda Nilayam</div><div style="font-size:11px;color:#9b792e;margin-top:6px">Estd. 1900 by Perraju Sharma</div></td></tr>' +
    '<tr><td style="padding:24px;background:#174b2d;color:#fffdf2;border-bottom:4px solid #d5b65d"><div style="font-size:24px;font-weight:700">Verify your email</div><div style="font-size:14px;margin-top:6px;color:#f6efd9">Use this secure code to continue to your SLGAN account.</div></td></tr>' +
    '<tr><td style="padding:30px 24px;text-align:center"><p style="margin:0 0 16px;font-size:15px">Your 6-digit verification code</p><div style="display:inline-block;padding:15px 25px;background:#f5f0dc;border:1px solid #d7bd70;border-radius:12px;color:#174b2d;font-size:32px;letter-spacing:8px;font-weight:700">' + code + '</div>' +
    '<p style="margin:20px 0 8px;font-size:14px;color:#596b5d">This code expires in <b>5 minutes</b>.</p><p style="margin:0;font-size:13px;color:#687565">Never share this code with anyone. If you did not request it, you can safely ignore this email.</p></td></tr>' +
    '<tr><td style="padding:18px;background:#174b2d;color:#fffdf2;text-align:center;border-top:4px solid #d5b65d;font-size:12px">Sri Lakshmi Ganapathi Ayurveda Nilayam<br>U. Kothapalli – 533447, Andhra Pradesh, India</td></tr></table></body></html>';
}

function verifyOtp(email, code) {
  if (!/^\d{6}$/.test(code)) return json({ ok: false, message: "Enter the 6-digit code." });
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const props = PropertiesService.getScriptProperties();
    const key = emailKey(email);
    const record = JSON.parse(props.getProperty(key) || "{}");
    if (!record.digest) return json({ ok: false, message: "Request a new verification code." });
    if (Date.now() > record.expiresAt) {
      props.deleteProperty(key);
      return json({ ok: false, message: "Code expired. Request a new one." });
    }
    if (record.attempts >= OTP_MAX_ATTEMPTS) {
      props.deleteProperty(key);
      return json({ ok: false, message: "Too many attempts. Request a new code." });
    }

    record.attempts++;
    if (record.digest !== hashCode(email, code, getOtpSecret())) {
      props.setProperty(key, JSON.stringify(record));
      return json({ ok: false, message: "Incorrect code." });
    }

    // Consume the OTP before creating the token, preventing replay.
    props.deleteProperty(key);
    const customToken = createFirebaseCustomToken(email);
    return json({
      ok: true,
      verified: true,
      email: email,
      customToken: customToken,
      message: "Email verified. Signing you in."
    });
  } catch (err) {
    console.error("OTP verification/token creation failed: " + err);
    return json({ ok: false, message: "Email verified, but sign-in could not be completed. Request a new code." });
  } finally {
    lock.releaseLock();
  }
}

function createFirebaseCustomToken(email) {
  const raw = PropertiesService.getScriptProperties().getProperty("FIREBASE_SERVICE_ACCOUNT_JSON");
  if (!raw) throw new Error("Missing FIREBASE_SERVICE_ACCOUNT_JSON Script Property.");
  const serviceAccount = JSON.parse(raw);
  if (!serviceAccount.project_id || !serviceAccount.client_email || !serviceAccount.private_key) {
    throw new Error("Service-account JSON is incomplete.");
  }

  // Firebase Auth custom tokens require a stable UID. The email is normalized
  // and valid as a UID; profile data is written by the Android client after sign-in.
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const claims = {
    iss: serviceAccount.client_email,
    sub: serviceAccount.client_email,
    aud: FIREBASE_CUSTOM_TOKEN_AUDIENCE,
    iat: now,
    exp: now + 55 * 60,
    uid: email
  };
  const unsigned = base64Url(JSON.stringify(header)) + "." + base64Url(JSON.stringify(claims));
  const signature = Utilities.computeRsaSha256Signature(unsigned, serviceAccount.private_key);
  return unsigned + "." + Utilities.base64EncodeWebSafe(signature).replace(/=+$/g, "");
}

function getOtpSecret() {
  const props = PropertiesService.getScriptProperties();
  let secret = props.getProperty("OTP_HASH_SECRET");
  if (!secret) {
    secret = Utilities.getUuid() + Utilities.getUuid();
    props.setProperty("OTP_HASH_SECRET", secret);
  }
  return secret;
}

function emailKey(email) {
  const bytes = Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, email);
  return "OTP_" + Utilities.base64EncodeWebSafe(bytes).replace(/=+$/g, "");
}

function hashCode(email, code, secret) {
  const bytes = Utilities.computeDigest(
    Utilities.DigestAlgorithm.SHA_256,
    email + ":" + code + ":" + secret
  );
  return Utilities.base64EncodeWebSafe(bytes).replace(/=+$/g, "");
}

function base64Url(value) {
  return Utilities.base64EncodeWebSafe(value, Utilities.Charset.UTF_8).replace(/=+$/g, "");
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
