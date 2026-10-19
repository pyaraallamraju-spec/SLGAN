# SLGAN Support — Free FAQ Mode

The SLGAN Android app uses its built-in Telugu/English FAQ bot. This mode is designed to avoid AI API charges.

## No-cost setup

- **Do not deploy the optional support backend.** The app already has a local FAQ fallback.
- Leave the Gradle property `chatbotApiBaseUrl` blank (the default). The app will answer using its built-in FAQ without calling any AI provider or requiring a server.
- Do not create or configure an `OPENAI_API_KEY`. The backend code is locked to free-only mode and does not make paid AI requests.
- Firebase Spark remains unchanged.

## FAQ behavior

The local FAQ answers common questions about order status, delivery, invoices, product information, and greetings in Telugu and English. Questions outside the built-in FAQ may receive a general FAQ response.

## Important cost distinction

This setup avoids OpenAI API usage and avoids needing a separately hosted chatbot backend. It cannot promise that unrelated services or your existing Firebase usage will never incur charges, but it adds no paid AI or hosting requirement.
