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


## Admin push notifications (Firebase Cloud Messaging)

The current source also supports admin-to-customer push announcements without Firebase Cloud Functions or a Blaze upgrade.

- Android Accounts → Notification preferences is ON by default. The choice is stored as `customers/{uid}.notificationsEnabled`.
- Android registers each device's FCM token in `customers/{uid}.fcmTokens`. Turning the preference OFF excludes every registered device for that customer from future admin campaigns.
- The Admin web panel has **Administration → Admin Notifications**. Admins can enter a title and message and send to opted-in customers.
- The Apps Script `sendPush` action verifies the Firebase ID token and the caller's `is_admin == true` role, reads customer preferences, and sends messages through FCM HTTP v1. Service-account credentials remain in Apps Script Script Properties.

### One-time setup for push delivery

1. In Google Cloud Console for project `slgan-a364e`, enable the **Firebase Cloud Messaging API**.
2. Ensure the service account stored in `FIREBASE_SERVICE_ACCOUNT_JSON` has permission to send Firebase Cloud Messaging HTTP v1 messages, in addition to its existing Firestore access.
3. Copy the latest `backend/apps-script/OrderNotifications.gs` into the existing standalone **SLGAN Order Notifications** Apps Script project.
4. Save, then choose **Deploy → Manage deployments → Edit → New version → Deploy**. Keep the existing Web App URL; the Android app and Admin web panel already call the shared `ORDER_EMAIL_WEB_APP_URL`.
5. Publish the updated `firestore.rules` from Firebase Console (**Firestore Database → Rules**) so customers can save `notificationsEnabled` and `fcmTokens`. The repository commit alone does not publish rules.
6. Build and install the new Android APK. Open Accounts once and allow Android's notification permission when prompted. A device whose Android notification permission is denied cannot display pushes even if the in-app preference is ON.
7. Sign in as an administrator on the SLGAN web panel, open **Admin Notifications**, and send a short test message to a test customer device.

No Cloud Functions, paid API, Firebase billing-plan change, or new server is required. Apps Script and FCM quotas/rate limits still apply. A campaign's result counts are reported in the Admin panel; a successful FCM send means accepted by FCM, not proof that the device displayed it.

## Updated branded email design

The current source now uses the SLGAN green, cream and gold email layout with the repository logo for:
- Customer order confirmation (PDF remains attached).
- Admin new-order notification with customer, address, item and total details.
- Accepted/rejected order status updates.

**Important:** Apps Script deployments are separate from GitHub. After updating this repository, copy the latest `OrderNotifications.gs` contents into the existing standalone Apps Script project, save, then **Deploy → Manage deployments → Edit → New version → Deploy**. No Script Properties need to change for the visual template update.

## Forgot-password email branding

The Android Forgot Password action now calls the existing Email OTP Apps Script Web App using its `passwordReset` action. That service generates a genuine Firebase password-reset action link with the Firebase service account and sends it through Gmail using the branded SLGAN HTML template. Follow the deployment and test instructions in `EMAIL_OTP_SETUP.md`.

The Firebase-generated default email is no longer requested by the app after this change. Keep the existing Firebase authentication password-reset capability enabled; the custom email contains a Firebase-issued action link and Firebase still performs the actual password change.
