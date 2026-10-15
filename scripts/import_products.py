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

if len(products) != 31:
    raise SystemExit(f"Safety check failed: expected 31 products, found {len(products)}")
if any(product.get("product_category") != "Rasayoga" for product in products):
    raise SystemExit("Safety check failed: every product must have category Rasayoga")

collection = db.collection("products")

# Explicitly requested replacement: delete all current product documents first.
existing_docs = list(collection.stream())
batch = db.batch()
pending = 0
for doc in existing_docs:
    batch.delete(doc.reference)
    pending += 1
    if pending == 400:
        batch.commit()
        batch = db.batch()
        pending = 0
if pending:
    batch.commit()

# Insert the 31 replacement products with the existing app's product schema.
batch = db.batch()
for product in products:
    batch.set(collection.document(), product)
batch.commit()

print(f"Replaced products collection: deleted {len(existing_docs)} existing documents and inserted {len(products)} Rasayoga products.")
