# Sri Lakshmi Ganapathi Ayurveda Nilayam (SLGAN)

Android storefront and administration app for Sri Lakshmi Ganapathi Ayurveda Nilayam, built with Kotlin, Jetpack Compose and Material 3.

## Android application

- Application ID / namespace: `com.pyara.slgan`
- Minimum Android SDK: 24
- Compile SDK: 35
- Java / Kotlin JVM target: 17
- UI: Jetpack Compose + Material 3
- Main source: `app/src/main/java/com/pyara/slgan/`

## Implemented app areas

The current Android source includes the following customer and administration screens:

- Sign-in and account flows, including Firebase authentication and email/phone OTP integration points
- Home screen, promotional banner slider and product catalogue
- Product categories, product details, product images, pack/weight display, ratings and wishlist controls
- Cart, saved addresses, address selection and order history
- Disease information and searchable disease listings
- Customer profile, help, contact, privacy and about pages
- Admin dashboard and management screens for products, orders, customers, diseases, payments, banners and themes
- Google Maps-based address selection
- Cashfree payment activity and order-email integration

Availability of individual online services depends on their external configuration, credentials, permissions and provider-side status. A screen or integration in the source does not by itself mean that the corresponding production service is enabled.

## Backend and service configuration

The Android app is integrated with Firebase services, including Authentication and Cloud Firestore. The repository also contains Firebase Hosting content, Firestore security rules, backend/functions and product-data utilities.

- Firebase project: `slgan-a364e`
- Firebase Android configuration: `app/google-services.json`
- Firestore rules: `firestore.rules`
- Hosting configuration: `firebase.json`
- Google Maps key: supplied to Gradle through the `MAPS_API_KEY` project property or `GOOGLE_MAPS_API_KEY` environment variable.
- Email OTP endpoint: configurable with the `emailOtpScriptUrl` Gradle property.
- Optional chatbot endpoint: configurable with the `chatbotApiBaseUrl` Gradle property.

Do not commit service-account files, private keys, payment credentials, signing keys or other secrets. Configure deployment credentials as protected GitHub Actions secrets. Keep Cashfree sandbox and live credentials separate, and do not enable live payments without explicit operational approval.

**Billing constraint:** Keep the Firebase project on the Spark/free plan. Do not upgrade billing or enable paid cloud services as part of routine development. Check current Firebase product-plan requirements before enabling or relying on a service.

## Build locally

1. Open the repository in Android Studio.
2. Use JDK 17 and allow Gradle sync to complete.
3. Configure the required local properties or environment variables for optional integrations.
4. Build the debug APK:

```bash
gradle assembleDebug
```

The output is:

```
app/build/outputs/apk/debug/app-debug.apk
```

A Google Maps key is optional for compile verification; map functionality requires a valid key and the appropriate Google Cloud configuration.

## GitHub Actions

- `.github/workflows/android.yml` builds and verifies the debug APK on relevant Android-source changes and supports manual dispatch.
- `.github/workflows/firebase-hosting.yml` deploys Hosting and Firestore rules when its configured paths change or when manually dispatched. It requires the `FIREBASE_SERVICE_ACCOUNT_JSON` GitHub Actions secret.

Check the individual workflow run before treating a build or deployment as successful. Hosting deployment and Android APK build are separate workflows.

## Data and customer safety

This repository contains application code and configuration, not a guarantee that production data or services are ready. Before a production release, review Firestore security rules, authentication and authorization, admin access, privacy disclosures, payment configuration, order handling, backups and any applicable regulatory requirements. Product and disease information should be reviewed by qualified personnel; app content is not a substitute for medical advice.
