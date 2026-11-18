package com.example.core

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextSecondary

data class RankTier(
    val name: String,
    val minLevel: Int,
    val maxLevel: Int,
    val accentColor: Color,
    val perkDescription: String
)

enum class ShopCategory {
    FRAME, TITLE, POWERUP
}

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val category: ShopCategory,
    val badgeText: String
)

object RankSystem {
    const val POINTS_PER_CORRECT_ANSWER = 10
    const val XP_PER_CORRECT_ANSWER = 25
    const val XP_WIN_BONUS = 40
    const val XP_PER_LEVEL = 120

    val tiers = listOf(
        RankTier("Rookie", 1, 2, TextSecondary, "Starter arena contender"),
        RankTier("Challenger", 3, 5, EmeraldWin, "Proven trivia competitor"),
        RankTier("Tactician", 6, 9, ElectricCyan, "Sharp multi-category strategist"),
        RankTier("Master", 10, 14, NeonViolet, "Elite tournament finalist"),
        RankTier("Grandmaster", 15, 24, ChampionGold, "Top-tier arena champion"),
        RankTier("Arena Legend", 25, 500, ChampionGold, "Hall of Fame trivia icon")
    )

    fun levelFromXp(xp: Long): Int = ((xp.coerceAtLeast(0L) / XP_PER_LEVEL) + 1L).toInt().coerceIn(1, 500)

    fun tierForLevel(level: Int): RankTier =
        tiers.firstOrNull { level in it.minLevel..it.maxLevel } ?: tiers.last()

    fun xpIntoCurrentLevel(xp: Long): Int = (xp.coerceAtLeast(0L) % XP_PER_LEVEL).toInt()

    fun levelProgressFraction(xp: Long): Float =
        (xpIntoCurrentLevel(xp).toFloat() / XP_PER_LEVEL.toFloat()).coerceIn(0f, 1f)

    val shopCatalog = listOf(
        // Power-ups (consumable)
        ShopItem(
            id = "powerup_hint",
            name = "50/50 Hint Pack",
            description = "Eliminates two wrong choices during any quiz question.",
            price = 20,
            category = ShopCategory.POWERUP,
            badgeText = "50/50"
        ),
        ShopItem(
            id = "powerup_freeze",
            name = "Time Freeze",
            description = "Pauses the question countdown clock for 10 extra seconds.",
            price = 25,
            category = ShopCategory.POWERUP,
            badgeText = "FREEZE"
        ),
        ShopItem(
            id = "powerup_double_xp",
            name = "2x XP Booster",
            description = "Doubles XP earned in your next quiz to rank up faster.",
            price = 30,
            category = ShopCategory.POWERUP,
            badgeText = "2X XP"
        ),
        // Profile Frames (permanent cosmetics)
        ShopItem(
            id = "frame_cyan_pulse",
            name = "Cyan Pulse Frame",
            description = "Neon electric cyan halo around your arena avatar.",
            price = 40,
            category = ShopCategory.FRAME,
            badgeText = "FRAME"
        ),
        ShopItem(
            id = "frame_gold_laurel",
            name = "Champion Gold Frame",
            description = "Prestigious gold tournament border on leaderboards.",
            price = 80,
            category = ShopCategory.FRAME,
            badgeText = "GOLD"
        ),
        ShopItem(
            id = "frame_quantum_violet",
            name = "Quantum Violet Aura",
            description = "Mythic ultraviolet energy frame for top contenders.",
            price = 120,
            category = ShopCategory.FRAME,
            badgeText = "MYTHIC"
        ),
        // Arena Titles (permanent cosmetics)
        ShopItem(
            id = "title_blitz_mind",
            name = "Blitz Mind",
            description = "Display 'Blitz Mind' title below your @username.",
            price = 30,
            category = ShopCategory.TITLE,
            badgeText = "TITLE"
        ),
        ShopItem(
            id = "title_trivia_titan",
            name = "Trivia Titan",
            description = "Command respect with the 'Trivia Titan' arena title.",
            price = 60,
            category = ShopCategory.TITLE,
            badgeText = "TITLE"
        ),
        ShopItem(
            id = "title_grand_oracle",
            name = "Grand Oracle",
            description = "Legendary title for masters of Random AI Quizzes.",
            price = 100,
            category = ShopCategory.TITLE,
            badgeText = "LEGEND"
        )
    )

    fun frameName(frameId: String): String = when (frameId) {
        "frame_cyan_pulse" -> "Cyan Pulse"
        "frame_gold_laurel" -> "Champion Gold"
        "frame_quantum_violet" -> "Quantum Violet"
        else -> "Starter Frame"
    }

    fun frameColor(frameId: String): Color = when (frameId) {
        "frame_cyan_pulse" -> ElectricCyan
        "frame_gold_laurel" -> ChampionGold
        "frame_quantum_violet" -> NeonViolet
        else -> OutlineSubtleColor
    }

    private val OutlineSubtleColor = Color(0xFF334155)
}
