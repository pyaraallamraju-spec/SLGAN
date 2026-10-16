import json
import os
import re
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

names = []
seen_names = set()
for disease in diseases:
    if not isinstance(disease, dict) or "diseases_name" not in disease:
        raise SystemExit("Safety check failed: every disease requires diseases_name")
    name = str(disease["diseases_name"]).strip()
    if not name:
        raise SystemExit("Safety check failed: diseases_name cannot be blank")
    normalized = name.casefold()
    if normalized in seen_names:
        raise SystemExit(f"Safety check failed: duplicate diseases_name {name!r}")
    seen_names.add(normalized)
    names.append(name)

collection = db.collection("diseases")

# Remove only the old placeholder created by the initial setup.
for doc in collection.stream():
    old_name = str((doc.to_dict() or {}).get("diseases_name", "")).strip()
    if old_name == "Example disease - replace with actual disease name":
        doc.reference.delete()

# Preserve IDs for names already in Firestore; generate the next ID for each new name.
existing_by_name = {}
max_number = 0
for doc in collection.stream():
    data = doc.to_dict() or {}
    name = str(data.get("diseases_name", "")).strip()
    if name:
        existing_by_name[name.casefold()] = (doc.id, data)
    match = re.fullmatch(r"DISEASE(\d+)", str(data.get("disease_id", doc.id)))
    if match:
        max_number = max(max_number, int(match.group(1)))

batch = db.batch()
written = 0
for name in names:
    existing = existing_by_name.get(name.casefold())
    if existing:
        disease_id = existing[0]
    else:
        max_number += 1
        disease_id = f"DISEASE{max_number:03d}"
    batch.set(collection.document(disease_id), {
        "disease_id": disease_id,
        "diseases_name": name,
    }, merge=True)
    written += 1
    if written % 400 == 0:
        batch.commit()
        batch = db.batch()

if written % 400:
    batch.commit()

for name in names:
    matches = list(collection.where("diseases_name", "==", name).limit(1).stream())
    if not matches:
        raise SystemExit(f"Verification failed: disease {name!r} was not written")
    data = matches[0].to_dict() or {}
    if not data.get("disease_id"):
        raise SystemExit(f"Verification failed: disease {name!r} has no disease_id")

print(f"Verified diseases collection: inserted/updated {len(names)} disease records with unique auto-generated IDs.")
