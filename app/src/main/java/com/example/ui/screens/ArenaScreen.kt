package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MatchPlayer
import com.example.model.MatchRoom
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingCenter
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.RankTierBadge
import com.example.ui.theme.ArenaMidnight
import com.example.ui.theme.ArenaSurfaceHigh
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.NeonViolet

@Composable
fun ArenaScreen(
    myProfile: PublicProfile,
    leaderboardState: UiState<List<PublicProfile>>,
    roomsState: UiState<List<MatchRoom>>,
    activeRoom: MatchRoom?,
    roomPlayers: List<MatchPlayer>,
    onStartRandomQuiz: (topic: String, difficulty: String, useDoubleXp: Boolean) -> Unit,
    onCreateMatchRoom: (title: String, topic: String, maxPlayers: Int) -> Unit,
    onJoinMatchRoom: (MatchRoom) -> Unit,
    onStartRoomByHost: (MatchRoom) -> Unit,
    onLeaveRoomLobby: () -> Unit,
    onOpenShop: () -> Unit
) {
    val topics = listOf("Surprise Me", "Science & Tech", "Gaming", "World History", "Movies & Music", "Sports")
    val difficulties = listOf("Easy", "Medium", "Hard")

    var selectedTopic by remember { mutableStateOf("Surprise Me") }
    var selectedDifficulty by remember { mutableStateOf("Medium") }
    var useDoubleXp by remember { mutableStateOf(false) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Play & Rooms, 1 = Global Leaderboard

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_arena_hero),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ArenaMidnight.copy(alpha = 0.92f),
                                        ArenaMidnight.copy(alpha = 0.45f)
                                    )
                                )
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            RankTierBadge(level = myProfile.level)
                            Text(
                                text = "Welcome, @${myProfile.username}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "+10 points for every correct answer!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChampionGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Segmented Switch: Play vs Global Leaderboard
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedSection == 0,
                        onClick = { selectedSection = 0 },
                        label = { Text("Play & Live Matches") },
                        leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arena_tab_play")
                    )
                    FilterChip(
                        selected = selectedSection == 1,
                        onClick = { selectedSection = 1 },
                        label = { Text("Global Leaderboard") },
                        leadingIcon = { Icon(Icons.Filled.Leaderboard, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arena_tab_leaderboard")
                    )
                }
            }

            if (selectedSection == 0) {
                // Random Quiz (AI) Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NeonViolet)
                                Column {
                                    Text(
                                        text = "Random Quiz Mode",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = NeonViolet
                                    )
                                    Text(
                                        text = "AI generates a brand-new 5-question trivia challenge",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(topics) { topic ->
                                    FilterChip(
                                        selected = selectedTopic == topic,
                                        onClick = { selectedTopic = topic },
                                        label = { Text(topic) }
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                difficulties.forEach { diff ->
                                    FilterChip(
                                        selected = selectedDifficulty == diff,
                                        onClick = { selectedDifficulty = diff },
                                        label = { Text(diff) }
                                    )
                                }
                            }

                            if (myProfile.doubleXpPowerUps > 0L) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Activate 2x XP Booster (${myProfile.doubleXpPowerUps} owned)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ChampionGold
                                    )
                                    Switch(
                                        checked = useDoubleXp,
                                        onCheckedChange = { useDoubleXp = it }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onStartRandomQuiz(
                                        selectedTopic,
                                        selectedDifficulty,
                                        useDoubleXp && myProfile.doubleXpPowerUps > 0L
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("start_random_quiz_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet, contentColor = ArenaMidnight),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Bolt, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Join Random Quiz", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Points Shop Quick Banner
                item {
                    Card(
                        onClick = onOpenShop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("arena_open_shop_card"),
                        colors = CardDefaults.cardColors(containerColor = ArenaSurfaceHigh),
                        border = BorderStroke(1.dp, ChampionGold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Filled.Storefront, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(28.dp))
                                Column {
                                    Text("Points Shop & Rewards", style = MaterialTheme.typography.titleMedium, color = ChampionGold)
                                    Text("Spend your ${myProfile.points} pts on frames, titles & boosters", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("Open", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                        }
                    }
                }

                // Live Multiplayer Match Rooms Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = ElectricCyan)
                            Text("Live Trivia Rooms", style = MaterialTheme.typography.titleLarge)
                        }
                        OutlinedButton(
                            onClick = { showCreateRoomDialog = true },
                            modifier = Modifier.testTag("create_live_room_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Host Match")
                        }
                    }
                }

                when (roomsState) {
                    UiState.Loading -> item { LoadingCenter("Loading live rooms...") }
                    is UiState.Error -> item { ErrorBannerCard(roomsState.message) }
                    is UiState.Success -> {
                        val activeRooms = roomsState.data.filter { it.status != "completed" }
                        if (activeRooms.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Text(
                                        text = "No open live rooms right now. Tap 'Host Match' to challenge real players!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(18.dp)
                                    )
                                }
                            }
                        } else {
                            items(activeRooms, key = { it.id }) { room ->
                                val joined = myProfile.userId in room.memberUids
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(room.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                text = "Host @${room.hostName} • ${room.topic} • ${room.playerCount}/${room.maxPlayers} players",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = { onJoinMatchRoom(room) },
                                            modifier = Modifier.testTag("join_room_${room.id}")
                                        ) {
                                            Text(if (joined) "Lobby" else "Join")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Global Leaderboard Tab
                when (leaderboardState) {
                    UiState.Loading -> item { LoadingCenter("Loading global rankings...") }
                    is UiState.Error -> item { ErrorBannerCard(leaderboardState.message) }
                    is UiState.Success -> {
                        val players = leaderboardState.data
                        if (players.isEmpty()) {
                            item {
                                Text("No ranked players yet.", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            item {
                                Text(
                                    text = "Top Global Players",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ChampionGold
                                )
                            }
                            itemsIndexed(players, key = { _, p -> p.userId }) { index, player ->
                                val rank = index + 1
                                val isMe = player.userId == myProfile.userId
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) ElectricCyan.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        when (rank) {
                                            1 -> ChampionGold
                                            2 -> ElectricCyan.copy(alpha = 0.7f)
                                            3 -> NeonViolet.copy(alpha = 0.7f)
                                            else -> if (isMe) ElectricCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "#$rank",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = if (rank <= 3) ChampionGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                        PlayerAvatar(
                                            username = player.username,
                                            frameId = player.equippedFrame,
                                            online = player.online,
                                            size = 44.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = "@${player.username}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isMe) {
                                                    Text("(You)", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                                                }
                                            }
                                            Text(
                                                text = "Lv.${player.level} • ${player.equippedTitle} • ${player.wins} wins",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${player.points} pts",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = ChampionGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${player.xp} XP",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = NeonViolet
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
    }

    // Create Live Room Dialog
    if (showCreateRoomDialog) {
        var roomTitle by remember { mutableStateOf("") }
        var roomTopic by remember { mutableStateOf("Science & Tech") }
        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            containerColor = ArenaSurfaceHigh,
            title = { Text("Host Live Trivia Room") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = roomTitle,
                        onValueChange = { roomTitle = it.take(60) },
                        label = { Text("Room Title") },
                        placeholder = { Text("e.g. Friday Brain Clash") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_room_title_input")
                    )
                    OutlinedTextField(
                        value = roomTopic,
                        onValueChange = { roomTopic = it.take(35) },
                        label = { Text("Trivia Topic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "AI will generate 5 synchronized questions for all players in the room.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateRoomDialog = false
                        onCreateMatchRoom(roomTitle.ifBlank { "$roomTopic Clash" }, roomTopic.ifBlank { "General" }, 6)
                    },
                    modifier = Modifier.testTag("confirm_create_room_button")
                ) {
                    Text("Create Room")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoomDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Waiting Room Lobby Dialog
    if (activeRoom != null && activeRoom.status == "waiting") {
        AlertDialog(
            onDismissRequest = onLeaveRoomLobby,
            containerColor = ArenaSurfaceHigh,
            icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ElectricCyan) },
            title = { Text(activeRoom.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Topic: ${activeRoom.topic} • Waiting for host to start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Players in Lobby (${roomPlayers.size}/${activeRoom.maxPlayers}):", style = MaterialTheme.typography.labelLarge)
                    roomPlayers.forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("@${p.username}", fontWeight = FontWeight.SemiBold)
                                Text(if (p.userId == activeRoom.hostId) "Host" else "Ready", color = EmeraldWin)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (activeRoom.hostId == myProfile.userId) {
                    Button(
                        onClick = { onStartRoomByHost(activeRoom) },
                        modifier = Modifier.testTag("start_live_room_button")
                    ) {
                        Text("Start Match")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onLeaveRoomLobby) { Text("Close") }
            }
        )
    }
}
