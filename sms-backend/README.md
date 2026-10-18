# SLGAN SMS Backend

This folder is reserved for the separate backend that will send Exotel order-confirmation and delivery-update SMS.

## Security
- Never commit Exotel API keys, API tokens, or account secrets.
- Store credentials only as environment variables in the backend hosting provider.
- The Android app must call this backend; it must never call Exotel with embedded credentials.
- Before production use, authenticate requests, authorize order access, validate phone numbers and order statuses, and prevent duplicate notifications.

## Status
Workspace created. Backend implementation and deployment configuration are not yet added. Do not deploy this folder until those files are ready.

## Hosting constraint
Firebase remains on the Spark plan. This service is intended to run separately from Firebase Cloud Functions.