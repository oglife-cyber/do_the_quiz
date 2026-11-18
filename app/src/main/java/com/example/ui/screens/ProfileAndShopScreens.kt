package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.RankSystem
import com.example.core.ShopCategory
import com.example.core.ShopItem
import com.example.model.MatchHistoryEntry
import com.example.model.PointTransaction
import com.example.model.PrivateAccount
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingCenter
import com.example.ui.components.PasscodeVerifyDialog
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.RankTierBadge
import com.example.ui.theme.ArenaMidnight
import com.example.ui.theme.ArenaSurfaceHigh
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.NeonViolet

@Composable
fun ProfileScreen(
    myProfile: PublicProfile,
    privateAccount: PrivateAccount?,
    matchHistoryState: UiState<List<MatchHistoryEntry>>,
    onToggleOnline: (Boolean) -> Unit,
    onUpdatePasscode: (enabled: Boolean, pin: String) -> Unit,
    onOpenShop: () -> Unit,
    onOpenWallet: () -> Unit,
    onSignOut: () -> Unit
) {
    var showPasscodeDialog by remember { mutableStateOf(false) }

    val tier = RankSystem.tierForLevel(myProfile.level)
    val progress = RankSystem.levelProgressFraction(myProfile.xp)
    val xpInto = RankSystem.xpIntoCurrentLevel(myProfile.xp)
    val accuracy = if (myProfile.totalAnswered > 0L) {
        ((myProfile.correctAnswers * 100L) / myProfile.totalAnswered).toInt()
    } else 0

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, tier.accentColor.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            PlayerAvatar(
                                username = myProfile.username,
                                frameId = myProfile.equippedFrame,
                                online = myProfile.online,
                                size = 64.dp
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(myProfile.displayName, style = MaterialTheme.typography.headlineSmall)
                                Text("@${myProfile.username} • ${myProfile.equippedTitle}", style = MaterialTheme.typography.bodyMedium, color = ElectricCyan)
                                RankTierBadge(level = myProfile.level)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Level ${myProfile.level} Progress", style = MaterialTheme.typography.labelLarge)
                                Text("$xpInto / ${RankSystem.XP_PER_LEVEL} XP", style = MaterialTheme.typography.labelLarge, color = NeonViolet)
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = tier.accentColor,
                                trackColor = ArenaSurfaceHigh
                            )
                            Text(tier.perkDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Shop & Wallet Actions
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onOpenShop,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("profile_open_shop_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionGold, contentColor = ArenaMidnight)
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Points Shop", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenWallet,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("profile_open_wallet_button")
                    ) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Transactions")
                    }
                }
            }

            // Stats Grid
            item {
                Text("Career Stats", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Points", "${myProfile.points}", ChampionGold, Modifier.weight(1f))
                        StatTile("Matches", "${myProfile.matchesPlayed}", ElectricCyan, Modifier.weight(1f))
                        StatTile("Wins", "${myProfile.wins}", EmeraldWin, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Accuracy", "$accuracy%", NeonViolet, Modifier.weight(1f))
                        StatTile("Win Streak", "${myProfile.winStreak}", ChampionGold, Modifier.weight(1f))
                        StatTile("Tourney Wins", "${myProfile.tournamentsWon}", EmeraldWin, Modifier.weight(1f))
                    }
                }
            }

            // Account & Status Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Appear Online", style = MaterialTheme.typography.titleMedium)
                                Text("Receive live tournament broadcast invites", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = myProfile.online,
                                onCheckedChange = onToggleOnline,
                                modifier = Modifier.testTag("online_status_switch")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Wallet Passcode Lock", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (privateAccount?.passcodeEnabled == true) "4-digit PIN active for gifts & purchases" else "No PIN set",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { showPasscodeDialog = true },
                                modifier = Modifier.testTag("manage_passcode_button")
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(if (privateAccount?.passcodeEnabled == true) "Update" else "Set PIN")
                            }
                        }
                    }
                }
            }

            // Match History
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = ElectricCyan)
                    Text("Match History", style = MaterialTheme.typography.titleLarge)
                }
            }

            when (matchHistoryState) {
                UiState.Loading -> item { LoadingCenter("Loading match history...") }
                is UiState.Error -> item { ErrorBannerCard(matchHistoryState.message) }
                is UiState.Success -> {
                    val history = matchHistoryState.data
                    if (history.isEmpty()) {
                        item {
                            Text("Play a Random Quiz or Tournament to record your match history!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(history, key = { it.id }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${entry.topic} • ${entry.correctCount}/${entry.totalQuestions} correct",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("+${entry.pointsEarned} pts", style = MaterialTheme.typography.titleMedium, color = ChampionGold, fontWeight = FontWeight.Bold)
                                        Text(if (entry.won) "WIN" else "PLAYED", style = MaterialTheme.typography.labelMedium, color = if (entry.won) EmeraldWin else ElectricCyan)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_out_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert),
                    border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPasscodeDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasscodeDialog = false },
            containerColor = ArenaSurfaceHigh,
            title = { Text("Wallet Passcode Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) newPin = it },
                        label = { Text("New 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length == 4) {
                            onUpdatePasscode(true, newPin)
                            showPasscodeDialog = false
                        }
                    },
                    enabled = newPin.length == 4
                ) { Text("Save PIN") }
            },
            dismissButton = {
                Row {
                    if (privateAccount?.passcodeEnabled == true) {
                        TextButton(onClick = {
                            onUpdatePasscode(false, "")
                            showPasscodeDialog = false
                        }) { Text("Disable", color = CoralAlert) }
                    }
                    TextButton(onClick = { showPasscodeDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ArenaSurfaceHigh)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ShopAndWalletScreen(
    myProfile: PublicProfile,
    privateAccount: PrivateAccount?,
    transactionsState: UiState<List<PointTransaction>>,
    initialTab: Int = 0,
    busy: Boolean,
    onBack: () -> Unit,
    onPurchaseItem: (ShopItem) -> Unit,
    onEquipItem: (ShopItem) -> Unit
) {
    BackHandler { onBack() }
    var tab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0 = Shop, 1 = Wallet Ledger
    var itemToVerify by remember { mutableStateOf<ShopItem?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("shop_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                        Text("Shop & Wallet", style = MaterialTheme.typography.headlineSmall)
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = ArenaSurfaceHigh) {
                        Text(
                            text = "${myProfile.points} pts",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampionGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        label = { Text("Points Shop") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shop_tab_catalog")
                    )
                    FilterChip(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        label = { Text("Transaction Ledger") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shop_tab_ledger")
                    )
                }
            }

            if (tab == 0) {
                items(RankSystem.shopCatalog, key = { it.id }) { item ->
                    val owned = item.id in myProfile.ownedItems
                    val equipped = (item.category == ShopCategory.FRAME && myProfile.equippedFrame == item.id) ||
                        (item.category == ShopCategory.TITLE && myProfile.equippedTitle == item.name)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, if (equipped) EmeraldWin else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = ArenaSurfaceHigh) {
                                        Text(
                                            item.badgeText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.size(10.dp))
                            when {
                                equipped -> Text("Equipped", style = MaterialTheme.typography.labelLarge, color = EmeraldWin)
                                owned && item.category != ShopCategory.POWERUP -> OutlinedButton(
                                    onClick = { onEquipItem(item) },
                                    enabled = !busy
                                ) { Text("Equip") }
                                else -> Button(
                                    onClick = {
                                        if (privateAccount?.passcodeEnabled == true && privateAccount.passcodeHash.isNotBlank()) {
                                            itemToVerify = item
                                        } else {
                                            onPurchaseItem(item)
                                        }
                                    },
                                    enabled = !busy && myProfile.points >= item.price,
                                    colors = ButtonDefaults.buttonColors(containerColor = ChampionGold, contentColor = ArenaMidnight),
                                    modifier = Modifier.testTag("buy_${item.id}")
                                ) {
                                    Text("${item.price} pts", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                when (transactionsState) {
                    UiState.Loading -> item { LoadingCenter("Loading transactions...") }
                    is UiState.Error -> item { ErrorBannerCard(transactionsState.message) }
                    is UiState.Success -> {
                        val list = transactionsState.data
                        if (list.isEmpty()) {
                            item { Text("No transactions yet.", style = MaterialTheme.typography.bodyMedium) }
                        } else {
                            items(list, key = { it.id }) { tx ->
                                val positive = tx.amount >= 0
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tx.description, style = MaterialTheme.typography.titleMedium)
                                            Text("Balance after: ${tx.balanceAfter} pts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = if (positive) "+${tx.amount} pts" else "${tx.amount} pts",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (positive) EmeraldWin else CoralAlert,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (itemToVerify != null && privateAccount != null) {
        val target = itemToVerify!!
        PasscodeVerifyDialog(
            expectedHash = privateAccount.passcodeHash,
            actionLabel = "buy ${target.name} (${target.price} pts)",
            onVerified = {
                itemToVerify = null
                onPurchaseItem(target)
            },
            onDismiss = { itemToVerify = null }
        )
    }
}
