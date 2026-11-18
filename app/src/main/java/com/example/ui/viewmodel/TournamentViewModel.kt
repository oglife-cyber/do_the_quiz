package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.extractFirestoreMessage
import com.example.data.AiQuizRepository
import com.example.data.TournamentRepository
import com.example.model.QuizQuestion
import com.example.model.Tournament
import com.example.model.TournamentInvite
import com.example.model.TournamentParticipant
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

class TournamentViewModel(
    private val tournamentRepo: TournamentRepository,
    private val aiRepo: AiQuizRepository,
    val currentUserId: String
) : ViewModel() {

    val tournamentsState: StateFlow<UiState<List<Tournament>>> = tournamentRepo.observeTournaments()
        .map<List<Tournament>, UiState<List<Tournament>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing tournaments", fsError ?: error)
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load tournaments")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val invitesState: StateFlow<UiState<List<TournamentInvite>>> = tournamentRepo.observeMyInvites(currentUserId)
        .map<List<TournamentInvite>, UiState<List<TournamentInvite>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load invites")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _selectedTournament = MutableStateFlow<Tournament?>(null)
    val selectedTournament: StateFlow<Tournament?> = _selectedTournament.asStateFlow()

    private val _participants = MutableStateFlow<List<TournamentParticipant>>(emptyList())
    val participants: StateFlow<List<TournamentParticipant>> = _participants.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _aiDraftBusy = MutableStateFlow(false)
    val aiDraftBusy: StateFlow<Boolean> = _aiDraftBusy.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    private var detailJob: Job? = null
    private var participantsJob: Job? = null

    fun clearFeedback() {
        _feedback.value = null
    }

    fun selectTournament(tournamentId: String) {
        detailJob?.cancel()
        participantsJob?.cancel()
        detailJob = viewModelScope.launch {
            tournamentRepo.observeTournament(tournamentId)
                .catch { e ->
                    if (e is CancellationException) throw e
                    _feedback.value = extractFirestoreMessage(e, "Failed to load tournament")
                }
                .collect { _selectedTournament.value = it }
        }
        participantsJob = viewModelScope.launch {
            tournamentRepo.observeParticipants(tournamentId)
                .catch { e ->
                    if (e is CancellationException) throw e
                }
                .collect { _participants.value = it }
        }
    }

    fun clearSelectedTournament() {
        detailJob?.cancel()
        participantsJob?.cancel()
        _selectedTournament.value = null
        _participants.value = emptyList()
    }

    fun generateAiQuestionDraft(topic: String, onDraftReady: (QuizQuestion) -> Unit) {
        viewModelScope.launch {
            _aiDraftBusy.value = true
            val res = aiRepo.generateSingleQuestion(topic.ifBlank { "General Knowledge" })
            _aiDraftBusy.value = false
            res.onSuccess { onDraftReady(it) }
                .onFailure { _feedback.value = it.localizedMessage ?: "Could not generate question" }
        }
    }

    fun createTournament(
        hostUsername: String,
        title: String,
        category: String,
        entryFee: Int,
        maxPlayers: Int,
        inviteScope: String,
        invitedFriendUids: List<String>,
        questions: List<QuizQuestion>,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = tournamentRepo.createTournament(
                hostUsername = hostUsername,
                title = title,
                category = category,
                entryFee = entryFee,
                maxPlayers = maxPlayers,
                inviteScope = inviteScope,
                invitedFriendUids = invitedFriendUids,
                questions = questions
            )
            _busy.value = false
            res.onSuccess { id ->
                _feedback.value = if (inviteScope == "online_all") {
                    "Tournament created & broadcast to all online players!"
                } else {
                    "Tournament created & invites sent!"
                }
                onCreated(id)
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Failed to create tournament"
            }
        }
    }

    fun joinTournament(tournamentId: String, username: String, onJoined: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            val res = tournamentRepo.joinTournament(tournamentId, username)
            _busy.value = false
            res.onSuccess {
                _feedback.value = "Joined tournament! Entry fee deducted."
                onJoined()
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Could not join tournament"
            }
        }
    }

    fun startTournament(tournamentId: String) {
        viewModelScope.launch {
            val res = tournamentRepo.setTournamentLive(tournamentId)
            res.onFailure { _feedback.value = it.localizedMessage ?: "Could not start tournament" }
        }
    }

    fun finalizeTournament(tournamentId: String) {
        val topParticipant = _participants.value.firstOrNull()
        if (topParticipant == null) {
            _feedback.value = "No participants yet."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            val res = tournamentRepo.completeTournament(
                tournamentId = tournamentId,
                winnerUid = topParticipant.userId,
                winnerName = topParticipant.username
            )
            _busy.value = false
            res.onSuccess {
                _feedback.value = "Tournament completed! Winner: @${topParticipant.username}"
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Could not finalize tournament"
            }
        }
    }

    fun claimPrize(tournamentId: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = tournamentRepo.claimTournamentPrize(tournamentId)
            _busy.value = false
            res.onSuccess { amount ->
                _feedback.value = "Claimed $amount prize points!"
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Failed to claim prize"
            }
        }
    }

    private companion object {
        const val TAG = "TournamentVM"
    }
}
