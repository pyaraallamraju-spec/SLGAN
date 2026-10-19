# SLGAN Support Backend

Secure Node.js backend for the SLGAN Android Help & Support hybrid chatbot.

## Security and usage limits

- Android sends a Firebase Authentication ID token; the server verifies it with Firebase Admin.
- Keep `OPENAI_API_KEY` and `FIREBASE_SERVICE_ACCOUNT_JSON` only in the hosting provider's secret environment variables. Never put either secret in the APK or Git.
- Requests are limited to 12 per authenticated user per minute.
- The default `DAILY_AI_LIMIT=5` allows up to five AI requests per signed-in user per UTC day. The server tracks usage in Firestore's `support_ai_usage` collection. When the limit is reached, the API returns `429 DAILY_AI_LIMIT_REACHED`; the Android app should fall back to its local Telugu/English FAQ bot.
- Set `DAILY_AI_LIMIT=0` to disable AI requests completely and use FAQ only. This is the safest option for guaranteeing no OpenAI API usage.
- The model is instructed not to diagnose, prescribe dosage, or invent customer/order/product data.

## Deploy to Render

1. Create a Render Web Service from this repository and set the root directory to `support-backend`, or use the repository's `support-backend/render.yaml` blueprint.
2. Add `OPENAI_API_KEY` as a secret environment variable only if you choose to enable AI requests.
3. Create a Firebase service-account key for project `slgan-a364e` and add its complete JSON as the secret `FIREBASE_SERVICE_ACCOUNT_JSON`. Restrict access to the service account and never commit the JSON.
4. Keep `DAILY_AI_LIMIT` at `5` for the default cap, or set it to `0` for FAQ-only mode.
5. Deploy and verify `https://YOUR-SERVICE.onrender.com/health` returns `{"ok":true,"service":"slgan-support"}`.

**Cost note:** OpenAI API usage is billed separately from ChatGPT and Firebase Spark. A daily cap limits usage but does not make API calls free. To guarantee no OpenAI API charges, set `DAILY_AI_LIMIT=0` and do not configure `OPENAI_API_KEY`. Hosting may also have separate costs depending on the provider's plan.

## Connect Android

Pass the public HTTPS backend base URL as a Gradle project property when building the APK:

```bash
gradle assembleDebug -PchatbotApiBaseUrl=https://YOUR-SERVICE.onrender.com
```

The Android client falls back to local bilingual FAQ replies if the URL is blank or the service is unavailable. Once the backend is deployed, rebuild the APK with the URL. Do not put API keys in Gradle properties or the Android app.
