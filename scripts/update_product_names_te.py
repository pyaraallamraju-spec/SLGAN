import json
import os
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, firestore

EXPECTED_PROJECT_ID = "slgan-a364e"
BATCH_SIZE = 400
ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    secret = os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON")
    if not secret:
        raise SystemExit("Missing GitHub Actions secret: FIREBASE_SERVICE_ACCOUNT_JSON")

    service_account_info = json.loads(secret)
    actual_project_id = service_account_info.get("project_id")
    if actual_project_id != EXPECTED_PROJECT_ID:
        raise SystemExit(
            "Safety check failed: service account targets "
            f"{actual_project_id!r}, expected {EXPECTED_PROJECT_ID!r}"
        )

    catalogue = json.loads(
        (ROOT / "data" / "product_names_te.json").read_text(encoding="utf-8")
    )
    translations = catalogue.get("translations", {})
    if not translations:
        raise SystemExit("Safety check failed: Telugu translation map is empty")

    normalized_map = {}
    for english_name, telugu_name in translations.items():
        if not isinstance(english_name, str) or not english_name.strip():
            raise SystemExit("Safety check failed: an English product name is blank")
        if not isinstance(telugu_name, str) or not telugu_name.strip():
            raise SystemExit(f"Safety check failed: Telugu name is blank for {english_name!r}")
        normalized_map[english_name.strip().casefold()] = telugu_name.strip()

    if not firebase_admin._apps:
        firebase_admin.initialize_app(
            credentials.Certificate(service_account_info),
            {"projectId": EXPECTED_PROJECT_ID},
        )

    db = firestore.client()
    name_fields = (
        "product_name", "name", "productName", "product_title", "title",
        "medicine_name", "productname", "item_name", "itemName",
        "medicineName", "display_name", "displayName",
    )
    telugu_fields = ("product_name_te", "name_te", "productNameTe", "telugu_name")

    docs = list(db.collection("products").stream())
    pending = []
    already_present = 0
    unmatched = set()
    unnamed = 0
    ambiguous_skipped = 0

    skipped = catalogue.get("skipped", {})
    skipped_names = {name.strip().casefold() for name in skipped}

    for doc in docs:
        data = doc.to_dict() or {}
        english_name = next(
            (
                str(data.get(field, "")).strip()
                for field in name_fields
                if data.get(field) is not None and str(data.get(field)).strip()
            ),
            "",
        )
        if not english_name:
            unnamed += 1
            continue

        key = english_name.casefold()
        if key in skipped_names:
            ambiguous_skipped += 1
            continue

        telugu_name = normalized_map.get(key)
        if not telugu_name:
            unmatched.add(english_name)
            continue

        existing_telugu = next(
            (
                str(data.get(field, "")).strip()
                for field in telugu_fields
                if data.get(field) is not None and str(data.get(field)).strip()
            ),
            "",
        )
        if existing_telugu:
            already_present += 1
            continue

        # Update exactly one field. No set/replace/delete operations are used.
        pending.append((doc.reference, telugu_name))

    updated = 0
    for start in range(0, len(pending), BATCH_SIZE):
        batch = db.batch()
        chunk = pending[start : start + BATCH_SIZE]
        for reference, telugu_name in chunk:
            batch.update(reference, {"product_name_te": telugu_name})
        batch.commit()
        updated += len(chunk)

    print("SLGAN Telugu product-name update complete.")
    print(f"Project verified: {EXPECTED_PROJECT_ID}")
    print(f"Live product documents scanned: {len(docs)}")
    print(f"Documents updated (product_name_te only): {updated}")
    print(f"Documents already containing a Telugu name (preserved): {already_present}")
    print(f"Ambiguous source names skipped: {ambiguous_skipped}")
    print(f"Documents without an English name: {unnamed}")
    print(f"Unmatched English product names: {len(unmatched)}")
    for name in sorted(unmatched):
        print(f"UNMATCHED: {name}")


if __name__ == "__main__":
    main()
