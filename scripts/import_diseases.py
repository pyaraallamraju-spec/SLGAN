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

# Normalize legacy IDs (DISEASE003 / disease003) to integer disease_id fields.
existing_docs = list(collection.stream())
def parse_numeric_id(doc, data):
    raw = data.get("disease_id")
    if isinstance(raw, int) and not isinstance(raw, bool) and raw > 0:
        return raw
    text = str(raw if raw is not None else doc.id).strip()
    match = re.fullmatch(r"(?:DISEASE|disease)(\d+)", text, re.IGNORECASE)
    if match:
        return int(match.group(1))
    if re.fullmatch(r"\d+", text):
        return int(text)
    match = re.fullmatch(r"(?:DISEASE|disease)(\d+)", doc.id, re.IGNORECASE)
    return int(match.group(1)) if match else None

parsed_ids = {doc.id: parse_numeric_id(doc, doc.to_dict() or {}) for doc in existing_docs}
counts = {}
for value in parsed_ids.values():
    if value is not None:
        counts[value] = counts.get(value, 0) + 1

# Preserve unique existing numeric IDs; safely renumber only missing/duplicate IDs.
assigned_ids = {}
for doc in existing_docs:
    value = parsed_ids[doc.id]
    if value is not None and counts[value] == 1:
        assigned_ids[doc.id] = value
max_number = max(assigned_ids.values(), default=0)
for doc in existing_docs:
    if doc.id not in assigned_ids:
        max_number += 1
        assigned_ids[doc.id] = max_number

batch = db.batch()
pending = 0
existing_by_name = {}
for doc in existing_docs:
    data = doc.to_dict() or {}
    numeric_id = assigned_ids[doc.id]
    batch.update(doc.reference, {"disease_id": numeric_id})
    pending += 1
    name = str(data.get("diseases_name", "")).strip()
    if name:
        existing_by_name[name.casefold()] = (doc.id, numeric_id, data)
    if pending >= 400:
        batch.commit()
        batch = db.batch()
        pending = 0
if pending:
    batch.commit()

# Preserve each existing document and numeric ID; new diseases receive max + 1.
batch = db.batch()
written = 0
for seed_record in diseases:
    name = str(seed_record["diseases_name"]).strip()
    existing = existing_by_name.get(name.casefold())
    if existing:
        document_id, disease_id, _ = existing
        ref = collection.document(document_id)
    else:
        max_number += 1
        disease_id = max_number
        ref = collection.document()
    fields = {
        "disease_id": int(disease_id),
        "diseases_name": name,
    }
    description = str(seed_record.get("disease_desc", "")).strip()
    if description:
        fields["disease_desc"] = description
    batch.set(ref, fields, merge=True)
    written += 1
    if written % 400 == 0:
        batch.commit()
        batch = db.batch()

if written % 400:
    batch.commit()

# Keep the shared counter aligned with the highest assigned integer ID.
db.collection("system_counters").document("disease_id").set({
    "last_id": int(max_number),
    "updatedAt": firestore.SERVER_TIMESTAMP,
}, merge=True)

for name in names:
    matches = list(collection.where("diseases_name", "==", name).limit(1).stream())
    if not matches:
        raise SystemExit(f"Verification failed: disease {name!r} was not written")
    data = matches[0].to_dict() or {}
    if not isinstance(data.get("disease_id"), int):
        raise SystemExit(f"Verification failed: disease {name!r} disease_id is not an integer")

print(f"Verified diseases collection: inserted/updated {len(names)} disease records with integer auto-increment IDs.")
