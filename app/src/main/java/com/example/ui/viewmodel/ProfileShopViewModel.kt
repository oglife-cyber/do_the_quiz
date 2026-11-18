package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ShopItem
import com.example.core.extractFirestoreMessage
import com.example.data.ProfileRepository
import com.example.model.MatchHistoryEntry
import com.example.model.PointTransaction
import com.example.model.PrivateAccount
import com.example.model.PublicProfile
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileShopViewModel(
    private val profileRepo: ProfileRepository,
    val currentUserId: String
) : ViewModel() {

    val myProfileState: StateFlow<UiState<PublicProfile?>> = profileRepo.observeMyProfile(currentUserId)
        .map<PublicProfile?, UiState<PublicProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            Log.w(TAG, "Failed observing profile", fsError ?: error)
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load profile")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val privateAccountState: StateFlow<UiState<PrivateAccount?>> = profileRepo.observePrivateAccount(currentUserId)
        .map<PrivateAccount?, UiState<PrivateAccount?>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            Log.w(TAG, "Failed observing private account", fsError ?: error)
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load account settings")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val leaderboardState: StateFlow<UiState<List<PublicProfile>>> = profileRepo.observeGlobalLeaderboard()
        .map<List<PublicProfile>, UiState<List<PublicProfile>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            Log.w(TAG, "Failed observing leaderboard", fsError ?: error)
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load leaderboard")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val matchHistoryState: StateFlow<UiState<List<MatchHistoryEntry>>> = profileRepo.observeMatchHistory(currentUserId)
        .map<List<MatchHistoryEntry>, UiState<List<MatchHistoryEntry>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load match history")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val transactionsState: StateFlow<UiState<List<PointTransaction>>> = profileRepo.observeTransactions(currentUserId)
        .map<List<PointTransaction>, UiState<List<PointTransaction>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load transactions")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    fun clearFeedback() {
        _feedback.value = null
    }

    fun completeOnboarding(username: String, displayName: String, optionalPin: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = profileRepo.completeSignUpOnboarding(username, displayName, optionalPin)
            _busy.value = false
            res.onFailure { _feedback.value = it.localizedMessage ?: "Failed to create profile" }
        }
    }

    fun toggleOnlineStatus(online: Boolean) {
        viewModelScope.launch {
            val res = profileRepo.setOnlineStatus(online)
            res.onFailure { _feedback.value = it.localizedMessage ?: "Failed to update status" }
        }
    }

    fun updateWalletPasscode(enabled: Boolean, pin: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = profileRepo.updateWalletPasscode(enabled, pin)
            _busy.value = false
            res.onSuccess {
                _feedback.value = if (enabled) "Wallet passcode enabled" else "Wallet passcode disabled"
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Failed to update passcode"
            }
        }
    }

    fun purchaseItem(item: ShopItem) {
        viewModelScope.launch {
            _busy.value = true
            val res = profileRepo.purchaseShopItem(item)
            _busy.value = false
            res.onSuccess {
                _feedback.value = "Purchased ${item.name} for ${item.price} pts!"
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Purchase failed"
            }
        }
    }

    fun equipItem(item: ShopItem) {
        viewModelScope.launch {
            _busy.value = true
            val res = profileRepo.equipCosmetic(item)
            _busy.value = false
            res.onSuccess {
                _feedback.value = "Equipped ${item.name}"
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Equip failed"
            }
        }
    }

    private companion object {
        const val TAG = "ProfileShopVM"
    }
}
