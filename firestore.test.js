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
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("public_profiles").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("tournaments").where("isPublic", "==", true).get());
  await assertFails(
    unauthDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice",
      displayName: "Alice",
      discoverable: true,
      online: true,
      points: 50,
      xp: 0,
      level: 1,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("PublicProfile: owner create/update, cross-user denial, and leaderboard query", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_quiz",
      displayName: "Alice",
      discoverable: true,
      online: true,
      points: 50,
      xp: 0,
      level: 1,
      matchesPlayed: 0,
      wins: 0,
      correctAnswers: 0,
      totalAnswered: 0,
      winStreak: 0,
      tournamentsWon: 0,
      equippedFrame: "starter",
      equippedTitle: "Rookie",
      ownedItems: ["starter"],
      hintPowerUps: 1,
      freezePowerUps: 0,
      doubleXpPowerUps: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      points: 80,
      xp: 75,
      level: 2,
      updatedAt: pastDate(1000),
    })
  );

  // Bob cannot modify Alice's profile
  await assertFails(
    bobDb.collection("public_profiles").doc(ALICE_UID).update({
      points: 99999,
      updatedAt: pastDate(1000),
    })
  );

  // Filtered leaderboard query succeeds, unfiltered fails
  await assertSucceeds(
    bobDb.collection("public_profiles").where("discoverable", "==", true).get()
  );
  await assertFails(bobDb.collection("public_profiles").get());

  // Ghost field & future timestamp rejected
  await assertFails(
    bobDb.collection("public_profiles").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_quiz",
      displayName: "Bob",
      discoverable: true,
      online: true,
      points: 50,
      xp: 0,
      level: 1,
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Private account, match history, and transactions are strictly owner-only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("account").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      passcodeEnabled: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("account").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("match_history").doc("m1").set({
      mode: "random_ai",
      title: "Science Sprint",
      topic: "Science",
      correctCount: 4,
      totalQuestions: 5,
      pointsEarned: 40,
      xpEarned: 100,
      won: true,
      createdAt: created,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("match_history").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1").set({
      type: "quiz_reward",
      amount: 40,
      balanceAfter: 90,
      description: "Random Quiz: 4 correct answers",
      createdAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1").update({
      amount: 1000,
    })
  );
});

test("Match rooms: create, join, live player score updates, and queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("match_rooms").doc("room_1").set({
      hostId: ALICE_UID,
      hostName: "alice_quiz",
      title: "Quick Tech Clash",
      topic: "Tech",
      status: "waiting",
      isPublic: true,
      maxPlayers: 4,
      playerCount: 1,
      memberUids: [ALICE_UID],
      questions: [{ question: "What is CPU?", options: ["A", "B", "C", "D"], correctIndex: 0 }],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("match_rooms").doc("room_1").collection("players").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_quiz",
      score: 0,
      correctCount: 0,
      answeredCount: 0,
      finished: false,
      updatedAt: created,
    })
  );

  // Bob joins
  await assertSucceeds(
    bobDb.collection("match_rooms").doc("room_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      playerCount: 2,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("match_rooms").doc("room_1").collection("players").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_quiz",
      score: 10,
      correctCount: 1,
      answeredCount: 1,
      finished: false,
      updatedAt: pastDate(1000),
    })
  );

  // Filtered room query succeeds
  await assertSucceeds(bobDb.collection("match_rooms").where("isPublic", "==", true).get());
  await assertFails(bobDb.collection("match_rooms").get());
});

