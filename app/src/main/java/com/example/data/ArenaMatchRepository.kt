package com.example.data

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.MatchPlayer
import com.example.model.MatchRoom
import com.example.model.QuizQuestion
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

class ArenaMatchRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    fun observePublicRooms(): Flow<List<MatchRoom>> =
        db.collection("match_rooms")
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MatchRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeRoom(roomId: String): Flow<MatchRoom?> =
        db.collection("match_rooms").document(roomId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(MatchRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }

    fun observeRoomPlayers(roomId: String): Flow<List<MatchPlayer>> =
        db.collection("match_rooms").document(roomId).collection("players")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MatchPlayer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.score }
            }

    suspend fun createMatchRoom(
        hostUsername: String,
        title: String,
        topic: String,
        maxPlayers: Int,
        questions: List<QuizQuestion>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanTitle = title.trim()
        if (cleanTitle.length !in 2..80) {
            return Result.failure(IllegalArgumentException("Room title must be 2–80 characters."))
        }
        if (questions.isEmpty() || questions.size > 10) {
            return Result.failure(IllegalArgumentException("Room must have 1–10 questions."))
        }
        val roomRef = db.collection("match_rooms").document()
        val playerRef = roomRef.collection("players").document(uid)
        return try {
            val batch = db.batch()
            batch.set(
                roomRef,
                mapOf(
                    "hostId" to uid,
                    "hostName" to hostUsername.take(40),
                    "title" to cleanTitle,
                    "topic" to topic.take(40),
                    "status" to "waiting",
                    "isPublic" to true,
                    "maxPlayers" to maxPlayers.coerceIn(2, 10),
                    "playerCount" to 1,
                    "memberUids" to listOf(uid),
                    "questions" to questions.map { it.toFirestoreMap() },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                playerRef,
                mapOf(
                    "userId" to uid,
                    "username" to hostUsername.take(40),
                    "score" to 0,
                    "correctCount" to 0,
                    "answeredCount" to 0,
                    "finished" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(roomRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinMatchRoom(roomId: String, username: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val roomRef = db.collection("match_rooms").document(roomId)
        val playerRef = roomRef.collection("players").document(uid)
        return try {
            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                if (!snap.exists()) throw IllegalStateException("Match room no longer exists.")
                val status = snap.getString("status") ?: "waiting"
                @Suppress("UNCHECKED_CAST")
                val members = (snap.get("memberUids") as? List<String>).orEmpty()
                val maxPlayers = (snap.getLong("maxPlayers") ?: 4L).toInt()
                val playerCount = (snap.getLong("playerCount") ?: 1L).toInt()
                if (uid !in members) {
                    if (status != "waiting") throw IllegalStateException("Match has already started.")
                    if (playerCount >= maxPlayers) throw IllegalStateException("Room is full.")
                    tx.update(
                        roomRef,
                        mapOf(
                            "memberUids" to members + uid,
                            "playerCount" to playerCount + 1,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        playerRef,
                        mapOf(
                            "userId" to uid,
                            "username" to username.take(40),
                            "score" to 0,
                            "correctCount" to 0,
                            "answeredCount" to 0,
                            "finished" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun setRoomStatus(roomId: String, status: String): Result<Unit> {
        val roomRef = db.collection("match_rooms").document(roomId)
        return try {
            roomRef.update(mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateMyProgress(
        roomId: String,
        score: Int,
        correctCount: Int,
        answeredCount: Int,
        finished: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val playerRef = db.collection("match_rooms").document(roomId).collection("players").document(uid)
        return try {
            playerRef.update(
                mapOf(
                    "score" to score,
                    "correctCount" to correctCount,
                    "answeredCount" to answeredCount,
                    "finished" to finished,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, playerRef.path)
            Result.failure(e)
        }
    }
}
