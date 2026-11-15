const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "cmd_admin_001";

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

async function seedAdmin() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(ADMIN_UID).set({
      role: "command_center_operator",
      active: true,
    });
  });
}

// --- COMMAND CENTER TIERS & SERVICES CATALOG ---

test("Catalog tiers and services: signed-in users query active records, cannot read inactive or write", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("command_center_tiers").doc("PERSONAL").set({
      code: "PERSONAL",
      title: "Personal & VIP",
      subtitle: "Close protection",
      startingRate: "From $45/hr",
      highlights: "Vetted protection",
      isRecommended: true,
      isActive: true,
      sortOrder: 1,
    });
    await db.collection("command_center_tiers").doc("ARCHIVED").set({
      code: "ARCHIVED",
      title: "Archived Tier",
      isActive: false,
      sortOrder: 9,
    });
    await db.collection("command_center_services").doc("ESCORT_GPS").set({
      code: "ESCORT_GPS",
      title: "Executive Escort",
      subtitle: "Point-to-point protection",
      description: "Live tracked officer escort",
      tierCode: "PERSONAL",
      tierTitle: "Personal & VIP",
      hourlyRate: 65,
      priceLabel: "$65/hr",
      badgeLabel: "VIP",
      isArmed: true,
      hasGpsTracking: true,
      isRecommended: true,
      isEmergencyDefault: true,
      isActive: true,
      sortOrder: 1,
    });
    await db.collection("command_center_services").doc("HIDDEN_SVC").set({
      code: "HIDDEN_SVC",
      title: "Internal Only",
      tierCode: "PERSONAL",
      priceLabel: "$199/hr",
      isActive: false,
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("command_center_tiers").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("command_center_services").where("isActive", "==", true).get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("command_center_tiers").where("isActive", "==", true).get());
  await assertSucceeds(aliceDb.collection("command_center_services").where("isActive", "==", true).get());

  // Unfiltered list or inactive get must fail for normal user
  await assertFails(aliceDb.collection("command_center_tiers").get());
  await assertFails(aliceDb.collection("command_center_services").get());
  await assertFails(aliceDb.collection("command_center_tiers").doc("ARCHIVED").get());
  await assertFails(aliceDb.collection("command_center_services").doc("HIDDEN_SVC").get());

  // Non-admin user cannot publish/modify catalog pricing
  await assertFails(
    aliceDb.collection("command_center_services").doc("FREE_SVC").set({
      code: "FREE_SVC",
      title: "Tampered Free",
      tierCode: "PERSONAL",
      hourlyRate: 0,
      priceLabel: "$0/hr",
      isActive: true,
    })
  );
});

test("Catalog tiers and services: Command Center admin can manage pricing and services", async () => {
  await seedAdmin();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  await assertSucceeds(
    adminDb.collection("command_center_tiers").doc("EVENT").set({
      code: "EVENT",
      title: "Event Security",
      startingRate: "From $85/hr",
      isActive: true,
      sortOrder: 2,
    })
  );

  await assertSucceeds(
    adminDb.collection("command_center_services").doc("EVENT_CROWD").set({
      code: "EVENT_CROWD",
      title: "Venue & Crowd Detail",
      tierCode: "EVENT",
      hourlyRate: 85,
      priceLabel: "$85/hr",
      isActive: true,
      sortOrder: 1,
    })
  );
});

// --- DISPATCHES QUEUE ---