test("Tournaments: create, broadcast invites, join with entryFee, and winner claim", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("tournaments").doc("t_1").set({
      hostId: ALICE_UID,
      hostName: "alice_quiz",
      title: "Grand Friday Cup",
      category: "Mixed",
      entryFee: 20,
      prizePool: 20,
      maxPlayers: 16,
      participantCount: 1,
      inviteScope: "online_all",
      status: "open",
      isPublic: true,
      memberUids: [ALICE_UID],
      questions: [{ question: "Capital of Japan?", options: ["Tokyo", "Seoul", "Osaka", "Kyoto"], correctIndex: 0 }],
      prizeClaimed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("tournament_invites").doc("inv_1").set({
      tournamentId: "t_1",
      tournamentTitle: "Grand Friday Cup",
      hostId: ALICE_UID,
      hostName: "alice_quiz",
      entryFee: 20,
      targetUid: "ALL_ONLINE",
      inviteType: "broadcast_online",
      createdAt: created,
    })
  );

  // Bob queries invites with whereIn [BOB_UID, "ALL_ONLINE"]
  await assertSucceeds(
    bobDb.collection("tournament_invites").where("targetUid", "in", [BOB_UID, "ALL_ONLINE"]).get()
  );

  // Bob joins tournament and increments participantCount and prizePool by entryFee
  await assertSucceeds(
    bobDb.collection("tournaments").doc("t_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      participantCount: 2,
      prizePool: 40,
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    bobDb.collection("tournaments").doc("t_1").collection("participants").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_quiz",
      score: 10,
      correctCount: 1,
      finished: true,
      joinedAt: created,
      updatedAt: pastDate(1500),
    })
  );

  // Host completes tournament with Bob as winner
  await assertSucceeds(
    aliceDb.collection("tournaments").doc("t_1").update({
      status: "completed",
      winnerUid: BOB_UID,
      winnerName: "bob_quiz",
      updatedAt: pastDate(1000),
    })
  );

  // Bob claims prize once; second claim fails
  await assertSucceeds(
    bobDb.collection("tournaments").doc("t_1").update({
      prizeClaimed: true,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    bobDb.collection("tournaments").doc("t_1").update({
      prizeClaimed: true,
      updatedAt: pastDate(200),
    })
  );
});

test("Friends, friend-only chat, and friend point gifts enforce strict membership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = pastDate(5000);
  const pairId = `${ALICE_UID}_${BOB_UID}`;

  // Friend request flow
  await assertSucceeds(
    aliceDb.collection("friend_requests").doc("req_1").set({
      fromUid: ALICE_UID,
      fromUsername: "alice_quiz",
      toUid: BOB_UID,
      toUsername: "bob_quiz",
      status: "pending",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    bobDb.collection("friend_requests").doc("req_1").update({
      status: "accepted",
      updatedAt: pastDate(2000),
    })
  );

  // Friendship document
  await assertSucceeds(
    bobDb.collection("friendships").doc(pairId).set({
      memberUids: [ALICE_UID, BOB_UID],
      user1Uid: ALICE_UID,
      user1Name: "alice_quiz",
      user2Uid: BOB_UID,
      user2Name: "bob_quiz",
      createdAt: created,
    })
  );

  // Friends can chat; non-friend Charlie is rejected
  await assertSucceeds(
    aliceDb.collection("chats").doc(pairId).collection("messages").doc("msg_1").set({
      senderUid: ALICE_UID,
      senderName: "alice_quiz",
      text: "Ready for the tournament?",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: pastDate(1000),
    })
  );
  await assertFails(
    charlieDb.collection("chats").doc(pairId).collection("messages").doc("msg_2").set({
      senderUid: CHARLIE_UID,
      senderName: "charlie",
      text: "Let me in!",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("chats").doc(pairId).collection("messages").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(
    charlieDb.collection("chats").doc(pairId).collection("messages").where("memberUids", "array-contains", BOB_UID).get()
  );

  // Friend gift flow
  await assertSucceeds(
    aliceDb.collection("friend_gifts").doc("gift_1").set({
      fromUid: ALICE_UID,
      fromUsername: "alice_quiz",
      toUid: BOB_UID,
      toUsername: "bob_quiz",
      friendshipId: pairId,
      amount: 30,
      note: "Good luck!",
      status: "pending",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    bobDb.collection("friend_gifts").doc("gift_1").update({
      status: "claimed",
      updatedAt: pastDate(500),
    })
  );
});
