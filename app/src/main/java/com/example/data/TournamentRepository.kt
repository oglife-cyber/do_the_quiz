package com.example.data

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.QuizQuestion
import com.example.model.Tournament
import com.example.model.TournamentInvite
import com.example.model.TournamentParticipant
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TournamentRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    fun observeTournaments(): Flow<List<Tournament>> =
        db.collection("tournaments")
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeTournament(tournamentId: String): Flow<Tournament?> =
        db.collection("tournaments").document(tournamentId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }

    fun observeParticipants(tournamentId: String): Flow<List<TournamentParticipant>> =
        db.collection("tournaments").document(tournamentId).collection("participants")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TournamentParticipant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareByDescending<TournamentParticipant> { it.score }.thenByDescending { it.correctCount })
            }

    fun observeMyInvites(uid: String): Flow<List<TournamentInvite>> =
        db.collection("tournament_invites")
            .whereIn("targetUid", listOf(uid, "ALL_ONLINE"))
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TournamentInvite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun createTournament(
        hostUsername: String,
        title: String,
        category: String,
        entryFee: Int,
        maxPlayers: Int,
        inviteScope: String,
        invitedFriendUids: List<String>,
        questions: List<QuizQuestion>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanTitle = title.trim()
        if (cleanTitle.length !in 3..80) {
            return Result.failure(IllegalArgumentException("Tournament title must be 3–80 characters."))
        }
        if (entryFee !in 0..10000) {
            return Result.failure(IllegalArgumentException("Entry fee must be 0–10,000 points."))
        }
        if (questions.isEmpty() || questions.size > 20) {
            return Result.failure(IllegalArgumentException("Add between 1 and 20 questions."))
        }

        val tourneyRef = db.collection("tournaments").document()
        val profileRef = db.collection("public_profiles").document(uid)
        val txRef = db.collection("users").document(uid).collection("transactions").document()
        val participantRef = tourneyRef.collection("participants").document(uid)

        return try {
            db.runTransaction { tx ->
                val profSnap = tx.get(profileRef)
                val currentPoints = profSnap.getLong("points") ?: 0L
                if (currentPoints < entryFee) {
                    throw IllegalStateException("You need $entryFee points to cover the host entry stake.")
                }
                val newBalance = currentPoints - entryFee
                if (entryFee > 0) {
                    tx.update(
                        profileRef,
                        mapOf("points" to newBalance, "updatedAt" to FieldValue.serverTimestamp())
                    )
                    tx.set(
                        txRef,
                        mapOf(
                            "type" to "tournament_entry",
                            "amount" to -entryFee,
                            "balanceAfter" to newBalance,
                            "description" to "Hosted Tournament: ${cleanTitle.take(50)}",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.set(
                    tourneyRef,
                    mapOf(
                        "hostId" to uid,
                        "hostName" to hostUsername.take(40),
                        "title" to cleanTitle,
                        "category" to category.take(40),
                        "entryFee" to entryFee,
                        "prizePool" to entryFee,
                        "maxPlayers" to maxPlayers.coerceIn(2, 50),
                        "participantCount" to 1,
                        "inviteScope" to inviteScope,
                        "status" to "open",
                        "isPublic" to true,
                        "memberUids" to listOf(uid),
                        "questions" to questions.map { it.toFirestoreMap() },
                        "prizeClaimed" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    participantRef,
                    mapOf(
                        "userId" to uid,
                        "username" to hostUsername.take(40),
                        "score" to 0,
                        "correctCount" to 0,
                        "finished" to false,
                        "joinedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (inviteScope == "online_all") {
                    val invRef = db.collection("tournament_invites").document()
                    tx.set(
                        invRef,
                        mapOf(
                            "tournamentId" to tourneyRef.id,
                            "tournamentTitle" to cleanTitle,
                            "hostId" to uid,
                            "hostName" to hostUsername.take(40),
                            "entryFee" to entryFee,
                            "targetUid" to "ALL_ONLINE",
                            "inviteType" to "broadcast_online",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    invitedFriendUids.distinct().take(25).forEach { friendUid ->
                        val invRef = db.collection("tournament_invites").document()
                        tx.set(
                            invRef,
                            mapOf(
                                "tournamentId" to tourneyRef.id,
                                "tournamentTitle" to cleanTitle,
                                "hostId" to uid,
                                "hostName" to hostUsername.take(40),
                                "entryFee" to entryFee,
                                "targetUid" to friendUid,
                                "inviteType" to "direct",
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }.await()
            Result.success(tourneyRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, tourneyRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinTournament(tournamentId: String, username: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val tourneyRef = db.collection("tournaments").document(tournamentId)
        val profileRef = db.collection("public_profiles").document(uid)
        val txRef = db.collection("users").document(uid).collection("transactions").document()
        val participantRef = tourneyRef.collection("participants").document(uid)

        return try {
            db.runTransaction { tx ->
                val tSnap = tx.get(tourneyRef)
                if (!tSnap.exists()) throw IllegalStateException("Tournament not found.")
                val status = tSnap.getString("status") ?: "open"
                if (status !in listOf("open", "live")) {
                    throw IllegalStateException("This tournament is already completed.")
                }
                @Suppress("UNCHECKED_CAST")
                val members = (tSnap.get("memberUids") as? List<String>).orEmpty()
                if (uid in members) return@runTransaction

                val maxPlayers = (tSnap.getLong("maxPlayers") ?: 16L).toInt()
                val participantCount = (tSnap.getLong("participantCount") ?: 0L).toInt()
                if (participantCount >= maxPlayers) {
                    throw IllegalStateException("Tournament is full.")
                }

                val entryFee = (tSnap.getLong("entryFee") ?: 0L).toInt()
                val currentPool = (tSnap.getLong("prizePool") ?: 0L).toInt()
                val title = tSnap.getString("title") ?: "Tournament"

                val pSnap = tx.get(profileRef)
                val currentPoints = pSnap.getLong("points") ?: 0L
                if (currentPoints < entryFee) {
                    throw IllegalStateException("Insufficient points! Entry fee is $entryFee pts.")
                }

                val newBalance = currentPoints - entryFee
                if (entryFee > 0) {
                    tx.update(
                        profileRef,
                        mapOf("points" to newBalance, "updatedAt" to FieldValue.serverTimestamp())
                    )
                    tx.set(
                        txRef,
                        mapOf(
                            "type" to "tournament_entry",
                            "amount" to -entryFee,
                            "balanceAfter" to newBalance,
                            "description" to "Joined Tournament: ${title.take(50)}",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.update(
                    tourneyRef,
                    mapOf(
                        "memberUids" to members + uid,
                        "participantCount" to participantCount + 1,
                        "prizePool" to currentPool + entryFee,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    participantRef,
                    mapOf(
                        "userId" to uid,
                        "username" to username.take(40),
                        "score" to 0,
                        "correctCount" to 0,
                        "finished" to false,
                        "joinedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, tourneyRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitTournamentScore(
        tournamentId: String,
        score: Int,
        correctCount: Int
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val partRef = db.collection("tournaments").document(tournamentId).collection("participants").document(uid)
        return try {
            partRef.update(
                mapOf(
                    "score" to score,
                    "correctCount" to correctCount,
                    "finished" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, partRef.path)
            Result.failure(e)
        }
    }

    suspend fun completeTournament(
        tournamentId: String,
        winnerUid: String,
        winnerName: String
    ): Result<Unit> {
        val tourneyRef = db.collection("tournaments").document(tournamentId)
        return try {
            tourneyRef.update(
                mapOf(
                    "status" to "completed",
                    "winnerUid" to winnerUid,
                    "winnerName" to winnerName.take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, tourneyRef.path)
            Result.failure(e)
        }
    }

    suspend fun claimTournamentPrize(tournamentId: String): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val tourneyRef = db.collection("tournaments").document(tournamentId)
        val profileRef = db.collection("public_profiles").document(uid)
        val txRef = db.collection("users").document(uid).collection("transactions").document()
        return try {
            var claimedAmount = 0
            db.runTransaction { tx ->
                val tSnap = tx.get(tourneyRef)
                val status = tSnap.getString("status") ?: ""
                val winnerUid = tSnap.getString("winnerUid") ?: ""
                val prizeClaimed = tSnap.getBoolean("prizeClaimed") ?: false
                val prizePool = (tSnap.getLong("prizePool") ?: 0L).toInt()
                val title = tSnap.getString("title") ?: "Tournament"

                if (status != "completed" || winnerUid != uid || prizeClaimed) {
                    throw IllegalStateException("Prize is not eligible to claim.")
                }

                val pSnap = tx.get(profileRef)
                val currentPoints = pSnap.getLong("points") ?: 0L
                val tournamentsWon = pSnap.getLong("tournamentsWon") ?: 0L
                val newBalance = currentPoints + prizePool
                claimedAmount = prizePool

                tx.update(
                    tourneyRef,
                    mapOf("prizeClaimed" to true, "updatedAt" to FieldValue.serverTimestamp())
                )
                tx.update(
                    profileRef,
                    mapOf(
                        "points" to newBalance,
                        "tournamentsWon" to tournamentsWon + 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (prizePool > 0) {
                    tx.set(
                        txRef,
                        mapOf(
                            "type" to "tournament_prize",
                            "amount" to prizePool,
                            "balanceAfter" to newBalance,
                            "description" to "Won Tournament Prize: ${title.take(45)}",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(claimedAmount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, tourneyRef.path)
            Result.failure(e)
        }
    }

    suspend fun setTournamentLive(tournamentId: String): Result<Unit> {
        val tourneyRef = db.collection("tournaments").document(tournamentId)
        return try {
            tourneyRef.update(
                mapOf("status" to "live", "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, tourneyRef.path)
            Result.failure(e)
        }
    }
}
