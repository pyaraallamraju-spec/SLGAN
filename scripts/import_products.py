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

# Let Firestore generate document IDs. On reruns, update a matching product by
# product_name so the workflow does not create duplicates for the same product.
collection = db.collection("products")
created_or_updated = 0

for product in products:
    matches = collection.where("product_name", "==", product["product_name"]).limit(1).stream()
    existing = next(matches, None)
    if existing:
        existing.reference.set(product, merge=True)
    else:
        collection.add(product)
    created_or_updated += 1

print(f"Imported/updated {created_or_updated} products in Firestore collection 'products'.")
