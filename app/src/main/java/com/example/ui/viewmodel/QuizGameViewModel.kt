package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.RankSystem
import com.example.core.extractFirestoreMessage
import com.example.data.AiQuizRepository
import com.example.data.ArenaMatchRepository
import com.example.data.ProfileRepository
import com.example.data.TournamentRepository
import com.example.model.MatchPlayer
import com.example.model.MatchRoom
import com.example.model.QuizQuestion
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ActiveQuizMode {
    RANDOM_AI, LIVE_ROOM, TOURNAMENT
}

sealed interface QuizSessionState {
    data object Idle : QuizSessionState
    data class Generating(val topic: String) : QuizSessionState
    data class Error(val message: String, val topic: String, val difficulty: String) : QuizSessionState
    data class Playing(
        val mode: ActiveQuizMode,
        val sessionId: String,
        val title: String,
        val topic: String,
        val questions: List<QuizQuestion>,
        val currentIndex: Int = 0,
        val selectedOptionIndex: Int? = null,
        val eliminatedIndices: Set<Int> = emptySet(),
        val correctCount: Int = 0,
        val secondsRemaining: Int = 20,
        val timeFrozen: Boolean = false,
        val doubleXpActive: Boolean = false,
        val usedHintThisQuestion: Boolean = false
    ) : QuizSessionState
    data class Completed(
        val mode: ActiveQuizMode,
        val title: String,
        val topic: String,
        val correctCount: Int,
        val totalQuestions: Int,
        val pointsEarned: Int,
        val xpEarned: Int,
        val won: Boolean
    ) : QuizSessionState
}

