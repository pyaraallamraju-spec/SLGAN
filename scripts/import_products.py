import json
import os
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, firestore

secret = os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON")
if not secret:
    raise SystemExit("Missing GitHub Actions secret: FIREBASE_SERVICE_ACCOUNT_JSON")

service_account_info = json.loads(secret)
if not firebase_admin._apps:
    firebase_admin.initialize_app(credentials.Certificate(service_account_info))

db = firestore.client()
products_path = Path(__file__).resolve().parents[1] / "data" / "products_seed.json"
products = json.loads(products_path.read_text(encoding="utf-8"))

# Stable document IDs make reruns update the same products rather than duplicate them.
batch = db.batch()
for product in products:
    doc_id = str(product["id"])
    batch.set(db.collection("products").document(doc_id), product, merge=True)
batch.commit()
print(f"Imported/updated {len(products)} products in Firestore collection 'products'.")
