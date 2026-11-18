package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val discoverable: Boolean = true,
    val online: Boolean = true,
    val points: Long = 0L,
    val xp: Long = 0L,
    val level: Int = 1,
    val matchesPlayed: Long = 0L,
    val wins: Long = 0L,
    val correctAnswers: Long = 0L,
    val totalAnswered: Long = 0L,
    val winStreak: Long = 0L,
    val tournamentsWon: Long = 0L,
    val equippedFrame: String = "starter",
    val equippedTitle: String = "Rookie",
    val ownedItems: List<String> = listOf("starter"),
    val hintPowerUps: Long = 1L,
    val freezePowerUps: Long = 0L,
    val doubleXpPowerUps: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val passcodeEnabled: Boolean = false,
    val passcodeHash: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchHistoryEntry(
    @DocumentId val id: String = "",
    val mode: String = "random_ai",
    val title: String = "",
    val topic: String = "",
    val correctCount: Int = 0,
    val totalQuestions: Int = 0,
    val pointsEarned: Int = 0,
    val xpEarned: Int = 0,
    val won: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PointTransaction(
    @DocumentId val id: String = "",
    val type: String = "quiz_reward",
    val amount: Int = 0,
    val balanceAfter: Long = 0L,
    val description: String = "",
    val counterpartyName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "question" to question,
        "options" to options,
        "correctIndex" to correctIndex
    )
}

@Keep
@IgnoreExtraProperties
data class MatchRoom(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val title: String = "",
    val topic: String = "",
    val status: String = "waiting",
    @get:PropertyName("isPublic") @PropertyName("isPublic")
    val isPublic: Boolean = true,
    val maxPlayers: Int = 4,
    val playerCount: Int = 1,
    val memberUids: List<String> = emptyList(),
    val questions: List<QuizQuestion> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchPlayer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val score: Int = 0,
    val correctCount: Int = 0,
    val answeredCount: Int = 0,
    val finished: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val title: String = "",
    val category: String = "",
    val entryFee: Int = 0,
    val prizePool: Int = 0,
    val maxPlayers: Int = 16,
    val participantCount: Int = 0,
    val inviteScope: String = "online_all",
    val status: String = "open",
    @get:PropertyName("isPublic") @PropertyName("isPublic")
    val isPublic: Boolean = true,
    val memberUids: List<String> = emptyList(),
    val questions: List<QuizQuestion> = emptyList(),
    val winnerUid: String = "",
    val winnerName: String = "",
    val prizeClaimed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TournamentParticipant(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val score: Int = 0,
    val correctCount: Int = 0,
    val finished: Boolean = false,
    @ServerTimestamp val joinedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TournamentInvite(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val entryFee: Int = 0,
    val targetUid: String = "",
    val inviteType: String = "broadcast_online",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val fromUsername: String = "",
    val toUid: String = "",
    val toUsername: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val memberUids: List<String> = emptyList(),
    val user1Uid: String = "",
    val user1Name: String = "",
    val user2Uid: String = "",
    val user2Name: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun partnerUid(myUid: String): String = if (user1Uid == myUid) user2Uid else user1Uid
    fun partnerName(myUid: String): String = if (user1Uid == myUid) user2Name else user1Name
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendGift(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val fromUsername: String = "",
    val toUid: String = "",
    val toUsername: String = "",
    val friendshipId: String = "",
    val amount: Int = 0,
    val note: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
