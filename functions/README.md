# SLGAN checkout backend (Sandbox starter)

This folder adds Firebase Cloud Functions for server-priced Firestore orders, Cashfree Sandbox order/session creation, PDF order summaries emailed via Gmail SMTP, and server-side payment verification.

## Setup
1. Confirm the Firebase project supports Cloud Functions deployment; billing may be required.
2. Run `firebase init functions` at the repository root if Firebase project configuration is not already set up. Select TypeScript and preserve this folder's files.
3. Set secrets using Firebase CLI (never commit secrets):
   - `firebase functions:secrets:set CASHFREE_APP_ID`
   - `firebase functions:secrets:set CASHFREE_SECRET_KEY`
   - `firebase functions:secrets:set GMAIL_USER`
   - `firebase functions:secrets:set GMAIL_APP_PASSWORD`
4. Use Cashfree Sandbox credentials. For Gmail, enable 2-Step Verification and create a Google App Password; do not use the normal account password.
5. Run `cd functions && npm install && npm run build`, then deploy after selecting the correct Firebase project with `firebase deploy --only functions`.
6. The Android client must call `createCheckoutOrder` with `items: [{productId, quantity}]` and `shippingAddress: {fullName, phone, line1, line2, city, state, postalCode}`. Pass the returned `paymentSessionId` to the official Cashfree Android SDK. After checkout, call `verifyCheckoutPayment` with `orderId`.

## Data and safety
- Product prices are read from the server-side `products` collection (`product_name`/`product_price`, or fallback `name`/`price`). Product document IDs or `id` fields must match app product IDs.
- Shipping and discount are currently zero placeholders; configure the actual policies before production.
- The first email is explicitly a payment-pending order summary; after server-side verification a second email confirms payment.
- Configure the correct Firebase project and a genuine return URL before deployment. Do not trust Android callbacks as proof of payment.
- Keep Cashfree and Gmail secrets in Firebase Secret Manager, never in source code or the APK.
