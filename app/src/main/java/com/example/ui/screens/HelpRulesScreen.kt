package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.RankSystem
import com.example.ui.theme.ArenaSurfaceHigh
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.NeonViolet

private data class FaqEntry(val question: String, val answer: String)

@Composable
fun HelpRulesScreen() {
    val rules = listOf(
        "1. Scoring & Points" to "Every correct answer in Random Quiz, Live Matches, and Tournaments awards +10 Arena Points and +25 XP. Winning a match grants +40 bonus XP.",
        "2. Tournament Entry Fees & Prize Pools" to "When joining a tournament, the host's entry fee is automatically deducted from your wallet and added to the tournament's Prize Pool. Once the host finalizes the tournament, the top scorer claims the full pool.",
        "3. Fair Play & Anti-Cheat" to "Automated bots, exploiting multiple accounts for point farming, or harassing friends in direct messages will result in rank disqualification.",
        "4. Friend Messaging & Point Gifts" to "Direct messaging and point transfers are strictly restricted to mutually accepted friends. Enable your 4-digit Wallet Passcode in Profile for extra transfer protection."
    )

    val faqs = listOf(
        FaqEntry(
            "How does Random Quiz mode work?",
            "Random Quiz uses on-demand AI generation to build 5 fresh, unique multiple-choice questions on your chosen topic and difficulty."
        ),
        FaqEntry(
            "How do Tournament Invites work?",
            "When creating a tournament, the host can invite selected friends directly or broadcast an invite to all online players across QuizArena."
        ),
        FaqEntry(
            "What can I buy with my points?",
            "Open the Points Shop from the top gold pill or Profile to buy 50/50 Hints, Time Freezes, 2x XP Boosters, Profile Frames, and Arena Titles."
        ),
        FaqEntry(
            "How do I level up my Rank Tier?",
            "Every 120 XP levels you up. Higher levels unlock prestigious Rank Tiers from Rookie up to Arena Legend."
        )
    )

    var expandedFaqIndex by remember { mutableStateOf<Int?>(0) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Gavel, contentDescription = null, tint = ChampionGold)
                    Text("Rules, Ranks & Help", style = MaterialTheme.typography.headlineMedium, color = ChampionGold)
                }
            }

            item {
                Text("Official Rules & Regulations", style = MaterialTheme.typography.titleLarge, color = ElectricCyan)
            }

            itemsIndexed(rules) { _, (title, body) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldWin)
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = NeonViolet)
                    Text("Rank Tiers & Level Progression", style = MaterialTheme.typography.titleLarge, color = NeonViolet)
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ArenaSurfaceHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RankSystem.tiers.forEach { tier ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tier.name, style = MaterialTheme.typography.titleMedium, color = tier.accentColor, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (tier.maxLevel >= 500) "Lv.${tier.minLevel}+" else "Lv.${tier.minLevel}–${tier.maxLevel}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = ElectricCyan)
                    Text("Frequently Asked Questions", style = MaterialTheme.typography.titleLarge)
                }
            }

            itemsIndexed(faqs) { index, faq ->
                val isOpen = expandedFaqIndex == index
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedFaqIndex = if (isOpen) null else index },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(faq.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (isOpen) "Collapse" else "Expand"
                            )
                        }
                        AnimatedVisibility(visible = isOpen) {
                            Text(
                                text = faq.answer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
