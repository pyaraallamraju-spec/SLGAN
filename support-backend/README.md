# SLGAN Support Backend

Secure Node.js backend for the SLGAN Android Help & Support hybrid chatbot.

## Security

- Android sends a Firebase Authentication ID token; the server verifies it with Firebase Admin.
- Keep `OPENAI_API_KEY` and `FIREBASE_SERVICE_ACCOUNT_JSON` only in the hosting provider's secret environment variables. Never put either secret in the APK or Git.
- Requests are limited to 12 per authenticated user per minute and message bodies are capped.
- The model is instructed not to diagnose, prescribe dosage, or invent customer/order/product data.

## Deploy to Render

1. Create a Render Web Service from this repository and set the root directory to `support-backend`, or use the repository's `support-backend/render.yaml` blueprint.
2. Add `OPENAI_API_KEY` as a secret environment variable.
3. Create a Firebase service-account key for project `slgan-a364e` and add its complete JSON as the secret `FIREBASE_SERVICE_ACCOUNT_JSON`. Restrict access to the service account and never commit the JSON.
4. Deploy and verify `https://YOUR-SERVICE.onrender.com/health` returns `{"ok":true,"service":"slgan-support"}`.

The AI provider API has separate usage billing; Firebase Spark does not pay for AI requests or the hosting service. Choose usage limits and monitor costs before enabling real AI for customers.

## Connect Android

Pass the public HTTPS backend base URL as a Gradle project property when building the APK:

```bash
gradle assembleDebug -PchatbotApiBaseUrl=https://YOUR-SERVICE.onrender.com
```

The Android client falls back to local bilingual FAQ replies if this property is blank or the service is unavailable. Once the backend is deployed, rebuild the APK with the URL. Do not put API keys in Gradle properties or the Android app.
