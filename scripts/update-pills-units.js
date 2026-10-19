/**
 * One-time Firestore migration for SLGAN products.
 *
 * Dry run (default):
 *   node scripts/update-pills-units.js
 *
 * Apply changes:
 *   node scripts/update-pills-units.js --apply
 *
 * Requires firebase-admin and Application Default Credentials with Firestore
 * access. Never commit service-account credentials to GitHub.
 */
const { initializeApp, applicationDefault, getApps } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

const APPLY = process.argv.includes("--apply");
const PROJECT_ID = process.env.GCLOUD_PROJECT || "slgan-a364e";

if (getApps().length === 0) {
  initializeApp({
    credential: applicationDefault(),
    projectId: PROJECT_ID,
  });
}

async function main() {
  const db = getFirestore();
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
    const chunk = matches.slice(i, i + 400);
    for (const doc of chunk) {
      batch.update(doc.ref, { product_units: "pills" });
    }
    await batch.commit();
    updated += chunk.length;
  }
  console.log(`Updated product_units to "pills" on ${updated} documents. No other fields were changed.`);
}

main().catch((error) => {
  console.error("Migration failed:", error);
  process.exitCode = 1;
});