test("Dispatches queue: owner creates REQUESTED dispatch, cannot spoof guard or read unfiltered", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const dispatchRef = aliceDb.collection("dispatches").doc("disp_001");

  // Valid initial dispatch request
  await assertSucceeds(
    dispatchRef.set({
      userId: ALICE_UID,
      securityTier: "PERSONAL",
      serviceType: "ESCORT_GPS",
      serviceName: "Executive Escort",
      status: "REQUESTED",
      isEmergency: false,
      locationName: "742 Evergreen Terrace",
      latitude: 37.789,
      longitude: -122.401,
      targetDateTime: "Now (Immediate Dispatch)",
      isScheduled: false,
      durationHours: 4,
      hourlyRate: 65,
      priceLabel: "$65/hr",
      estimatedTotalLabel: "$260 est. (4h)",
      notes: "Meet at main lobby",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Client matching query passes
  await assertSucceeds(aliceDb.collection("dispatches").where("userId", "==", ALICE_UID).get());
  // Client unfiltered query fails
  await assertFails(aliceDb.collection("dispatches").get());

  // Client cannot fabricate DISPATCHED status or assign a fake guard on create
  await assertFails(
    aliceDb.collection("dispatches").doc("disp_spoof_status").set({
      userId: ALICE_UID,
      serviceType: "ESCORT_GPS",
      serviceName: "Executive Escort",
      status: "DISPATCHED",
      isEmergency: true,
      locationName: "Lobby",
      latitude: 37.789,
      longitude: -122.401,
      targetDateTime: "Now",
      isScheduled: false,
      guardName: "Fake Officer",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner cannot tamper guard assignment or pricing on update
  await assertFails(
    dispatchRef.update({
      guardName: "Self Assigned Officer",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    dispatchRef.update({
      status: "ON_SITE",
      updatedAt: new Date(),
    })
  );

  // Owner CAN submit rating/comment or cancel
  await assertSucceeds(
    dispatchRef.update({
      rating: 5,
      feedbackComment: "Prompt response",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    dispatchRef.update({
      status: "CANCELLED",
      updatedAt: new Date(),
    })
  );

  // Bob cannot read or modify Alice's dispatch
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("dispatches").doc("disp_001").get());
  await assertFails(bobDb.collection("dispatches").where("userId", "==", ALICE_UID).get());
});

test("Dispatches queue: Command Center admin assigns real guard & transitions status", async () => {
  await seedAdmin();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const dispatchRef = aliceDb.collection("dispatches").doc("disp_002");

  await assertSucceeds(
    dispatchRef.set({
      userId: ALICE_UID,
      securityTier: "PERSONAL",
      serviceType: "SOS_EMERGENCY",
      serviceName: "Emergency Rapid Response",
      status: "REQUESTED",
      isEmergency: true,
      locationName: "Live GPS Signal",
      latitude: 37.789,
      longitude: -122.401,
      targetDateTime: "Immediate SOS Emergency",
      isScheduled: false,
      createdAt: new Date(Date.now() - 2000),
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(
    adminDb.collection("dispatches").doc("disp_002").update({
      status: "DISPATCHED",
      guardName: "Officer Jordan Wells",
      guardBadge: "CMD-402",
      guardPhone: "+1-555-0402",
      guardVehicle: "Response SUV #4",
      deploymentId: "dep_002",
      guardLat: 37.792,
      guardLng: -122.405,
      etaMinutes: 6,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(dispatchRef.get());
});

// --- DEPLOYMENTS COLLECTION ---

test("Deployments: owner queries own deployments, cross-user & unauth rejected, only admin writes", async () => {
  await seedAdmin();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  await assertSucceeds(
    adminDb.collection("deployments").doc("dep_alice_1").set({
      clientUserId: ALICE_UID,
      requestId: "disp_002",
      guardName: "Officer Jordan Wells",
      badgeNumber: "CMD-402",
      serviceType: "Emergency Rapid Response",
      phone: "+1-555-0402",
      vehicle: "Response SUV #4",
      yearsOfExperience: 9,
      rating: 4.9,
      completedMissions: 310,
      specializations: ["Rapid Response", "Medical First Aid"],
      certifications: ["State Security License"],
      guardLat: 37.792,
      guardLng: -122.405,
      status: "DISPATCHED",
      isRecommended: true,
      isActive: true,
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertSucceeds(aliceDb.collection("deployments").where("clientUserId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("deployments").get());
  await assertFails(bobDb.collection("deployments").doc("dep_alice_1").get());
  await assertFails(unauthDb.collection("deployments").where("clientUserId", "==", ALICE_UID).get());
  await assertFails(
    aliceDb.collection("deployments").doc("dep_fake").set({
      clientUserId: ALICE_UID,
      requestId: "disp_002",
      guardName: "Fake Self Guard",
      isActive: true,
    })
  );
});

// --- USER SCOPED SECURITY REQUESTS & PROFILES ---

test("Unauthenticated user: cannot read user security requests", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("security_requests").get());
});

test("Authenticated user: cannot read another user's security requests", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("security_requests").doc("bob_req").set({
      userId: BOB_UID,
      serviceType: "ARMED_GUARD",
      serviceName: "Armed Guard",
      status: "REQUESTED",
      isEmergency: false,
      locationName: "123 Main St",
      latitude: 37.7749,
      longitude: -122.4194,
      targetDateTime: "2026-11-12 10:00",
      isScheduled: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("security_requests").doc("bob_req").get());
});

test("Authenticated user: can create and read their own initial security request without fake guard", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const reqRef = aliceDb.collection("users").doc(ALICE_UID).collection("security_requests").doc("req_001");

  await assertSucceeds(
    reqRef.set({
      userId: ALICE_UID,
      securityTier: "PERSONAL",
      serviceType: "ESCORT_GPS",
      serviceName: "Personal Security Escort",
      status: "REQUESTED",
      isEmergency: true,
      locationName: "Downtown Financial Plaza",
      latitude: 37.789,
      longitude: -122.401,
      targetDateTime: "Now",
      isScheduled: false,
      hourlyRate: 65,
      priceLabel: "$65/hr",
      notes: "Requires immediate executive escort",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(reqRef.get());
});

test("Authenticated user: cannot inject invalid status, spoof ownership, or spoof guard in security_requests", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const reqRef = aliceDb.collection("users").doc(ALICE_UID).collection("security_requests").doc("req_bad");

  // Spoofing userId to Bob
  await assertFails(
    reqRef.set({
      userId: BOB_UID,
      serviceType: "STANDARD_GUARD",
      serviceName: "Standard Guard",
      status: "REQUESTED",
      isEmergency: false,
      locationName: "Site Alpha",
      latitude: 37.7,
      longitude: -122.4,
      targetDateTime: "Now",
      isScheduled: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid status
  await assertFails(
    reqRef.set({
      userId: ALICE_UID,
      serviceType: "STANDARD_GUARD",
      serviceName: "Standard Guard",
      status: "INVALID_STATUS",
      isEmergency: false,
      locationName: "Site Alpha",
      latitude: 37.7,
      longitude: -122.4,
      targetDateTime: "Now",
      isScheduled: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Client pre-assigning guard
  await assertFails(
    reqRef.set({
      userId: ALICE_UID,
      serviceType: "STANDARD_GUARD",
      serviceName: "Standard Guard",
      status: "REQUESTED",
      isEmergency: false,
      locationName: "Site Alpha",
      latitude: 37.7,
      longitude: -122.4,
      targetDateTime: "Now",
      isScheduled: false,
      guardName: "Fake Client Guard",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated user: can save and update their own profile with emergency contact and addresses", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice Mercer",
      email: "alice@example.com",
      phoneNumber: "+1-555-0100",
      emergencyContact: "Sarah Mercer (Spouse) • +1-555-0199",
      emergencyContactName: "Sarah Mercer",
      emergencyContactPhone: "+1-555-0199",
      emergencyContactRelation: "Spouse",
      homeAddress: "420 Oak Ave, San Francisco, CA",
      businessAddress: "101 California St, Suite 400",
      preferredAddressType: "HOME",
      defaultLocation: "420 Oak Ave, San Francisco, CA",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    profileRef.update({
      preferredAddressType: "BUSINESS",
      defaultLocation: "101 California St, Suite 400",
      fcmToken: "fcm_token_sample_abc123",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot bypass preferredAddressType or write another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceProfileRef = aliceDb.collection("users").doc(ALICE_UID);
  const bobProfileRef = aliceDb.collection("users").doc(BOB_UID);

  await assertFails(
    bobProfileRef.set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceProfileRef.set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      preferredAddressType: "INVALID_TYPE",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
