/**
 * One-time Firestore migration for SLGAN products.
 *
 * Dry run (default):
 *   node scripts/update-pills-units.js
 *
 * Apply changes:
 *   node scripts/update-pills-units.js --apply
 *
 * Requires firebase-admin and GOOGLE_APPLICATION_CREDENTIALS pointing to a
 * trusted Firebase service-account JSON file with Firestore write permission.
 * Never commit the service-account JSON to GitHub.
 */
const admin = require("firebase-admin");

const APPLY = process.argv.includes("--apply");
const PROJECT_ID = process.env.GCLOUD_PROJECT || "slgan-a364e";

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId: PROJECT_ID,
  });
}

async function main() {
  const db = admin.firestore();
  const snapshot = await db.collection("products").get();
  const matches = snapshot.docs.filter((doc) => {
    const units = doc.get("product_units");
    return typeof units === "string" && units.trim() === "Pills";
  });

  console.log(`Project: ${PROJECT_ID}`);
  console.log(`Products scanned: ${snapshot.size}`);
  console.log(`Exact "Pills" matches: ${matches.length}`);
  for (const doc of matches) {
    console.log(`- ${doc.id}: ${doc.get("product_name") || doc.get("name") || "(name unavailable)"}`);
  }

  if (!APPLY) {
    console.log("\nDRY RUN ONLY — no documents changed. Re-run with --apply to update only product_units.");
    return;
  }

  let updated = 0;
  for (let i = 0; i < matches.length; i += 400) {
    const batch = db.batch();
    for (const doc of matches.slice(i, i + 400)) {
      batch.update(doc.ref, { product_units: "pills" });
    }
    await batch.commit();
    updated += matches.slice(i, i + 400).length;
  }
  console.log(`Updated product_units to "pills" on ${updated} documents. No other fields were changed.`);
}

main().catch((error) => {
  console.error("Migration failed:", error);
  process.exitCode = 1;
});
