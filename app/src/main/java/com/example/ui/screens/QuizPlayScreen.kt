package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.RankSystem
import com.example.model.MatchPlayer
import com.example.model.PublicProfile
import com.example.ui.components.ErrorBannerCard
import com.example.ui.theme.ArenaMidnight
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.NeonViolet
import com.example.ui.viewmodel.ActiveQuizMode
import com.example.ui.viewmodel.QuizSessionState

@Composable
fun QuizPlayScreen(
    sessionState: QuizSessionState,
    profile: PublicProfile?,
    roomPlayers: List<MatchPlayer>,
    onSubmitAnswer: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onUseHint: () -> Unit,
    onUseFreeze: () -> Unit,
    onRetryAiQuiz: (topic: String, difficulty: String) -> Unit,
    onExit: () -> Unit
) {
    BackHandler { onExit() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaMidnight)
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (sessionState) {
                is QuizSessionState.Generating -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NeonViolet, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(20.dp))
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Crafting your ${sessionState.topic} Random Quiz...",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Generating fresh trivia questions via AI",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is QuizSessionState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ErrorBannerCard(message = sessionState.message)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = onExit,
                                modifier = Modifier.testTag("quiz_error_back_button")
                            ) {
                                Text("Back to Arena")
                            }
                            Button(
                                onClick = { onRetryAiQuiz(sessionState.topic, sessionState.difficulty) },
                                modifier = Modifier.testTag("quiz_error_retry_button")
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is QuizSessionState.Playing -> {
                    val question = sessionState.questions.getOrNull(sessionState.currentIndex)
                    // Top header bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sessionState.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Question ${sessionState.currentIndex + 1} of ${sessionState.questions.size} • ${sessionState.topic}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onExit,
                            modifier = Modifier.testTag("quiz_exit_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Exit quiz")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (sessionState.currentIndex + 1).toFloat() / sessionState.questions.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = ElectricCyan,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )

                    Spacer(Modifier.height(12.dp))

                    // Live multiplayer opponent ticker
                    if (sessionState.mode == ActiveQuizMode.LIVE_ROOM && roomPlayers.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(roomPlayers, key = { it.userId }) { player ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(
                                        1.dp,
                                        if (player.userId == profile?.userId) ElectricCyan else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Text(
                                        text = "@${player.username}: ${player.score} pts",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    // Timer + Score + Power-Ups Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (sessionState.timeFrozen) ElectricCyan.copy(alpha = 0.2f)
                            else if (sessionState.secondsRemaining <= 5) CoralAlert.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (sessionState.timeFrozen) Icons.Filled.AcUnit else Icons.Filled.Timer,
                                    contentDescription = "Timer",
                                    tint = if (sessionState.timeFrozen) ElectricCyan else if (sessionState.secondsRemaining <= 5) CoralAlert else ChampionGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (sessionState.timeFrozen) "FROZEN" else "${sessionState.secondsRemaining}s",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Earned: ${sessionState.correctCount * RankSystem.POINTS_PER_CORRECT_ANSWER} pts",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampionGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Power-ups strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onUseHint,
                            enabled = sessionState.selectedOptionIndex == null &&
                                !sessionState.usedHintThisQuestion &&
                                (profile?.hintPowerUps ?: 0L) > 0L,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("use_hint_powerup_button")
                        ) {
                            Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("50/50 (${profile?.hintPowerUps ?: 0})")
                        }
                        OutlinedButton(
                            onClick = onUseFreeze,
                            enabled = sessionState.selectedOptionIndex == null &&
                                !sessionState.timeFrozen &&
                                (profile?.freezePowerUps ?: 0L) > 0L,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("use_freeze_powerup_button")
                        ) {
                            Icon(Icons.Filled.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Freeze (${profile?.freezePowerUps ?: 0})")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (question != null) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = question.question,
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }

                            question.options.forEachIndexed { idx, optionText ->
                                val isEliminated = idx in sessionState.eliminatedIndices
                                val isSelected = sessionState.selectedOptionIndex == idx
                                val isAnswered = sessionState.selectedOptionIndex != null
                                val isCorrect = idx == question.correctIndex

                                val targetBg = when {
                                    isAnswered && isCorrect -> EmeraldWin.copy(alpha = 0.22f)
                                    isAnswered && isSelected && !isCorrect -> CoralAlert.copy(alpha = 0.22f)
                                    isEliminated -> MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                                val animatedBg by animateColorAsState(targetValue = targetBg, label = "optBg")
                                val borderColor = when {
                                    isAnswered && isCorrect -> EmeraldWin
                                    isAnswered && isSelected && !isCorrect -> CoralAlert
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                }

                                Card(
                                    onClick = { if (!isEliminated && !isAnswered) onSubmitAnswer(idx) },
                                    enabled = !isEliminated && !isAnswered,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quiz_option_$idx"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = animatedBg,
                                        disabledContainerColor = animatedBg
                                    ),
                                    border = BorderStroke(1.5.dp, borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${('A' + idx)}.  $optionText",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isEliminated) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isAnswered && isCorrect) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "Correct", tint = EmeraldWin)
                                        }
                                    }
                                }
                            }

                            if (sessionState.selectedOptionIndex != null) {
                                val gotItRight = sessionState.selectedOptionIndex == question.correctIndex
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (gotItRight) EmeraldWin.copy(alpha = 0.16f) else CoralAlert.copy(alpha = 0.16f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = when {
                                            gotItRight -> "Correct! +${RankSystem.POINTS_PER_CORRECT_ANSWER} pts & +${RankSystem.XP_PER_CORRECT_ANSWER} XP"
                                            sessionState.selectedOptionIndex == -1 -> "Time's up! Correct answer: ${question.options.getOrElse(question.correctIndex) { "" }}"
                                            else -> "Wrong answer! Correct: ${question.options.getOrElse(question.correctIndex) { "" }}"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (gotItRight) EmeraldWin else CoralAlert,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }

                        if (sessionState.selectedOptionIndex != null) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = onNextQuestion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("quiz_next_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = if (sessionState.currentIndex + 1 >= sessionState.questions.size) "Finish & Claim Rewards" else "Next Question",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.size(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }

                is QuizSessionState.Completed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = if (sessionState.won) ChampionGold else ElectricCyan,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (sessionState.won) "Victory!" else "Challenge Complete!",
                            style = MaterialTheme.typography.displaySmall,
                            color = if (sessionState.won) ChampionGold else ElectricCyan
                        )
                        Text(
                            text = sessionState.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = BorderStroke(1.dp, ChampionGold.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SummaryStatRow("Score", "${sessionState.correctCount} / ${sessionState.totalQuestions} correct")
                                SummaryStatRow("Points Earned", "+${sessionState.pointsEarned} pts", ChampionGold)
                                SummaryStatRow("XP Gained", "+${sessionState.xpEarned} XP", NeonViolet)
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onExit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("quiz_done_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ArenaMidnight),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Return to Arena", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                QuizSessionState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun SummaryStatRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
    }
}
