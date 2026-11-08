import base64
import json
import os

import firebase_admin
from firebase_admin import credentials, firestore

EXPECTED_PROJECT_ID = "slgan-a364e"

FIELDS = {
    "description": ("product_description", "description", "product_details", "details"),
    "usage": ("product_usage", "usage", "how_to_use", "directions"),
    "cures": ("product_cures", "cures", "benefits", "indications"),
    "ingredients": ("product_major_ingredients", "major_ingredients", "ingredients", "product_ingredients"),
}
NAME_FIELDS = ("product_name", "name", "productName", "product_title", "title",
               "medicine_name", "productname", "item_name", "itemName",
               "medicineName", "display_name", "displayName")


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
    info = json.loads(secret)
    if info.get("project_id") != EXPECTED_PROJECT_ID:
        raise SystemExit(f"Refusing unexpected Firebase project: {info.get('project_id')!r}")
    if not firebase_admin._apps:
        firebase_admin.initialize_app(credentials.Certificate(info), {"projectId": EXPECTED_PROJECT_ID})
    docs = list(firestore.client().collection("products").stream())
    report = {key: {} for key in FIELDS}
    populated_docs = {key: 0 for key in FIELDS}
    for doc in docs:
        data = doc.to_dict() or {}
        name = first_text(data, NAME_FIELDS) or "(unnamed)"
        for field, aliases in FIELDS.items():
            value = first_text(data, aliases)
            if not value:
                continue
            populated_docs[field] += 1
            item = report[field].setdefault(value, {"names": [], "doc_ids": []})
            if name not in item["names"]:
                item["names"].append(name)
            if len(item["doc_ids"]) < 10:
                item["doc_ids"].append(doc.id)
    print(f"PROJECT={EXPECTED_PROJECT_ID}")
    print(f"DOCUMENTS_SCANNED={len(docs)}")
    print("POPULATED_DOCUMENT_COUNTS=" + json.dumps(populated_docs, ensure_ascii=False, sort_keys=True))
    print("UNIQUE_TEXT_COUNTS=" + json.dumps({k: len(v) for k, v in report.items()}, sort_keys=True))
    payload = json.dumps(report, ensure_ascii=False, sort_keys=True).encode("utf-8")
    encoded = base64.b64encode(payload).decode("ascii")
    print("REPORT_JSON_BASE64=" + encoded)


if __name__ == "__main__":
    main()
