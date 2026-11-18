package com.example.data

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.ChatMessage
import com.example.model.FriendGift
import com.example.model.FriendRequest
import com.example.model.Friendship
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

class SocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    fun pairIdFor(uidA: String, uidB: String): String =
        if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"

    fun observeFriendships(uid: String): Flow<List<Friendship>> =
        db.collection("friendships")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeIncomingRequests(uid: String): Flow<List<FriendRequest>> =
        db.collection("friend_requests")
            .whereEqualTo("toUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.status == "pending" }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeOutgoingRequests(uid: String): Flow<List<FriendRequest>> =
        db.collection("friend_requests")
            .whereEqualTo("fromUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.status == "pending" }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeChatMessages(chatId: String, myUid: String): Flow<List<ChatMessage>> =
        db.collection("chats").document(chatId).collection("messages")
            .whereArrayContains("memberUids", myUid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(Long.MAX_VALUE / 2, 0) }
            }

    fun observeIncomingGifts(uid: String): Flow<List<FriendGift>> =
        db.collection("friend_gifts")
            .whereEqualTo("toUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FriendGift::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.status == "pending" }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun sendFriendRequest(
        fromUsername: String,
        toUid: String,
        toUsername: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (uid == toUid) return Result.failure(IllegalArgumentException("Cannot add yourself."))
        val reqId = "${uid}_to_${toUid}"
        val reqRef = db.collection("friend_requests").document(reqId)
        return try {
            reqRef.set(
                mapOf(
                    "fromUid" to uid,
                    "fromUsername" to fromUsername.take(40),
                    "toUid" to toUid,
                    "toUsername" to toUsername.take(40),
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun respondToFriendRequest(
        request: FriendRequest,
        accept: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val reqRef = db.collection("friend_requests").document(request.id)
        val pairId = pairIdFor(request.fromUid, request.toUid)
        val friendshipRef = db.collection("friendships").document(pairId)
        return try {
            val batch = db.batch()
            batch.update(
                reqRef,
                mapOf(
                    "status" to if (accept) "accepted" else "declined",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (accept) {
                val (u1, u1Name, u2, u2Name) = if (request.fromUid < request.toUid) {
                    listOf(request.fromUid, request.fromUsername, request.toUid, request.toUsername)
                } else {
                    listOf(request.toUid, request.toUsername, request.fromUid, request.fromUsername)
                }
                batch.set(
                    friendshipRef,
                    mapOf(
                        "memberUids" to listOf(u1, u2),
                        "user1Uid" to u1,
                        "user1Name" to u1Name,
                        "user2Uid" to u2,
                        "user2Name" to u2Name,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        chatId: String,
        senderName: String,
        memberUids: List<String>,
        text: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanText = text.trim()
        if (cleanText.isEmpty() || cleanText.length > 500) {
            return Result.failure(IllegalArgumentException("Message must be 1–500 characters."))
        }
        val msgRef = db.collection("chats").document(chatId).collection("messages").document()
        return try {
            msgRef.set(
                mapOf(
                    "senderUid" to uid,
                    "senderName" to senderName.take(40),
                    "text" to cleanText,
                    "memberUids" to memberUids,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendFriendGift(
        fromUsername: String,
        toUid: String,
        toUsername: String,
        amount: Int,
        note: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (amount !in 1..10000) {
            return Result.failure(IllegalArgumentException("Gift must be between 1 and 10,000 points."))
        }
        val pairId = pairIdFor(uid, toUid)
        val profileRef = db.collection("public_profiles").document(uid)
        val txRef = db.collection("users").document(uid).collection("transactions").document()
        val giftRef = db.collection("friend_gifts").document()
        return try {
            db.runTransaction { tx ->
                val pSnap = tx.get(profileRef)
                val currentPoints = pSnap.getLong("points") ?: 0L
                if (currentPoints < amount) {
                    throw IllegalStateException("Insufficient points! Balance: $currentPoints pts.")
                }
                val newBalance = currentPoints - amount
                tx.update(
                    profileRef,
                    mapOf("points" to newBalance, "updatedAt" to FieldValue.serverTimestamp())
                )
                tx.set(
                    txRef,
                    mapOf(
                        "type" to "friend_gift_sent",
                        "amount" to -amount,
                        "balanceAfter" to newBalance,
                        "description" to "Sent points to @$toUsername",
                        "counterpartyName" to toUsername.take(40),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                val giftPayload = mutableMapOf<String, Any>(
                    "fromUid" to uid,
                    "fromUsername" to fromUsername.take(40),
                    "toUid" to toUid,
                    "toUsername" to toUsername.take(40),
                    "friendshipId" to pairId,
                    "amount" to amount,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (note.isNotBlank()) {
                    giftPayload["note"] = note.trim().take(100)
                }
                tx.set(giftRef, giftPayload)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, giftRef.path)
            Result.failure(e)
        }
    }

    suspend fun claimFriendGift(gift: FriendGift): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val giftRef = db.collection("friend_gifts").document(gift.id)
        val profileRef = db.collection("public_profiles").document(uid)
        val txRef = db.collection("users").document(uid).collection("transactions").document()
        return try {
            db.runTransaction { tx ->
                val gSnap = tx.get(giftRef)
                if (gSnap.getString("status") != "pending") {
                    throw IllegalStateException("Gift already claimed.")
                }
                val pSnap = tx.get(profileRef)
                val currentPoints = pSnap.getLong("points") ?: 0L
                val newBalance = currentPoints + gift.amount

                tx.update(
                    giftRef,
                    mapOf("status" to "claimed", "updatedAt" to FieldValue.serverTimestamp())
                )
                tx.update(
                    profileRef,
                    mapOf("points" to newBalance, "updatedAt" to FieldValue.serverTimestamp())
                )
                tx.set(
                    txRef,
                    mapOf(
                        "type" to "friend_gift_received",
                        "amount" to gift.amount,
                        "balanceAfter" to newBalance,
                        "description" to "Gift from @${gift.fromUsername}",
                        "counterpartyName" to gift.fromUsername.take(40),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(gift.amount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, giftRef.path)
            Result.failure(e)
        }
    }
}
