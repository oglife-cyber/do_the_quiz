package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.RankSystem
import com.example.core.sha256Hex
import com.example.ui.theme.ArenaMidnight
import com.example.ui.theme.ArenaSurfaceHigh
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.ChampionGoldContainer
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.OnChampionGoldContainer

@Composable
fun PlayerAvatar(
    username: String,
    frameId: String,
    online: Boolean,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val frameColor = RankSystem.frameColor(frameId)
    val initial = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                )
                .border(width = 2.5.dp, color = frameColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        if (online) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(EmeraldWin)
                    .border(2.dp, ArenaMidnight, CircleShape)
            )
        }
    }
}

@Composable
fun RankTierBadge(level: Int, modifier: Modifier = Modifier) {
    val tier = RankSystem.tierForLevel(level)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = tier.accentColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, tier.accentColor.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = tier.accentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Lv.$level ${tier.name}",
                style = MaterialTheme.typography.labelMedium,
                color = tier.accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PointsPill(
    points: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .testTag("top_points_shop_pill"),
        shape = RoundedCornerShape(999.dp),
        color = ChampionGoldContainer,
        border = BorderStroke(1.dp, ChampionGold.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MonetizationOn,
                contentDescription = "Arena points",
                tint = ChampionGold,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "$points pts",
                style = MaterialTheme.typography.labelLarge,
                color = OnChampionGoldContainer,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Filled.Storefront,
                contentDescription = "Open Shop",
                tint = ChampionGold,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun LoadingCenter(label: String = "Loading...", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorBannerCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun PasscodeVerifyDialog(
    expectedHash: String,
    actionLabel: String,
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArenaSurfaceHigh,
        icon = {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = ChampionGold)
        },
        title = {
            Text("Enter Wallet Passcode", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Confirm your 4-digit passcode to $actionLabel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
                    label = { Text("4-Digit Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_passcode_input")
                )
                if (pinError != null) {
                    Text(
                        text = pinError!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sha256Hex(pinInput) == expectedHash) {
                        onVerified()
                    } else {
                        pinError = "Incorrect passcode. Try again."
                    }
                },
                modifier = Modifier.testTag("verify_passcode_confirm")
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
