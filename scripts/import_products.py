import json
import os
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, firestore

secret = os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON")
if not secret:
    raise SystemExit("Missing GitHub Actions secret: FIREBASE_SERVICE_ACCOUNT_JSON")

service_account_info = json.loads(secret)
expected_project_id = "slgan-a364e"
actual_project_id = service_account_info.get("project_id")
if actual_project_id != expected_project_id:
    raise SystemExit(f"Safety check failed: FIREBASE_SERVICE_ACCOUNT_JSON targets {actual_project_id!r}, expected {expected_project_id!r}")
if not firebase_admin._apps:
    firebase_admin.initialize_app(credentials.Certificate(service_account_info), {"projectId": expected_project_id})

db = firestore.client()
products_path = Path(__file__).resolve().parents[1] / "data" / "products_seed.json"
products = json.loads(products_path.read_text(encoding="utf-8"))  # Catalogue includes the Pills category.

expected_category_counts = {
    "Rasayoga": 31,
    "Rasayanas": 9,
    "Churnas": 19,
    "Gritas": 5,
    "Tailas": 16,
    "Pills": 7,
}
actual_category_counts = {}
for product in products:
    category = product.get("product_category")
    actual_category_counts[category] = actual_category_counts.get(category, 0) + 1
if actual_category_counts != expected_category_counts:
    raise SystemExit(
        f"Safety check failed: expected category counts {expected_category_counts}, "
        f"found {actual_category_counts}"
    )
required_fields = {"product_name", "product_quantity", "product_weight", "product_price", "product_units", "product_category", "signature"}
if any(not required_fields.issubset(product) for product in products):
    raise SystemExit("Safety check failed: one or more products are missing required app fields")
if any(product.get("signature") not in (0, 1) for product in products):
    raise SystemExit("Safety check failed: signature must be 1 for signature products or 0 for other products")

collection = db.collection("products")
existing_docs = list(collection.stream())
new_document_ids = {f"product-{i:03d}" for i in range(1, len(products) + 1)}

# Write and verify the new catalogue first; only then remove old product documents.
for start in range(0, len(products), 400):
    batch = db.batch()
    for index, product in enumerate(products[start:start + 400], start=start + 1):
        batch.set(collection.document(f"product-{index:03d}"), product)
    batch.commit()

written_count = sum(1 for doc_id in new_document_ids if collection.document(doc_id).get().exists)
if written_count != len(products):
    raise SystemExit(f"Safety check failed: wrote {written_count} of {len(products)} products; old documents were retained")

stale_docs = [doc for doc in existing_docs if doc.id not in new_document_ids]
for start in range(0, len(stale_docs), 400):
    batch = db.batch()
    for doc in stale_docs[start:start + 400]:
        batch.delete(doc.reference)
    batch.commit()

print(f"Replaced products collection: inserted/updated {written_count} products and removed {len(stale_docs)} old product documents.")
