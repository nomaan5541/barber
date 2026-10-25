const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_owner_1";
const ALICE_UID = "customer_alice";
const BOB_BARBER_UID = "barber_bob";
const CHARLIE_BARBER_UID = "barber_charlie";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function serverTs() {
  return new Date(Date.now() - 1000);
}

test("1. Unauthenticated user cannot read services or bookings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("services").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("bookings").get());
});

test("2. Customer cannot self-assign ADMIN role or ACTIVE BARBER status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTs();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      email: "alice@example.com",
      phone: "9876543210",
      role: "ADMIN",
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      email: "alice@example.com",
      phone: "9876543210",
      role: "BARBER",
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid customer registration succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      email: "alice@example.com",
      phone: "9876543210",
      role: "CUSTOMER",
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("3. Spoofed admin email with email_verified=false is rejected", async () => {
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "nomaankhannfs2@gmail.com",
      email_verified: false,
    })
    .firestore();
  const ts = serverTs();
  await assertFails(
    spoofDb.collection("services").doc("haircut_1").set({
      name: "Classic Haircut",
      category: "HAIRCUTS",
      description: "Scissor & clipper cut",
      pricePaise: 30000,
      durationMinutes: 30,
      isActive: true,
      isPackage: false,
      instructions: "Keep a chair ready",
      sortOrder: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("4. Verified owner email can create services, barber commission config, and audit logs", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "nomaankhannfs2@gmail.com",
      email_verified: true,
    })
    .firestore();
  const ts = serverTs();
  await assertSucceeds(
    adminDb.collection("services").doc("haircut_1").set({
      name: "Classic Haircut",
      category: "HAIRCUTS",
      description: "Scissor & clipper cut",
      pricePaise: 30000,
      durationMinutes: 30,
      isActive: true,
      isPackage: false,
      instructions: "Keep a chair ready",
      sortOrder: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("platformConfig").doc("global").set({
      bookingFeeType: "BARBER_COMMISSION_PERCENT",
      fixedBookingFeePaise: 0,
      percentageBookingFeeBps: 0,
      barberCommissionBps: 1500,
      barberFlatFeePaise: 0,
      freeCancellationCutoffHours: 2,
      slotHoldMinutes: 10,
      policyText: "Zero platform fee for customers. 15% commission charged from barbers.",
      maintenanceMode: false,
      updatedBy: ADMIN_UID,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("auditLogs").doc("log_1").set({
      actorId: ADMIN_UID,
      action: "CREATE_SERVICE",
      entityType: "Service",
      entityId: "haircut_1",
      reason: "Initial catalog setup",
      createdAt: ts,
    })
  );

  // Even admin cannot delete or update audit logs
  await assertFails(adminDb.collection("auditLogs").doc("log_1").delete());
});

test("5. Zero Customer Fee + Barber Commission + GPS Home Door Address Isolation", async () => {
  const ts = serverTs();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("barberProfiles").doc(BOB_BARBER_UID).set({
      barberId: BOB_BARBER_UID,
      name: "Bob Master Barber",
      bio: "8 yrs exp",
      experienceYears: 8,
      approvalStatus: "APPROVED",
      isOnline: true,
      ratingAvg: 4.9,
      reviewCount: 12,
      serviceIds: ["haircut_1"],
      serviceAreaIds: ["area_blr"],
      workingDays: ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"],
      workStartHour: 9,
      workEndHour: 20,
      breakStartHour: 13,
      breakEndHour: 14,
      travelBufferMinutes: 15,
      skills: ["Zero Skin Fade", "Razor Beard Sculpt", "Scissor Texture"],
      perfectionBadge: "99% Skin Fade Perfection",
      shopName: "Royal Blade Grooming Atelier",
      shopAddress: "100ft Rd, Indiranagar",
      homeVisitAvailable: true,
      latitude: 12.9784,
      longitude: 77.6408,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_1").set({
      customerId: ALICE_UID,
      label: "HOME",
      flatHouse: "Door #402, Palm Grove",
      streetArea: "12th Main Indiranagar",
      landmark: "Near Metro",
      postalCode: "560038",
      serviceAreaId: "area_blr",
      serviceAreaName: "Indiranagar",
      accessInstructions: "Call at gate",
      isDefault: true,
      latitude: 12.9716,
      longitude: 77.6412,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Charlie cannot read Alice's saved door address
  const charlieDb = testEnv.authenticatedContext(CHARLIE_BARBER_UID).firestore();
  await assertFails(charlieDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_1").get());

  // Create booking with ₹0 customer platform fee and 15% Barber Commission atomically in batch
  const batch = aliceDb.batch();
  const bookingRef = aliceDb.collection("bookings").doc("book_101");
  const privRef = bookingRef.collection("privateDetails").doc("main");
  batch.set(bookingRef, {
    bookingNumber: "DC-2026-1001",
    customerId: ALICE_UID,
    customerName: "Alice",
    barberId: BOB_BARBER_UID,
    barberName: "Bob Master Barber",
    status: "AWAITING_BARBER",
    dateKey: "2026-10-26",
    startHour: 10,
    durationMinutes: 45,
    serviceIds: ["haircut_1"],
    serviceNamesSummary: "Classic Haircut (Rs 300)",
    areaName: "Indiranagar",
    serviceSubtotalPaise: 55000,
    approvedAddOnPaise: 0,
    travelChargePaise: 0,
    barberServicePayablePaise: 55000,
    platformBookingFeePaise: 0,
    totalCustomerCostPaise: 55000,
    bookingFeeStatus: "ZERO_CUSTOMER_FEE",
    bookingFeeTxRef: "SLOT-LOCK-1001",
    barberCommissionPaise: 8250,
    barberNetEarningsPaise: 46750,
    barberCommissionStatus: "PENDING_SETTLEMENT",
    directPaymentStatus: "PENDING",
    directPaymentMethod: "NONE",
    directPaymentDeclaredPaise: 0,
    directPaymentRef: "",
    pendingAddOnServiceId: "",
    pendingAddOnName: "",
    pendingAddOnPricePaise: 0,
    pendingAddOnDurationMin: 0,
    pendingAddOnStatus: "NONE",
    customerNotes: "Ring bell once",
    cancellationReason: "",
    policySnapshot: "Zero customer platform fee • Free cancellation up to 2h prior",
    hasReview: false,
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(privRef, {
    bookingId: "book_101",
    customerId: ALICE_UID,
    barberId: BOB_BARBER_UID,
    fullAddressSnapshot: "Door #402, Palm Grove, 12th Main Indiranagar, 560038",
    accessInstructions: "Call at gate",
    customerPhone: "9876543210",
    createdAt: ts,
    updatedAt: ts,
  });
  await assertSucceeds(batch.commit());

  // Assigned barber Bob CAN read privateDetails while active
  const bobDb = testEnv.authenticatedContext(BOB_BARBER_UID).firestore();
  await assertSucceeds(bobDb.collection("bookings").doc("book_101").collection("privateDetails").doc("main").get());

  // Unassigned barber Charlie CANNOT read privateDetails
  await assertFails(charlieDb.collection("bookings").doc("book_101").collection("privateDetails").doc("main").get());

  // Barber Bob CANNOT silently hike barberServicePayablePaise or zero out barberCommissionPaise
  await assertFails(
    bobDb.collection("bookings").doc("book_101").update({
      barberCommissionPaise: 0,
      updatedAt: ts,
    })
  );

  // Barber Bob CAN request an add-on without changing approvedAddOnPaise
  await assertSucceeds(
    bobDb.collection("bookings").doc("book_101").update({
      status: "CONFIRMED",
      pendingAddOnServiceId: "wash_1",
      pendingAddOnName: "Hair Wash",
      pendingAddOnPricePaise: 10000,
      pendingAddOnDurationMin: 15,
      pendingAddOnStatus: "REQUESTED",
      updatedAt: ts,
    })
  );

  // Unfiltered bookings query fails (Query Enforcer)
  await assertFails(aliceDb.collection("bookings").get());
  // Filtered bookings query succeeds
  await assertSucceeds(aliceDb.collection("bookings").where("customerId", "==", ALICE_UID).get());
});
