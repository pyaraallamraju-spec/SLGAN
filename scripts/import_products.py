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

expected_categories = (
    ["Rasayoga"] * 31
    + ["Rasayanas"] * 10
    + ["Churnas"] * 19
    + ["Gritas"] * 5
    + ["Tailas"] * 16
)
if len(products) != 81:
    raise SystemExit(f"Safety check failed: expected 81 products, found {len(products)}")
if any(product.get("product_category") != expected_categories[i] for i, product in enumerate(products)):
    raise SystemExit("Safety check failed: categories must be products 1–31 Rasayoga, 32–41 Rasayanas, 42–60 Churnas, 61–65 Gritas, and 66–81 Tailas")

collection = db.collection("products")
existing_docs = list(collection.stream())

# Replace the products collection with the approved 81-item catalogue.
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

batch = db.batch()
for product in products:
    batch.set(collection.document(), product)
batch.commit()

print(f"Replaced products collection: deleted {len(existing_docs)} existing documents and inserted {len(products)} products.")
