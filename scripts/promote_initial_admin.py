import json
import os

import firebase_admin
from firebase_admin import auth, credentials, firestore

ADMIN_EMAIL = "amruth.allamraju@gmail.com"
EXPECTED_PROJECT_ID = "slgan-a364e"

secret = os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON")
if not secret:
    raise SystemExit("Missing GitHub Actions secret: FIREBASE_SERVICE_ACCOUNT_JSON")

service_account_info = json.loads(secret)
if service_account_info.get("project_id") != EXPECTED_PROJECT_ID:
    raise SystemExit("Safety check failed: service account must target slgan-a364e")

if not firebase_admin._apps:
    firebase_admin.initialize_app(
        credentials.Certificate(service_account_info),
        {"projectId": EXPECTED_PROJECT_ID},
    )

try:
    user = auth.get_user_by_email(ADMIN_EMAIL)
except auth.UserNotFoundError:
    print(
        f"Firebase Authentication account for {ADMIN_EMAIL} does not exist yet. "
        "Create it in the app first, then rerun this workflow."
    )
    raise SystemExit(0)

db = firestore.client()
customer_ref = db.collection("customers").document(user.uid)
customer_ref.set(
    {
        "email": ADMIN_EMAIL,
        "is_admin": True,
        "updatedAt": firestore.SERVER_TIMESTAMP,
    },
    merge=True,
)
print(f"Admin role enabled for {ADMIN_EMAIL} (UID: {user.uid}).")
