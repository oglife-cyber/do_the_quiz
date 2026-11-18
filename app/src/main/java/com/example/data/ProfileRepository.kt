package com.example.data

import com.example.core.OperationType
import com.example.core.RankSystem
import com.example.core.ShopCategory
import com.example.core.ShopItem
import com.example.core.handleFirestoreError
import com.example.core.sha256Hex
import com.example.model.MatchHistoryEntry
import com.example.model.PointTransaction
import com.example.model.PrivateAccount
import com.example.model.PublicProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeMyProfile(uid: String): Flow<PublicProfile?> =
        db.collection("public_profiles").document(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }

    fun observePrivateAccount(uid: String): Flow<PrivateAccount?> =
        db.collection("users").document(uid).collection("private").document("account")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(PrivateAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }

    fun observeGlobalLeaderboard(): Flow<List<PublicProfile>> =
        db.collection("public_profiles")
            .whereEqualTo("discoverable", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareByDescending<PublicProfile> { it.points }.thenByDescending { it.xp })
            }

    fun observeMatchHistory(uid: String): Flow<List<MatchHistoryEntry>> =
        db.collection("users").document(uid).collection("match_history")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MatchHistoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeTransactions(uid: String): Flow<List<PointTransaction>> =
        db.collection("users").document(uid).collection("transactions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PointTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun completeSignUpOnboarding(
        usernameInput: String,
        displayNameInput: String,
        optionalPin: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val email = auth.currentUser?.email.orEmpty()
        val cleanUsername = usernameInput.trim().lowercase().replace(Regex("[^a-z0-9_\\-]"), "")
        val cleanDisplay = displayNameInput.trim().ifBlank { cleanUsername }
        if (cleanUsername.length !in 3..24) {
            return Result.failure(IllegalArgumentException("Username must be 3–24 characters (letters, numbers, _ or -)."))
        }
        if (cleanDisplay.length !in 1..40) {
            return Result.failure(IllegalArgumentException("Display name must be 1–40 characters."))
        }
        if (optionalPin.isNotBlank() && optionalPin.length != 4) {
            return Result.failure(IllegalArgumentException("Wallet passcode must be exactly 4 digits or left blank."))
        }

        val profilePath = "public_profiles/$uid"
        return try {
            // Check username uniqueness among discoverable players
            val existingByUsername = db.collection("public_profiles")
                .whereEqualTo("discoverable", true)
                .whereEqualTo("username", cleanUsername)
                .get()
                .await()
            if (existingByUsername.documents.any { it.id != uid }) {
                return Result.failure(IllegalArgumentException("Username @$cleanUsername is already taken."))
            }

            val profileData = mapOf(
                "userId" to uid,
                "username" to cleanUsername,
                "displayName" to cleanDisplay,
                "discoverable" to true,
                "online" to true,
                "points" to 30L,
                "xp" to 0L,
                "level" to 1,
                "matchesPlayed" to 0L,
                "wins" to 0L,
                "correctAnswers" to 0L,
                "totalAnswered" to 0L,
                "winStreak" to 0L,
                "tournamentsWon" to 0L,
                "equippedFrame" to "starter",
                "equippedTitle" to "Rookie",
                "ownedItems" to listOf("starter"),
                "hintPowerUps" to 1L,
                "freezePowerUps" to 1L,
                "doubleXpPowerUps" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val privateData = mutableMapOf<String, Any>(
                "userId" to uid,
                "passcodeEnabled" to optionalPin.isNotBlank(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (email.isNotBlank()) privateData["email"] = email.take(120)
            if (optionalPin.isNotBlank()) privateData["passcodeHash"] = sha256Hex(optionalPin)

            val starterTx = mapOf(
                "type" to "quiz_reward",
                "amount" to 30,
                "balanceAfter" to 30L,
                "description" to "Welcome Arena Starter Bonus",
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(db.collection("public_profiles").document(uid), profileData)
            batch.set(db.collection("users").document(uid).collection("private").document("account"), privateData)
            batch.set(db.collection("users").document(uid).collection("transactions").document(), starterTx)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, profilePath)
            Result.failure(e)
        }
    }

    suspend fun setOnlineStatus(online: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "public_profiles/$uid"
        return try {
            db.collection("public_profiles").document(uid).update(
                mapOf("online" to online, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateWalletPasscode(enabled: Boolean, newPin: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (enabled && newPin.length != 4) {
            return Result.failure(IllegalArgumentException("Passcode must be 4 digits."))
        }
        val path = "users/$uid/private/account"
        return try {
            val updates = mutableMapOf<String, Any>(
                "passcodeEnabled" to enabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (enabled) {
                updates["passcodeHash"] = sha256Hex(newPin)
            }
            db.collection("users").document(uid).collection("private").document("account")
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordQuizCompletion(
        mode: String,
        title: String,
        topic: String,
        correctCount: Int,
        totalQuestions: Int,
        won: Boolean,
        usedDoubleXp: Boolean
    ): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val pointsEarned = correctCount * RankSystem.POINTS_PER_CORRECT_ANSWER
        val baseXp = (correctCount * RankSystem.XP_PER_CORRECT_ANSWER) + if (won) RankSystem.XP_WIN_BONUS else 0
        val xpEarned = if (usedDoubleXp) baseXp * 2 else baseXp
        val profileRef = db.collection("public_profiles").document(uid)
        val historyRef = db.collection("users").document(uid).collection("match_history").document()
        val txRef = db.collection("users").document(uid).collection("transactions").document()

        return try {
            db.runTransaction { tx ->
                val snap = tx.get(profileRef)
                val currentPoints = snap.getLong("points") ?: 0L
                val currentXp = snap.getLong("xp") ?: 0L
                val currentMatches = snap.getLong("matchesPlayed") ?: 0L
                val currentWins = snap.getLong("wins") ?: 0L
                val currentCorrect = snap.getLong("correctAnswers") ?: 0L
                val currentTotal = snap.getLong("totalAnswered") ?: 0L
                val currentStreak = snap.getLong("winStreak") ?: 0L

                val newPoints = currentPoints + pointsEarned
                val newXp = currentXp + xpEarned
                val newLevel = RankSystem.levelFromXp(newXp)
                val newStreak = if (won) currentStreak + 1L else 0L

                val profileUpdates = mutableMapOf<String, Any>(
                    "points" to newPoints,
                    "xp" to newXp,
                    "level" to newLevel,
                    "matchesPlayed" to currentMatches + 1L,
                    "wins" to currentWins + if (won) 1L else 0L,
                    "correctAnswers" to currentCorrect + correctCount,
                    "totalAnswered" to currentTotal + totalQuestions,
                    "winStreak" to newStreak,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (usedDoubleXp) {
                    val currentDxp = snap.getLong("doubleXpPowerUps") ?: 0L
                    profileUpdates["doubleXpPowerUps"] = (currentDxp - 1L).coerceAtLeast(0L)
                }
                tx.update(profileRef, profileUpdates)

                tx.set(
                    historyRef,
                    mapOf(
                        "mode" to mode,
                        "title" to title.take(80),
                        "topic" to topic.take(40),
                        "correctCount" to correctCount,
                        "totalQuestions" to totalQuestions,
                        "pointsEarned" to pointsEarned,
                        "xpEarned" to xpEarned,
                        "won" to won,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                if (pointsEarned > 0) {
                    tx.set(
                        txRef,
                        mapOf(
                            "type" to "quiz_reward",
                            "amount" to pointsEarned,
                            "balanceAfter" to newPoints,
                            "description" to "${title.take(60)}: $correctCount/${totalQuestions} correct",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(pointsEarned)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profileRef.path)
            Result.failure(e)
        }
    }

    suspend fun consumePowerUp(type: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val profileRef = db.collection("public_profiles").document(uid)
        val field = when (type) {
            "hint" -> "hintPowerUps"
            "freeze" -> "freezePowerUps"
            "double_xp" -> "doubleXpPowerUps"
            else -> return Result.failure(IllegalArgumentException("Unknown power-up"))
        }
        return try {
            db.runTransaction { tx ->
                val snap = tx.get(profileRef)
                val count = snap.getLong(field) ?: 0L
                if (count <= 0L) throw IllegalStateException("No power-ups remaining.")
                tx.update(
                    profileRef,
                    mapOf(field to count - 1L, "updatedAt" to FieldValue.serverTimestamp())
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileRef.path)
            Result.failure(e)
        }
    }

    suspend fun purchaseShopItem(item: ShopItem): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val profileRef = db.collection("public_profiles").document(uid)
        val txRef = db.collection("users").document(uid).collection("transactions").document()
        return try {
            db.runTransaction { tx ->
                val snap = tx.get(profileRef)
                val currentPoints = snap.getLong("points") ?: 0L
                if (currentPoints < item.price) {
                    throw IllegalStateException("Not enough points! Need ${item.price} pts.")
                }
                @Suppress("UNCHECKED_CAST")
                val owned = (snap.get("ownedItems") as? List<String>).orEmpty()
                if (item.category != ShopCategory.POWERUP && item.id in owned) {
                    throw IllegalStateException("You already own ${item.name}.")
                }

                val newBalance = currentPoints - item.price
                val updates = mutableMapOf<String, Any>(
                    "points" to newBalance,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                when (item.category) {
                    ShopCategory.FRAME -> {
                        updates["ownedItems"] = (owned + item.id).distinct()
                        updates["equippedFrame"] = item.id
                    }
                    ShopCategory.TITLE -> {
                        updates["ownedItems"] = (owned + item.id).distinct()
                        updates["equippedTitle"] = item.name
                    }
                    ShopCategory.POWERUP -> {
                        when (item.id) {
                            "powerup_hint" -> updates["hintPowerUps"] = (snap.getLong("hintPowerUps") ?: 0L) + 1L
                            "powerup_freeze" -> updates["freezePowerUps"] = (snap.getLong("freezePowerUps") ?: 0L) + 1L
                            "powerup_double_xp" -> updates["doubleXpPowerUps"] = (snap.getLong("doubleXpPowerUps") ?: 0L) + 1L
                        }
                    }
                }
                tx.update(profileRef, updates)
                tx.set(
                    txRef,
                    mapOf(
                        "type" to "shop_purchase",
                        "amount" to -item.price,
                        "balanceAfter" to newBalance,
                        "description" to "Purchased ${item.name}",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profileRef.path)
            Result.failure(e)
        }
    }

    suspend fun equipCosmetic(item: ShopItem): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val profileRef = db.collection("public_profiles").document(uid)
        return try {
            val field = when (item.category) {
                ShopCategory.FRAME -> "equippedFrame"
                ShopCategory.TITLE -> "equippedTitle"
                ShopCategory.POWERUP -> return Result.success(Unit)
            }
            val value = if (item.category == ShopCategory.FRAME) item.id else item.name
            profileRef.update(mapOf(field to value, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileRef.path)
            Result.failure(e)
        }
    }
}
