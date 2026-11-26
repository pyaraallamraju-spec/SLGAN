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


function validOrder(orderId, customerId) {
  return {
    orderId,
    customerId,
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
    invoiceNumber: orderId,
    invoiceStatus: "not_generated",
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
}

test("customers can create a valid pending COD order", async () => {
  const db = testEnv.authenticatedContext("customer-user").firestore();
  await assertSucceeds(setDoc(
    doc(db, "orders", "order-valid"),
    validOrder("order-valid", "customer-user")
  ));
});

test("customers cannot inject unsupported backend fields into orders", async () => {
  const db = testEnv.authenticatedContext("customer-user").firestore();
  await assertFails(setDoc(doc(db, "orders", "order-injected"), {
    ...validOrder("order-injected", "customer-user"),
    invoicePdfUrl: "https://attacker.example/fake-invoice.pdf"
  }));
});

test("customers can read their own orders but not another customer's orders", async () => {
  await testEnv.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), "orders", "customer-order"), validOrder("customer-order", "customer-user"));
    await setDoc(doc(context.firestore(), "orders", "other-order"), validOrder("other-order", "other-user"));
  });
  const customerDb = testEnv.authenticatedContext("customer-user").firestore();
  await assertSucceeds(getDoc(doc(customerDb, "orders", "customer-order")));
  await assertFails(getDoc(doc(customerDb, "orders", "other-order")));
});

test("only admins can change order status", async () => {
  await seedAdmin();
  await testEnv.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), "orders", "status-order"), validOrder("status-order", "customer-user"));
  });
  const customerDb = testEnv.authenticatedContext("customer-user").firestore();
  await assertFails(setDoc(doc(customerDb, "orders", "status-order"), {
    orderStatus: "accepted"
  }, { merge: true }));

  const adminDb = testEnv.authenticatedContext("admin-user").firestore();
  await assertSucceeds(setDoc(doc(adminDb, "orders", "status-order"), {
    orderStatus: "accepted",
    updatedAt: new Date().toISOString()
  }, { merge: true }));
});
