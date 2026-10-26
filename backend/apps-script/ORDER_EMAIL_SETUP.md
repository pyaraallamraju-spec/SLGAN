# SLGAN customer order emails (Firebase Spark-compatible)

The order email service is in `backend/apps-script/OrderNotifications.gs`. It is separate from `EmailOtp.gs` because an Apps Script project can only have one `doPost` handler per project. Deploy it as its own Google Apps Script Web App.

## What it does

- `orderPlaced`: verifies the Firebase ID token, checks that the order belongs to the signed-in customer, creates a PDF order confirmation/provisional invoice, emails it as an attachment, and records email/invoice status in the existing Firestore order.
- `statusUpdated`: verifies the Firebase ID token and checks the caller's `customers/{uid}.is_admin == true` profile before sending an accepted/rejected status email. The status must already be saved in Firestore.
- `retryInvoice`: admin-only retry for failed order confirmation emails.

The PDF is explicitly labelled a provisional order confirmation, not a final tax invoice, because the app currently collects no online payment and final tax/delivery charges may need shop confirmation.

## Required deployment steps

1. Open Google Apps Script while signed in to the account that will send SLGAN emails, create a **new standalone project** named `SLGAN Order Notifications`, and paste the full contents of `OrderNotifications.gs`.
2. Open **Project Settings → Script Properties** and add:
   - `FIREBASE_SERVICE_ACCOUNT_JSON`: the full service-account JSON for Firebase project `slgan-a364e`. Store it only in Script Properties; never commit it to GitHub or put it in Android.
   - `FIREBASE_WEB_API_KEY`: the Web API key from Firebase project settings for `slgan-a364e`.
3. Ensure the service account has permission to read/update Firestore documents in this project. Enable the Firestore API if Google prompts for it.
4. Run a test/deployment as the intended sender and approve the Gmail/UrlFetch permissions.
5. Click **Deploy → New deployment → Web app**; execute as **Me**. Choose access **Anyone** so signed-in app users can call it; the script independently verifies Firebase ID tokens and admin role. Copy the resulting `/exec` URL.
6. Update the `SLGAN_ORDER_EMAIL_WEB_APP_URL` constant in `app/src/main/java/com/pyara/slgan/MainActivity.kt` to that URL, replacing the previous order-email URL.
7. Update `ORDER_EMAIL_WEB_APP_URL` in `public/index.html` to the same URL. Redeploy Firebase Hosting after the change.
8. Test with a low-value test order using an email inbox you control. Confirm the PDF attachment arrives and Firestore has `invoiceStatus: generated` and `invoiceEmailStatus: sent`. Then test Accept and Reject from Admin Orders and confirm the corresponding status email. Do not treat a successful HTTP response alone as proof of inbox delivery.

## Important limitations

- The Apps Script source is committed, but Apps Script deployment and Script Properties cannot be configured from this GitHub repository. Until the Web App is deployed and its URL is inserted in both places, this feature is **not live**.
- Apps Script email quotas apply. Failed sends are recorded as failed; an admin can call the `retryInvoice` action through an authenticated request after deployment.
- This uses Apps Script/Gmail and Firestore REST; it does not require Firebase Cloud Functions, a payment gateway, or a change to the Firebase billing plan.