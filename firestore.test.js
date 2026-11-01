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

test("2. Onboarding demographics & role protection prevent privilege escalation", async () => {
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

  // Initial Google sign-in bootstrap before onboarding completes
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      email: "alice@example.com",
      phone: "",
      role: "CUSTOMER",
      status: "ACTIVE",
      dob: "",
      age: 0,
      gender: "",
      onboardingCompleted: false,
      privacyAccepted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Completing onboarding with DOB, age, gender, phone, and privacy consent succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      name: "Alice Sharma",
      phone: "+919876501234",
      dob: "1998-06-15",
      age: 28,
      gender: "FEMALE",
      onboardingCompleted: true,
      privacyAccepted: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Once onboardingCompleted == true, user cannot arbitrarily switch role
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "BARBER",
      status: "PENDING",
      updatedAt: new Date(Date.now() - 200),
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

test("4. Verified owner email can create men's & ladies' services, commission config, and audit logs", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "nomaankhannfs2@gmail.com",
      email_verified: true,
    })
    .firestore();
  const ts = serverTs();
  await assertSucceeds(
    adminDb.collection("services").doc("ladies_cut_1").set({
      name: "Ladies Layered Haircut & Blowout",
      category: "LADIES_STYLING",
      description: "Precision layered cut and smooth blowout by a verified stylist",
      pricePaise: 49900,
      durationMinutes: 45,
      isActive: true,
      isPackage: false,
      instructions: "Keep a mirror and chair ready",
      sortOrder: 2,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("platformConfig").doc("global").set({
      bookingFeeType: "BARBER_COMMISSION_PERCENT",
      fixedBookingFeePaise: 2900,
      percentageBookingFeeBps: 0,
      barberCommissionBps: 1500,
      barberFlatFeePaise: 0,
      freeCancellationCutoffHours: 2,
      slotHoldMinutes: 10,
      policyText: "Free cancellation up to 2h prior. Pay service charge directly to appointed barber.",
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
      entityId: "ladies_cut_1",
      reason: "Ladies catalog setup",
      createdAt: ts,
    })
  );

  await assertFails(adminDb.collection("auditLogs").doc("log_1").delete());
});

test("5. Open-Dispatch Pool: Customer books UNASSIGNED -> Barber claims -> Unlocks door address & streams live GPS", async () => {
  const ts = serverTs();
  const bobDb = testEnv.authenticatedContext(BOB_BARBER_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_BARBER_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Barber Bob creates his real verified profile with Lady Stylist specialization
  await assertSucceeds(
    bobDb.collection("barberProfiles").doc(BOB_BARBER_UID).set({
      barberId: BOB_BARBER_UID,
      name: "Priya Verma",
      phone: "+919840011223",
      gender: "FEMALE",
      servesLadies: true,
      servesMen: true,
      bio: "7 yrs certified hair stylist",
      experienceYears: 7,
      approvalStatus: "APPROVED",
      isOnline: true,
      ratingAvg: 5.0,
      reviewCount: 0,
      serviceIds: ["ladies_cut_1"],
      serviceAreaIds: ["area_hyd"],
      workingDays: ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"],
      workStartHour: 9,
      workEndHour: 20,
      breakStartHour: 13,
      breakEndHour: 14,
      travelBufferMinutes: 15,
      skills: ["Ladies Layer Cut", "Blowout Styling"],
      perfectionBadge: "Certified Lady Hair Stylist",
      shopName: "Priya Home Studio",
      shopAddress: "Jubilee Hills",
      homeVisitAvailable: true,
      latitude: 17.4312,
      longitude: 78.4071,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice creates an UNASSIGNED open-dispatch booking requesting a Lady Hair Barber
  const batch = aliceDb.batch();
  const bookingRef = aliceDb.collection("bookings").doc("book_open_201");
  const privRef = bookingRef.collection("privateDetails").doc("main");
  batch.set(bookingRef, {
    bookingNumber: "DC-2026-2001",
    customerId: ALICE_UID,
    customerName: "Alice Sharma",
    customerPhone: "+919876501234",
    customerGender: "FEMALE",
    customerAge: 28,
    preferLadyBarber: true,
    bookingMode: "ASAP",
    customerLat: 17.4239,
    customerLon: 78.4138,
    barberId: "UNASSIGNED",
    barberName: "Assigning Verified Stylist",
    barberPhone: "",
    barberLat: 0.0,
    barberLon: 0.0,
    status: "AWAITING_BARBER",
    dateKey: "2026-10-26",
    startHour: 11,
    durationMinutes: 45,
    serviceIds: ["ladies_cut_1"],
    serviceNamesSummary: "Ladies Layered Haircut (Rs 499)",
    areaName: "Jubilee Hills",
    serviceSubtotalPaise: 49900,
    approvedAddOnPaise: 0,
    travelChargePaise: 0,
    barberServicePayablePaise: 49900,
    platformBookingFeePaise: 2900,
    totalCustomerCostPaise: 52800,
    bookingFeeStatus: "UNPAID",
    bookingFeeTxRef: "DISPATCH-2001",
    barberCommissionPaise: 7485,
    barberNetEarningsPaise: 42415,
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
    customerNotes: "Need Lady Hair Stylist please",
    cancellationReason: "",
    policySnapshot: "Free cancellation before dispatch",
    hasReview: false,
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(privRef, {
    bookingId: "book_open_201",
    customerId: ALICE_UID,
    barberId: "UNASSIGNED",
    fullAddressSnapshot: "Flat 304, Lotus Enclave, Jubilee Hills, 500033",
    accessInstructions: "Call when at main gate",
    customerPhone: "+919876501234",
    createdAt: ts,
    updatedAt: ts,
  });
  await assertSucceeds(batch.commit());

  // While UNASSIGNED, barbers CAN list the open dispatch queue, but CANNOT read privateDetails yet!
  await assertSucceeds(
    bobDb
      .collection("bookings")
      .where("barberId", "==", "UNASSIGNED")
      .where("status", "==", "AWAITING_BARBER")
      .get()
  );
  await assertFails(bobDb.collection("bookings").doc("book_open_201").collection("privateDetails").doc("main").get());

  // Stylist Priya (BOB_BARBER_UID) claims the open booking AND creates atomic slotLocks!
  await assertSucceeds(
    bobDb.collection("bookings").doc("book_open_201").update({
      barberId: BOB_BARBER_UID,
      barberName: "Priya Verma",
      barberPhone: "+919840011223",
      barberLat: 17.4312,
      barberLon: 78.4071,
      status: "CONFIRMED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    bobDb.collection("slotLocks").doc(`${BOB_BARBER_UID}_2026-10-26_11`).set({
      barberId: BOB_BARBER_UID,
      dateKey: "2026-10-26",
      hourSlot: 11,
      bookingId: "book_open_201",
      customerId: ALICE_UID,
      status: "CONFIRMED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Now claimed stylist CAN read privateDetails, while Charlie CANNOT read privateDetails OR edit the booking!
  await assertSucceeds(bobDb.collection("bookings").doc("book_open_201").collection("privateDetails").doc("main").get());
  await assertFails(charlieDb.collection("bookings").doc("book_open_201").collection("privateDetails").doc("main").get());
  await assertFails(
    charlieDb.collection("bookings").doc("book_open_201").update({
      status: "ON_THE_WAY",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Claimed stylist CAN stream ON_THE_WAY live GPS movement!
  await assertSucceeds(
    bobDb.collection("bookings").doc("book_open_201").update({
      status: "ON_THE_WAY",
      barberLat: 17.4275,
      barberLon: 78.4105,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Post-service direct payment declaration MUST record barber UID + timestamp
  await assertFails(
    bobDb.collection("bookings").doc("book_open_201").update({
      directPaymentStatus: "UPI_DECLARED",
      directPaymentMethod: "UPI",
      directPaymentDeclaredPaise: 49900,
      directPaymentRef: "UTR998811",
      updatedAt: new Date(Date.now() - 150),
    })
  );
  await assertSucceeds(
    bobDb.collection("bookings").doc("book_open_201").update({
      directPaymentStatus: "UPI_DECLARED",
      directPaymentMethod: "UPI",
      directPaymentDeclaredPaise: 49900,
      directPaymentRef: "UTR998811",
      directPaymentRecordedBy: BOB_BARBER_UID,
      directPaymentRecordedAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Unfiltered bookings query still fails (Query Enforcer)
  await assertFails(aliceDb.collection("bookings").get());
});

test("6. Zero-trust payment verification blocks client-spoofed PAID_VERIFIED without server/Admin receipt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "nomaankhannfs2@gmail.com",
    email_verified: true,
  }).firestore();
  const ts = serverTs();

  const basePayload = {
    bookingNumber: "DC-2026-3001",
    customerId: ALICE_UID,
    customerName: "Alice Sharma",
    customerPhone: "+919876501234",
    customerGender: "FEMALE",
    customerAge: 28,
    preferLadyBarber: false,
    bookingMode: "ASAP",
    customerLat: 17.4239,
    customerLon: 78.4138,
    barberId: "UNASSIGNED",
    barberName: "Assigning Verified Stylist",
    barberPhone: "",
    barberLat: 0.0,
    barberLon: 0.0,
    status: "AWAITING_BARBER",
    dateKey: "2026-10-27",
    startHour: 15,
    durationMinutes: 30,
    serviceIds: ["svc_classic_cut"],
    serviceNamesSummary: "Classic Haircut (Rs 299)",
    areaName: "Jubilee Hills",
    serviceSubtotalPaise: 29900,
    approvedAddOnPaise: 0,
    travelChargePaise: 0,
    barberServicePayablePaise: 29900,
    platformBookingFeePaise: 2900,
    totalCustomerCostPaise: 32800,
    bookingFeeStatus: "PAID_VERIFIED",
    bookingFeeTxRef: "FAKE-CLIENT-TX",
    barberCommissionPaise: 4485,
    barberNetEarningsPaise: 25415,
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
    customerNotes: "",
    cancellationReason: "",
    policySnapshot: "Free cancellation before dispatch",
    hasReview: false,
    createdAt: ts,
    updatedAt: ts,
  };

  // Client CANNOT spoof PAID_VERIFIED when /verifiedPayments/book_spoof_301 does not exist!
  await assertFails(aliceDb.collection("bookings").doc("book_spoof_301").set(basePayload));

  // Client CANNOT forge a /verifiedPayments receipt either!
  await assertFails(
    aliceDb.collection("verifiedPayments").doc("book_spoof_301").set({
      bookingId: "book_spoof_301",
      customerId: ALICE_UID,
      amountPaise: 2900,
      provider: "RAZORPAY_WEBHOOK",
      orderId: "order_fake",
      paymentId: "pay_fake",
      verifiedBy: ALICE_UID,
      verifiedAt: ts,
    })
  );

  // Server / Admin writes /verifiedPayments/book_spoof_301 after Razorpay HMAC verification
  await assertSucceeds(
    adminDb.collection("verifiedPayments").doc("book_spoof_301").set({
      bookingId: "book_spoof_301",
      customerId: ALICE_UID,
      amountPaise: 2900,
      provider: "RAZORPAY_WEBHOOK",
      orderId: "order_real_123",
      paymentId: "pay_real_456",
      verifiedBy: ADMIN_UID,
      verifiedAt: ts,
    })
  );

  // Now booking with PAID_VERIFIED succeeds!
  await assertSucceeds(aliceDb.collection("bookings").doc("book_spoof_301").set(basePayload));
});

