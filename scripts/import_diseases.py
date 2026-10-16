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
    raise SystemExit(
        f"Safety check failed: service account targets {actual_project_id!r}, "
        f"expected {expected_project_id!r}"
    )

if not firebase_admin._apps:
    firebase_admin.initialize_app(
        credentials.Certificate(service_account_info),
        {"projectId": expected_project_id},
    )

db = firestore.client()
seed_path = Path(__file__).resolve().parents[1] / "data" / "diseases_seed.json"
diseases = json.loads(seed_path.read_text(encoding="utf-8"))

if not isinstance(diseases, list):
    raise SystemExit("Safety check failed: diseases_seed.json must contain a JSON array")

required_fields = {"disease_id", "diseases_name"}
seen_ids = set()
for disease in diseases:
    if not isinstance(disease, dict) or not required_fields.issubset(disease):
        raise SystemExit("Safety check failed: every disease requires disease_id and diseases_name")
    disease_id = str(disease["disease_id"]).strip()
    disease_name = str(disease["diseases_name"]).strip()
    if not disease_id or not disease_name:
        raise SystemExit("Safety check failed: disease_id and diseases_name cannot be blank")
    if disease_id in seen_ids:
        raise SystemExit(f"Safety check failed: duplicate disease_id {disease_id!r}")
    seen_ids.add(disease_id)

collection = db.collection("diseases")
for start in range(0, len(diseases), 400):
    batch = db.batch()
    for disease in diseases[start:start + 400]:
        disease_id = str(disease["disease_id"]).strip()
        batch.set(collection.document(disease_id), {
            "disease_id": disease_id,
            "diseases_name": str(disease["diseases_name"]).strip(),
        }, merge=True)
    batch.commit()

for disease in diseases:
    doc = collection.document(str(disease["disease_id"]).strip()).get()
    if not doc.exists:
        raise SystemExit(f"Verification failed: disease {disease['disease_id']!r} was not written")

print(f"Verified diseases collection: inserted/updated {len(diseases)} disease records.")
