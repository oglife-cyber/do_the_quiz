package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ArenaColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = OnElectricCyanContainer,
    secondary = ChampionGold,
    onSecondary = OnChampionGold,
    secondaryContainer = ChampionGoldContainer,
    onSecondaryContainer = OnChampionGoldContainer,
    tertiary = NeonViolet,
    onTertiary = OnNeonViolet,
    tertiaryContainer = NeonVioletContainer,
    onTertiaryContainer = OnNeonVioletContainer,
    background = ArenaMidnight,
    onBackground = TextPrimary,
    surface = ArenaSurface,
    onSurface = TextPrimary,
    surfaceVariant = ArenaSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = ArenaSurface,
    surfaceContainer = ArenaSurfaceHigh,
    surfaceContainerHigh = ArenaSurfaceHighest,
    outline = OutlineSubtle,
    error = CoralAlert,
    onError = ArenaMidnight,
    errorContainer = CoralAlertContainer,
    onErrorContainer = TextPrimary
)

val ArenaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun QuizArenaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ArenaColorScheme,
        typography = Typography,
        shapes = ArenaShapes,
        content = content
    )
}
