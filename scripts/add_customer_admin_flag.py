import json
import os

import firebase_admin
from firebase_admin import credentials, firestore

secret = os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON")
if not secret:
    raise SystemExit("Missing GitHub Actions secret: FIREBASE_SERVICE_ACCOUNT_JSON")

service_account_info = json.loads(secret)
expected_project_id = "slgan-a364e"
if service_account_info.get("project_id") != expected_project_id:
    raise SystemExit("Safety check failed: service account must target slgan-a364e")

if not firebase_admin._apps:
    firebase_admin.initialize_app(
        credentials.Certificate(service_account_info),
        {"projectId": expected_project_id},
    )

db = firestore.client()
collection = db.collection("customers")
batch = db.batch()
pending = 0
updated = 0
preserved = 0

for doc in collection.stream():
    data = doc.to_dict() or {}
    if "is_admin" in data:
        preserved += 1
        continue
    batch.set(doc.reference, {"is_admin": False}, merge=True)
    pending += 1
    updated += 1
    if pending == 400:
        batch.commit()
        batch = db.batch()
        pending = 0

if pending:
    batch.commit()

print(f"Customer is_admin migration complete: set false on {updated} records; preserved existing values on {preserved} records.")
