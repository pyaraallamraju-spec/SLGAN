import json
import os

import firebase_admin
from firebase_admin import credentials, firestore

EXPECTED_PROJECT_ID = "slgan-a364e"
FIELDS_TO_REMOVE = (
    "product_description_te",
    "product_usage_te",
    "product_cures_te",
    "product_major_ingredients_te",
)


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
    if not firebase_admin._apps:
        firebase_admin.initialize_app(
            credentials.Certificate(service_account_info),
            {"projectId": EXPECTED_PROJECT_ID},
        )

    db = firestore.client()
    docs = list(db.collection("products").stream())
    delete_field = firestore.DELETE_FIELD
    changed_docs = 0
    removed_counts = {field: 0 for field in FIELDS_TO_REMOVE}

    for start in range(0, len(docs), 400):
        batch = db.batch()
        chunk = docs[start : start + 400]
        chunk_changes = 0
        for doc in chunk:
            data = doc.to_dict() or {}
            updates = {
                field: delete_field
                for field in FIELDS_TO_REMOVE
                if field in data
            }
            if updates:
                batch.update(doc.reference, updates)
                chunk_changes += 1
                for field in updates:
                    removed_counts[field] += 1
        if chunk_changes:
            batch.commit()
            changed_docs += chunk_changes

    print(f"Project verified: {EXPECTED_PROJECT_ID}")
    print(f"Product documents scanned: {len(docs)}")
    print(f"Documents cleaned: {changed_docs}")
    for field, count in removed_counts.items():
        print(f"Removed {field}: {count}")
    print("No English fields or product_name_te fields were changed.")


if __name__ == "__main__":
    main()
