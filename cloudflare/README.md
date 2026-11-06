# SLGAN Cashfree Worker

This Worker serves the existing Cashfree Sandbox checkout and the separately isolated Cashfree Production checkout. It uses the existing Firebase project and Firestore REST API; it does not require Firebase Cloud Functions or a Firebase Blaze upgrade.

## Deploy

1. Open the existing `slgan-cashfree-api` Worker in Cloudflare.
2. Replace its editor source with `cloudflare/worker.js`.
3. Add or retain the following Worker secrets/variables:

| Name | Type | Purpose |
| --- | --- | --- |
| `FIREBASE_WEB_API_KEY` | Secret or variable | Firebase project's Web API key |
| `FIREBASE_SERVICE_ACCOUNT` | Secret | Service-account JSON with Firestore access |
| `CASHFREE_APP_ID` | Secret | Existing Cashfree Sandbox App ID |
| `CASHFREE_SECRET_KEY` | Secret | Existing Cashfree Sandbox Secret Key |
| `CASHFREE_LIVE_APP_ID` | Secret | Cashfree Production App ID |
| `CASHFREE_LIVE_SECRET_KEY` | Secret | Cashfree Production Secret Key |

Do not put credentials in this repository, Android code, Firestore, or screenshots. Keep the Sandbox values unchanged. The Worker uses the Live secret both for Production API authentication and for verifying Production webhook signatures.

4. Configure the Cashfree Production webhook URL as:
   `https://slgan-cashfree-api.amruth4official.workers.dev/cashfree-webhook`
5. Deploy the Worker.
6. Test Sandbox order creation, checkout, verification, and webhook first. Then test Production with a controlled low-value transaction and confirm the Firestore order fields before enabling Live checkout in the app.

## Routes

- `POST /create-order` and `POST /verify-payment` require a Firebase ID token and JSON containing `documentId` and `environment` (`sandbox` or `production`).
- Omitting `environment` defaults to `sandbox` for backward compatibility.
- `POST /cashfree-webhook` accepts Cashfree webhooks and validates the signature against the Sandbox and Production secrets before processing the matching environment.

## Environment isolation

- Sandbox uses `https://sandbox.cashfree.com/pg`, `CASHFREE_APP_ID`, `CASHFREE_SECRET_KEY`, and the existing `cashfreeOrderId` field.
- Production uses `https://api.cashfree.com/pg`, `CASHFREE_LIVE_APP_ID`, `CASHFREE_LIVE_SECRET_KEY`, and a separate `cashfreeLiveOrderId` field.
- Payment verification checks the requested environment against the order's `paymentEnvironment`, and webhooks query the environment-specific order ID.
- A successful payment is written to Firestore only after server-side Cashfree verification and amount/currency checks.

## App activation gate

The Android admin Live switch and customer Live checkout are intentionally hard-disabled by `CASHFREE_LIVE_BACKEND_READY = false` in `MainActivity.kt`. Keep it false until the Worker is deployed, secrets and webhook are configured, and Production verification has been tested. Only then change the constant to `true` in a separate reviewed commit. The Admin `liveEnabled` setting remains off by default.

## Important

The Worker uses the existing Firebase service account pattern from the deployed Worker. Ensure that service account remains limited to the Firestore access required by the app. Never paste its private key or Cashfree secret values into GitHub issues, chat, or source files.
