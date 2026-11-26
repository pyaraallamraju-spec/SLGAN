const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const path = require("node:path");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require("@firebase/rules-unit-testing");
const { doc, getDoc, setDoc } = require("firebase/firestore");

const projectId = "demo-slgan-rules";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    firestore: {
      rules: fs.readFileSync(path.join(__dirname, "..", "firestore.rules"), "utf8")
    }
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

async function seedAdmin(uid = "admin-user") {
  await testEnv.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), "customers", uid), {
      is_admin: true,
      is_active: true
    });
  });
}

test("categories are readable without signing in", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(db, "categories", "lehyas")));
});

test("signed-in customers can read categories but cannot create them", async () => {
  const db = testEnv.authenticatedContext("customer-user").firestore();
  await assertSucceeds(getDoc(doc(db, "categories", "lehyas")));
  await assertFails(setDoc(doc(db, "categories", "new-category"), {
    category_name: "New Category",
    name: "New Category",
    emoji: "🌿"
  }));
});

test("trusted admins can create and update categories", async () => {
  await seedAdmin();
  const db = testEnv.authenticatedContext("admin-user").firestore();
  const category = doc(db, "categories", "test-category");
  await assertSucceeds(setDoc(category, {
    category_name: "Test Category",
    name: "Test Category",
    emoji: "🌿"
  }));
  await assertSucceeds(setDoc(category, { name: "Updated Category" }, { merge: true }));
});

test("customers cannot create orders with admin or backend-only fields", async () => {
  const db = testEnv.authenticatedContext("customer-user").firestore();
  await assertFails(setDoc(doc(db, "orders", "order-1"), {
    orderId: "order-1",
    customerId: "customer-user",
    customerName: "Test Customer",
    email: "test@example.com",
    phone: "9999999999",
    shippingAddress: "Test address",
    items: [{ productId: "p1", productName: "Product", unitPrice: 10, quantity: 1, lineTotal: 10 }],
    subtotal: 10,
    grandTotal: 10,
    paymentStatus: "not_collected",
    paymentMethod: "cod",
    paymentEnvironment: "none",
    orderStatus: "pending_verification",
    invoiceStatus: "not_generated",
    invoiceNumber: "ADMIN-CONTROLLED"
  }));
});
