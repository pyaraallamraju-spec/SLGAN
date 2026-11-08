import json
import os
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, firestore

EXPECTED_PROJECT_ID = "slgan-a364e"
ROOT = Path(__file__).resolve().parents[1]
FIELDS = {
    "description": {
        "source_keys": ("product_description", "description", "product_details", "details"),
        "target_key": "product_description_te",
    },
    "cures": {
        "source_keys": ("product_cures", "cures", "benefits", "indications"),
        "target_key": "product_cures_te",
    },
}
NAME_FIELDS = (
    "product_name", "name", "productName", "product_title", "title",
    "medicine_name", "productname", "item_name", "itemName",
    "medicineName", "display_name", "displayName",
)


def first_text(data, keys):
    for key in keys:
        value = data.get(key)
        if value is not None and str(value).strip():
            return str(value).strip()
    return ""


def main():
    secret = os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON")
    if not secret:
        raise SystemExit("Missing FIREBASE_SERVICE_ACCOUNT_JSON")
    service_account_info = json.loads(secret)
    if service_account_info.get("project_id") != EXPECTED_PROJECT_ID:
        raise SystemExit(
            "Refusing unexpected Firebase project: "
            f"{service_account_info.get('project_id')!r}"
        )

    catalogue = json.loads(
        (ROOT / "data" / "product_content_te.json").read_text(encoding="utf-8")
    )
    translations = {
        field: catalogue.get("descriptions" if field == "description" else "cures", {})
        for field in FIELDS
    }
    for field, mapping in translations.items():
        if not mapping:
            raise SystemExit(f"Missing Telugu translation map for {field}")
        for source, translated in mapping.items():
            if not isinstance(source, str) or not source.strip():
                raise SystemExit(f"Blank English source in {field} map")
            if not isinstance(translated, str) or not translated.strip():
                raise SystemExit(f"Blank Telugu translation for {field}: {source!r}")

    if not firebase_admin._apps:
        firebase_admin.initialize_app(
            credentials.Certificate(service_account_info),
            {"projectId": EXPECTED_PROJECT_ID},
        )

    db = firestore.client()
    docs = list(db.collection("products").stream())
    pending = []
    already_present = {field: 0 for field in FIELDS}
    unmatched = {field: set() for field in FIELDS}
    translated_docs = {field: set() for field in FIELDS}

    for doc in docs:
        data = doc.to_dict() or {}
        product_name = first_text(data, NAME_FIELDS) or "(unnamed)"
        updates = {}
        for field, config in FIELDS.items():
            source_text = first_text(data, config["source_keys"])
            if not source_text:
                continue
            target_key = config["target_key"]
            existing_translation = str(data.get(target_key, "") or "").strip()
            if existing_translation:
                already_present[field] += 1
                continue
            translated_text = translations[field].get(source_text)
            if not translated_text:
                unmatched[field].add(source_text)
                continue
            updates[target_key] = translated_text.strip()
            translated_docs[field].add(doc.id)
        if updates:
            # Update only the Telugu companion fields. The English fields and
            # all other product data remain unchanged.
            pending.append((doc.reference, updates, product_name))

    updated_docs = 0
    for start in range(0, len(pending), 400):
        batch = db.batch()
        chunk = pending[start : start + 400]
        for reference, updates, _ in chunk:
            batch.update(reference, updates)
        batch.commit()
        updated_docs += len(chunk)

    print(f"Project verified: {EXPECTED_PROJECT_ID}")
    print(f"Live product documents scanned: {len(docs)}")
    print(f"Documents updated (Telugu content fields only): {updated_docs}")
    for field, config in FIELDS.items():
        print(f"{field} populated English docs translated: {len(translated_docs[field])}")
        print(f"{field} pre-existing Telugu docs preserved: {already_present[field]}")
        print(f"{field} unmatched unique English texts: {len(unmatched[field])}")
        for value in sorted(unmatched[field]):
            print(f"UNMATCHED_{field.upper()}: {value}")


if __name__ == "__main__":
    main()
