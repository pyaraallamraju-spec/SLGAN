# SLGAN email OTP deployment

The Android app posts `send` and `verify` requests to the Apps Script Web App URL configured as `emailOtpScriptUrl` (default is the existing SLGAN deployment). A successful `verify` response must include `customToken` so Firebase Authentication can establish a real session.

## One-time setup

1. Open the existing SLGAN Apps Script project and replace its source with `EmailOtp.gs` from this folder.
2. In **Project Settings → Script Properties**, add `FIREBASE_SERVICE_ACCOUNT_JSON` with the complete service-account JSON for Firebase project `slgan-a364e`. Keep this value only in Script Properties. Never commit it or place it in Android.
3. Ensure the service account belongs to the same Firebase project as the Android app. Its private key is used server-side to sign Firebase custom tokens; no Firebase service-account key or API secret is embedded in the APK.
4. In Apps Script, run a function once if prompted to authorize Gmail and Script Properties. Confirm the sending account is allowed to send mail.
5. **Deploy → Manage deployments → Edit** the existing Web App deployment, select a new version, and deploy. Keep execution as the script owner and access set to **Anyone** so the Android app can call it. Apps Script protects the private key in script properties; OTPs are one-use, expire after five minutes, and are rate limited.
6. Test the deployment URL with a browser GET; it should return `{"ok":true,"service":"SLGAN EMAIL OTP AUTH"}`.
7. In Firebase Console → Authentication → Sign-in method, keep the providers needed for the app enabled (Google, Email/Password, and Phone). The custom-token flow uses Firebase Authentication and does not require Cloud Functions or Blaze billing.

## Optional endpoint override

The default endpoint is the previously configured SLGAN Apps Script URL. To override it, add this Gradle property to your local `~/.gradle/gradle.properties` (or CI Gradle properties):

```properties
emailOtpScriptUrl=https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec
```

Do not place the service-account JSON in Gradle properties, GitHub source, or the Android project.

## Important account behavior

The Apps Script creates a Firebase custom token whose UID is the normalized email address. Firebase Auth therefore creates/uses that UID for email-OTP sign-in; it does not automatically merge it with a separately-created email/password account that already has a different UID. Users who already have an email/password account should continue to use that method unless an account-linking/migration flow is implemented deliberately. Google Sign-In, email/password, and phone OTP code paths are otherwise retained.

## Security notes

- Never return the OTP itself from the endpoint.
- Never accept a client-supplied UID; the server derives the token UID from the verified email.
- Keep the service-account key in Script Properties and restrict who can edit the Apps Script project.
- Apps Script Web Apps are public endpoints, so the resend/daily limits and verification attempt limit in the script are important. Review quotas and abuse monitoring periodically.


## Updated branded OTP email design

The current source sends a responsive SLGAN-branded HTML verification email using the green, cream and gold palette and the repository logo. The 6-digit code, five-minute expiry and security warning are clearly highlighted.

**Important:** Apps Script deployments are separate from GitHub. Copy the latest `EmailOtp.gs` contents into the existing OTP Apps Script project, save, then **Deploy → Manage deployments → Edit → New version → Deploy**. Keep the existing Script Properties and deployment URL unchanged.


## Branded password reset email

The same deployed Email OTP Apps Script Web App now supports a `passwordReset` action. It uses the Firebase service account already stored in Script Properties to generate a genuine Firebase password-reset action link, then sends it with Gmail using the SLGAN green, cream and gold HTML template and logo.

No new Script Properties or separate deployment URL are required. The existing `FIREBASE_SERVICE_ACCOUNT_JSON` property is reused. The script requests the Google Identity Toolkit OAuth scope when generating a reset link; on the first redeployment, Apps Script may ask the owner to approve the additional Google service access.

After updating this file in the existing Apps Script project:
1. Keep the existing Script Properties and deployment URL.
2. Save the complete updated `EmailOtp.gs`.
3. Open **Deploy → Manage deployments → Edit → New version → Deploy**.
4. Test **Forgot Password** in SLGAN with an inbox you control. Confirm the email arrives, the button opens Firebase's password-reset page, and a new password can be saved.

The public response is intentionally the same whether or not the email belongs to an account, to avoid exposing registered users. Requests are rate-limited per email and globally. Do not disable the existing Firebase password reset until the branded flow has been tested successfully.
