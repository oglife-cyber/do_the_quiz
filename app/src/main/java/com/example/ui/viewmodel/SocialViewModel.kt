package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.extractFirestoreMessage
import com.example.data.SocialRepository
import com.example.model.ChatMessage
import com.example.model.FriendGift
import com.example.model.FriendRequest
import com.example.model.Friendship
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SocialViewModel(
    private val socialRepo: SocialRepository,
    val currentUserId: String
) : ViewModel() {

    val friendshipsState: StateFlow<UiState<List<Friendship>>> = socialRepo.observeFriendships(currentUserId)
        .map<List<Friendship>, UiState<List<Friendship>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            Log.w(TAG, "Failed observing friends", fsError ?: error)
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load friends")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val incomingRequestsState: StateFlow<UiState<List<FriendRequest>>> = socialRepo.observeIncomingRequests(currentUserId)
        .map<List<FriendRequest>, UiState<List<FriendRequest>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load requests")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val outgoingRequestsState: StateFlow<UiState<List<FriendRequest>>> = socialRepo.observeOutgoingRequests(currentUserId)
        .map<List<FriendRequest>, UiState<List<FriendRequest>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load outgoing requests")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val incomingGiftsState: StateFlow<UiState<List<FriendGift>>> = socialRepo.observeIncomingGifts(currentUserId)
        .map<List<FriendGift>, UiState<List<FriendGift>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load gifts")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _activeChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatMessages.asStateFlow()

    private val _activeFriendship = MutableStateFlow<Friendship?>(null)
    val activeFriendship: StateFlow<Friendship?> = _activeFriendship.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    private var chatJob: Job? = null

    fun clearFeedback() {
        _feedback.value = null
    }

    fun openChat(friendship: Friendship) {
        chatJob?.cancel()
        _activeFriendship.value = friendship
        chatJob = viewModelScope.launch {
            socialRepo.observeChatMessages(friendship.id, currentUserId)
                .catch { e ->
                    if (e is CancellationException) throw e
                    _feedback.value = extractFirestoreMessage(e, "Failed to open chat")
                }
                .collect { _activeChatMessages.value = it }
        }
    }

    fun closeChat() {
        chatJob?.cancel()
        _activeFriendship.value = null
        _activeChatMessages.value = emptyList()
    }

    fun sendFriendRequest(myUsername: String, toUid: String, toUsername: String) {
        viewModelScope.launch {
            val res = socialRepo.sendFriendRequest(myUsername, toUid, toUsername)
            res.onSuccess {
                _feedback.value = "Friend request sent to @$toUsername"
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Failed to send request"
            }
        }
    }

    fun respondToRequest(request: FriendRequest, accept: Boolean) {
        viewModelScope.launch {
            val res = socialRepo.respondToFriendRequest(request, accept)
            res.onSuccess {
                _feedback.value = if (accept) "You are now friends with @${request.fromUsername}!" else "Request declined."
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Failed to respond to request"
            }
        }
    }

    fun sendChatMessage(senderName: String, text: String) {
        val friendship = _activeFriendship.value ?: return
        viewModelScope.launch {
            val res = socialRepo.sendChatMessage(
                chatId = friendship.id,
                senderName = senderName,
                memberUids = friendship.memberUids,
                text = text
            )
            res.onFailure {
                _feedback.value = it.localizedMessage ?: "Failed to send message"
            }
        }
    }

    fun sendPointsGift(
        fromUsername: String,
        toUid: String,
        toUsername: String,
        amount: Int,
        note: String
    ) {
        viewModelScope.launch {
            val res = socialRepo.sendFriendGift(fromUsername, toUid, toUsername, amount, note)
            res.onSuccess {
                _feedback.value = "Sent $amount pts gift to @$toUsername!"
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Failed to send gift"
            }
        }
    }

    fun claimGift(gift: FriendGift) {
        viewModelScope.launch {
            val res = socialRepo.claimFriendGift(gift)
            res.onSuccess { amt ->
                _feedback.value = "Claimed +$amt pts from @${gift.fromUsername}!"
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Could not claim gift"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatJob?.cancel()
    }

    private companion object {
        const val TAG = "SocialVM"
    }
}
