import json
import os

import firebase_admin
from firebase_admin import credentials, firestore

secret = os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON")
if not secret:
    raise SystemExit("Missing GitHub Actions secret: FIREBASE_SERVICE_ACCOUNT_JSON")

service_account_info = json.loads(secret)
expected_project_id = "slgan-a364e"
actual_project_id = service_account_info.get("project_id")
if actual_project_id != expected_project_id:
    raise SystemExit(
        f"Safety check failed: credentials target {actual_project_id!r}, "
        f"expected {expected_project_id!r}"
    )

if not firebase_admin._apps:
    firebase_admin.initialize_app(
        credentials.Certificate(service_account_info),
        {"projectId": expected_project_id},
    )

db = firestore.client()
collection = db.collection("products")
docs = list(collection.stream())

if not docs:
    raise SystemExit("Safety check failed: products collection is empty; nothing was changed.")

missing = [doc for doc in docs if "product_stock" not in (doc.to_dict() or {})]
print(f"Found {len(docs)} product documents; {len(missing)} are missing product_stock.")

# Only add the field when absent. Preserve all existing product fields and any
# stock values already set to 0 or 1. Firestore batches are limited to 500 writes.
updated = 0
for start in range(0, len(missing), 400):
    batch = db.batch()
    chunk = missing[start:start + 400]
    for doc in chunk:
        batch.update(doc.reference, {"product_stock": 1})
    batch.commit()
    updated += len(chunk)

# Verify every product now has a valid stock value.
invalid = []
for doc in collection.stream():
    value = (doc.to_dict() or {}).get("product_stock")
    if value not in (0, 1):
        invalid.append((doc.id, value))

if invalid:
    examples = ", ".join(f"{doc_id}={value!r}" for doc_id, value in invalid[:10])
    raise SystemExit(f"Verification failed: invalid product_stock values: {examples}")

print(f"Migration complete: set product_stock=1 on {updated} documents; all products verified.")