class QuizGameViewModel(
    private val aiRepo: AiQuizRepository,
    private val matchRepo: ArenaMatchRepository,
    private val profileRepo: ProfileRepository,
    private val tournamentRepo: TournamentRepository,
    val currentUserId: String
) : ViewModel() {

    val publicRoomsState: StateFlow<UiState<List<MatchRoom>>> = matchRepo.observePublicRooms()
        .map<List<MatchRoom>, UiState<List<MatchRoom>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing rooms", fsError ?: error)
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load live rooms")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _sessionState = MutableStateFlow<QuizSessionState>(QuizSessionState.Idle)
    val sessionState: StateFlow<QuizSessionState> = _sessionState.asStateFlow()

    private val _roomPlayers = MutableStateFlow<List<MatchPlayer>>(emptyList())
    val roomPlayers: StateFlow<List<MatchPlayer>> = _roomPlayers.asStateFlow()

    private val _activeRoom = MutableStateFlow<MatchRoom?>(null)
    val activeRoom: StateFlow<MatchRoom?> = _activeRoom.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private var timerJob: Job? = null
    private var roomObserverJob: Job? = null
    private var playersObserverJob: Job? = null

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun startRandomAiQuiz(topic: String, difficulty: String, useDoubleXp: Boolean = false) {
        timerJob?.cancel()
        viewModelScope.launch {
            _sessionState.value = QuizSessionState.Generating(topic)
            val result = aiRepo.generateRandomQuiz(topic = topic, difficulty = difficulty, questionCount = 5)
            result.onSuccess { generated ->
                _sessionState.value = QuizSessionState.Playing(
                    mode = ActiveQuizMode.RANDOM_AI,
                    sessionId = "ai_${System.currentTimeMillis()}",
                    title = generated.title,
                    topic = generated.topic,
                    questions = generated.questions,
                    doubleXpActive = useDoubleXp
                )
                startQuestionTimer()
            }.onFailure { err ->
                _sessionState.value = QuizSessionState.Error(
                    message = err.localizedMessage ?: "Failed to generate Random Quiz.",
                    topic = topic,
                    difficulty = difficulty
                )
            }
        }
    }

    fun createAndJoinLiveRoom(hostUsername: String, title: String, topic: String, maxPlayers: Int) {
        viewModelScope.launch {
            _sessionState.value = QuizSessionState.Generating(topic)
            val aiRes = aiRepo.generateRandomQuiz(topic = topic, difficulty = "Medium", questionCount = 5)
            aiRes.onFailure { err ->
                _sessionState.value = QuizSessionState.Idle
                _actionMessage.value = err.localizedMessage ?: "Could not generate questions for room."
                return@launch
            }
            val generated = aiRes.getOrThrow()
            val roomRes = matchRepo.createMatchRoom(
                hostUsername = hostUsername,
                title = title.ifBlank { generated.title },
                topic = generated.topic,
                maxPlayers = maxPlayers,
                questions = generated.questions
            )
            roomRes.onSuccess { roomId ->
                _sessionState.value = QuizSessionState.Idle
                watchRoomLobby(roomId)
            }.onFailure { err ->
                _sessionState.value = QuizSessionState.Idle
                _actionMessage.value = err.localizedMessage ?: "Failed to create room."
            }
        }
    }

    fun joinLiveRoom(roomId: String, username: String) {
        viewModelScope.launch {
            val res = matchRepo.joinMatchRoom(roomId, username)
            res.onSuccess {
                watchRoomLobby(roomId)
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Could not join room."
            }
        }
    }

    fun watchRoomLobby(roomId: String) {
        roomObserverJob?.cancel()
        playersObserverJob?.cancel()
        roomObserverJob = viewModelScope.launch {
            matchRepo.observeRoom(roomId)
                .catch { e ->
                    if (e is CancellationException) throw e
                    _actionMessage.value = extractFirestoreMessage(e, "Room connection error")
                }
                .collect { room ->
                    _activeRoom.value = room
                    if (room != null && room.status == "active" && _sessionState.value is QuizSessionState.Idle) {
                        startLiveRoomQuestions(room)
                    }
                }
        }
        playersObserverJob = viewModelScope.launch {
            matchRepo.observeRoomPlayers(roomId)
                .catch { e ->
                    if (e is CancellationException) throw e
                }
                .collect { players ->
                    _roomPlayers.value = players
                }
        }
    }

    fun startLiveRoomByHost(room: MatchRoom) {
        viewModelScope.launch {
            matchRepo.setRoomStatus(room.id, "active")
            startLiveRoomQuestions(room)
        }
    }

    fun leaveRoomLobby() {
        roomObserverJob?.cancel()
        playersObserverJob?.cancel()
        _activeRoom.value = null
        _roomPlayers.value = emptyList()
    }

    private fun startLiveRoomQuestions(room: MatchRoom) {
        timerJob?.cancel()
        _sessionState.value = QuizSessionState.Playing(
            mode = ActiveQuizMode.LIVE_ROOM,
            sessionId = room.id,
            title = room.title,
            topic = room.topic,
            questions = room.questions
        )
        startQuestionTimer()
    }

    fun startTournamentQuiz(
        tournamentId: String,
        title: String,
        category: String,
        questions: List<QuizQuestion>
    ) {
        timerJob?.cancel()
        _sessionState.value = QuizSessionState.Playing(
            mode = ActiveQuizMode.TOURNAMENT,
            sessionId = tournamentId,
            title = title,
            topic = category,
            questions = questions
        )
        startQuestionTimer()
    }

    private fun startQuestionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _sessionState.value
                if (current !is QuizSessionState.Playing || current.selectedOptionIndex != null) {
                    break
                }
                if (current.timeFrozen) continue
                val next = current.secondsRemaining - 1
                if (next <= 0) {
                    submitAnswer(-1) // timed out
                    break
                } else {
                    _sessionState.value = current.copy(secondsRemaining = next)
                }
            }
        }
    }

    fun useHintPowerUp() {
        val current = _sessionState.value as? QuizSessionState.Playing ?: return
        if (current.selectedOptionIndex != null || current.usedHintThisQuestion) return
        val q = current.questions.getOrNull(current.currentIndex) ?: return
        viewModelScope.launch {
            val res = profileRepo.consumePowerUp("hint")
            res.onSuccess {
                val wrongIndices = q.options.indices.filter { it != q.correctIndex }.shuffled().take(2).toSet()
                val updated = _sessionState.value as? QuizSessionState.Playing ?: return@onSuccess
                _sessionState.value = updated.copy(
                    eliminatedIndices = wrongIndices,
                    usedHintThisQuestion = true
                )
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "No 50/50 hints left!"
            }
        }
    }

    fun useFreezePowerUp() {
        val current = _sessionState.value as? QuizSessionState.Playing ?: return
        if (current.selectedOptionIndex != null || current.timeFrozen) return
        viewModelScope.launch {
            val res = profileRepo.consumePowerUp("freeze")
            res.onSuccess {
                val updated = _sessionState.value as? QuizSessionState.Playing ?: return@onSuccess
                _sessionState.value = updated.copy(
                    timeFrozen = true,
                    secondsRemaining = updated.secondsRemaining + 10
                )
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "No Time Freezes left!"
            }
        }
    }

    fun submitAnswer(optionIndex: Int) {
        val current = _sessionState.value as? QuizSessionState.Playing ?: return
        if (current.selectedOptionIndex != null) return
        timerJob?.cancel()

        val q = current.questions.getOrNull(current.currentIndex) ?: return
        val isCorrect = optionIndex == q.correctIndex
        val newCorrect = current.correctCount + if (isCorrect) 1 else 0

        _sessionState.value = current.copy(
            selectedOptionIndex = optionIndex,
            correctCount = newCorrect
        )

        if (current.mode == ActiveQuizMode.LIVE_ROOM) {
            viewModelScope.launch {
                matchRepo.updateMyProgress(
                    roomId = current.sessionId,
                    score = newCorrect * RankSystem.POINTS_PER_CORRECT_ANSWER,
                    correctCount = newCorrect,
                    answeredCount = current.currentIndex + 1,
                    finished = current.currentIndex + 1 >= current.questions.size
                )
            }
        }
    }

    fun advanceToNextQuestion() {
        val current = _sessionState.value as? QuizSessionState.Playing ?: return
        val nextIndex = current.currentIndex + 1
        if (nextIndex < current.questions.size) {
            _sessionState.value = current.copy(
                currentIndex = nextIndex,
                selectedOptionIndex = null,
                eliminatedIndices = emptySet(),
                secondsRemaining = 20,
                timeFrozen = false,
                usedHintThisQuestion = false
            )
            startQuestionTimer()
        } else {
            finishQuizSession(current)
        }
    }

    private fun finishQuizSession(finalPlay: QuizSessionState.Playing) {
        timerJob?.cancel()
        val total = finalPlay.questions.size
        val correct = finalPlay.correctCount
        val won = when (finalPlay.mode) {
            ActiveQuizMode.RANDOM_AI -> correct * 2 >= total
            ActiveQuizMode.LIVE_ROOM -> {
                val topScore = _roomPlayers.value.maxOfOrNull { it.score } ?: 0
                (correct * RankSystem.POINTS_PER_CORRECT_ANSWER) >= topScore && correct > 0
            }
            ActiveQuizMode.TOURNAMENT -> correct * 2 >= total
        }
        val pointsEarned = correct * RankSystem.POINTS_PER_CORRECT_ANSWER
        val baseXp = (correct * RankSystem.XP_PER_CORRECT_ANSWER) + if (won) RankSystem.XP_WIN_BONUS else 0
        val xpEarned = if (finalPlay.doubleXpActive) baseXp * 2 else baseXp

        _sessionState.value = QuizSessionState.Completed(
            mode = finalPlay.mode,
            title = finalPlay.title,
            topic = finalPlay.topic,
            correctCount = correct,
            totalQuestions = total,
            pointsEarned = pointsEarned,
            xpEarned = xpEarned,
            won = won
        )

        viewModelScope.launch {
            val modeStr = when (finalPlay.mode) {
                ActiveQuizMode.RANDOM_AI -> "random_ai"
                ActiveQuizMode.LIVE_ROOM -> "live_match"
                ActiveQuizMode.TOURNAMENT -> "tournament"
            }
            profileRepo.recordQuizCompletion(
                mode = modeStr,
                title = finalPlay.title,
                topic = finalPlay.topic,
                correctCount = correct,
                totalQuestions = total,
                won = won,
                usedDoubleXp = finalPlay.doubleXpActive
            )
            if (finalPlay.mode == ActiveQuizMode.TOURNAMENT) {
                tournamentRepo.submitTournamentScore(
                    tournamentId = finalPlay.sessionId,
                    score = pointsEarned,
                    correctCount = correct
                )
            }
            if (finalPlay.mode == ActiveQuizMode.LIVE_ROOM && _activeRoom.value?.hostId == currentUserId) {
                matchRepo.setRoomStatus(finalPlay.sessionId, "completed")
            }
        }
    }

    fun exitQuizSession() {
        timerJob?.cancel()
        _sessionState.value = QuizSessionState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        roomObserverJob?.cancel()
        playersObserverJob?.cancel()
    }

    private companion object {
        const val TAG = "QuizGameVM"
    }
}
