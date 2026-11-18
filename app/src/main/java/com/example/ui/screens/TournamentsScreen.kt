package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Friendship
import com.example.model.PublicProfile
import com.example.model.QuizQuestion
import com.example.model.Tournament
import com.example.model.TournamentInvite
import com.example.model.TournamentParticipant
import com.example.model.UiState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingCenter
import com.example.ui.theme.ArenaMidnight
import com.example.ui.theme.ArenaSurfaceHigh
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.NeonViolet

@Composable
fun TournamentsScreen(
    myProfile: PublicProfile,
    tournamentsState: UiState<List<Tournament>>,
    invitesState: UiState<List<TournamentInvite>>,
    friendshipsState: UiState<List<Friendship>>,
    selectedTournament: Tournament?,
    participants: List<TournamentParticipant>,
    busy: Boolean,
    aiDraftBusy: Boolean,
    onSelectTournament: (String) -> Unit,
    onClearSelectedTournament: () -> Unit,
    onJoinTournament: (String) -> Unit,
    onStartTournament: (String) -> Unit,
    onPlayTournament: (Tournament) -> Unit,
    onFinalizeTournament: (String) -> Unit,
    onClaimPrize: (String) -> Unit,
    onGenerateAiQuestion: (topic: String, onReady: (QuizQuestion) -> Unit) -> Unit,
    onCreateTournament: (
        title: String,
        category: String,
        entryFee: Int,
        maxPlayers: Int,
        inviteScope: String,
        invitedFriendUids: List<String>,
        questions: List<QuizQuestion>
    ) -> Unit
) {
    var showCreateScreen by remember { mutableStateOf(false) }

    if (showCreateScreen) {
        CreateTournamentScreen(
            myProfile = myProfile,
            friendships = (friendshipsState as? UiState.Success)?.data.orEmpty(),
            busy = busy,
            aiDraftBusy = aiDraftBusy,
            onGenerateAiQuestion = onGenerateAiQuestion,
            onBack = { showCreateScreen = false },
            onSubmit = { title, cat, fee, maxP, scope, friendUids, questions ->
                onCreateTournament(title, cat, fee, maxP, scope, friendUids, questions)
                showCreateScreen = false
            }
        )
        return
    }

    if (selectedTournament != null) {
        TournamentDetailScreen(
            myProfile = myProfile,
            tournament = selectedTournament,
            participants = participants,
            busy = busy,
            onBack = onClearSelectedTournament,
            onJoin = { onJoinTournament(selectedTournament.id) },
            onStart = { onStartTournament(selectedTournament.id) },
            onPlay = { onPlayTournament(selectedTournament) },
            onFinalize = { onFinalizeTournament(selectedTournament.id) },
            onClaimPrize = { onClaimPrize(selectedTournament.id) }
        )
        return
    }

    Scaffold(
        containerColor = ArenaMidnight,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateScreen = true },
                containerColor = ChampionGold,
                contentColor = ArenaMidnight,
                modifier = Modifier.testTag("fab_create_tournament")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Tournament")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tournament Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_tournament_banner),
                            contentDescription = stringResource(R.string.cd_tournament_banner),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(ArenaMidnight.copy(alpha = 0.9f), ArenaMidnight.copy(alpha = 0.35f))
                                    )
                                )
                                .padding(18.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column {
                                Text("Live Tournaments", style = MaterialTheme.typography.headlineMedium, color = ChampionGold)
                                Text(
                                    "Host custom question cups, set point entry fees & win the prize pool!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }

                // Incoming Invites
                if (invitesState is UiState.Success && invitesState.data.isNotEmpty()) {
                    item {
                        Text("Tournament Invites", style = MaterialTheme.typography.titleLarge, color = ElectricCyan)
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(invitesState.data.take(10), key = { it.id }) { invite ->
                                Card(
                                    modifier = Modifier
                                        .widthIn(min = 240.dp, max = 280.dp)
                                        .clickable { onSelectTournament(invite.tournamentId) },
                                    colors = CardDefaults.cardColors(containerColor = ArenaSurfaceHigh),
                                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = if (invite.inviteType == "broadcast_online") Icons.Filled.Campaign else Icons.Filled.Group,
                                                contentDescription = null,
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (invite.inviteType == "broadcast_online") "Online Broadcast" else "Friend Invite",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = ElectricCyan
                                            )
                                        }
                                        Text(invite.tournamentTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Host @${invite.hostName} • Fee: ${invite.entryFee} pts", style = MaterialTheme.typography.bodyMedium, color = ChampionGold)
                                    }
                                }
                            }
                        }
                    }
                }

                // All Tournaments Happening
                item {
                    Text("All Tournaments", style = MaterialTheme.typography.titleLarge)
                }

                when (tournamentsState) {
                    UiState.Loading -> item { LoadingCenter("Loading tournaments...") }
                    is UiState.Error -> item { ErrorBannerCard(tournamentsState.message) }
                    is UiState.Success -> {
                        val list = tournamentsState.data
                        if (list.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Text(
                                        text = "No tournaments running yet. Tap '+' to host the first tournament and invite friends or online users!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }
                            }
                        } else {
                            items(list, key = { it.id }) { t ->
                                TournamentListCard(
                                    tournament = t,
                                    isJoined = myProfile.userId in t.memberUids,
                                    onClick = { onSelectTournament(t.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentListCard(
    tournament: Tournament,
    isJoined: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (tournament.status) {
        "live" -> EmeraldWin
        "completed" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> ElectricCyan
    }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tournament_card_${tournament.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tournament.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.16f)) {
                    Text(
                        text = tournament.status.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = "Host @${tournament.hostName} • ${tournament.category} • ${tournament.questions.size} Qs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Entry: ${tournament.entryFee} pts", style = MaterialTheme.typography.labelLarge, color = ChampionGold)
                    Text("Pool: ${tournament.prizePool} pts", style = MaterialTheme.typography.labelLarge, color = EmeraldWin)
                }
                Text(
                    text = if (isJoined) "Joined (${tournament.participantCount}/${tournament.maxPlayers})"
                    else "${tournament.participantCount}/${tournament.maxPlayers} players",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
            }
        }
    }
}

@Composable
private fun TournamentDetailScreen(
    myProfile: PublicProfile,
    tournament: Tournament,
    participants: List<TournamentParticipant>,
    busy: Boolean,
    onBack: () -> Unit,
    onJoin: () -> Unit,
    onStart: () -> Unit,
    onPlay: () -> Unit,
    onFinalize: () -> Unit,
    onClaimPrize: () -> Unit
) {
    BackHandler { onBack() }
    val isJoined = myProfile.userId in tournament.memberUids
    val isHost = tournament.hostId == myProfile.userId
    val myParticipant = participants.firstOrNull { it.userId == myProfile.userId }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tournament_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                    Text(tournament.title, style = MaterialTheme.typography.headlineSmall)
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, ChampionGold.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hosted by @${tournament.hostName} • ${tournament.category}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Entry Fee: ${tournament.entryFee} pts", style = MaterialTheme.typography.titleMedium, color = ChampionGold)
                            Text("Prize Pool: ${tournament.prizePool} pts", style = MaterialTheme.typography.titleMedium, color = EmeraldWin)
                        }
                        Text("Status: ${tournament.status.uppercase()} • ${tournament.questions.size} Custom Questions", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                        if (tournament.winnerName.isNotBlank()) {
                            Text("Winner: @${tournament.winnerName}", style = MaterialTheme.typography.titleMedium, color = ChampionGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isJoined && tournament.status in listOf("open", "live")) {
                        Button(
                            onClick = onJoin,
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("join_tournament_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionGold, contentColor = ArenaMidnight)
                        ) {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("Join Tournament (-${tournament.entryFee} pts)", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isHost && tournament.status == "open") {
                        OutlinedButton(
                            onClick = onStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("host_start_tournament_button")
                        ) {
                            Text("Launch Tournament Live")
                        }
                    }

                    if (isJoined && tournament.status in listOf("open", "live") && myParticipant?.finished != true) {
                        Button(
                            onClick = onPlay,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("play_tournament_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("Play Tournament Questions", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isHost && tournament.status != "completed" && participants.any { it.finished }) {
                        Button(
                            onClick = onFinalize,
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("finalize_tournament_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet, contentColor = ArenaMidnight)
                        ) {
                            Text("Finalize & Crown Top Scorer", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (tournament.status == "completed" && tournament.winnerUid == myProfile.userId && !tournament.prizeClaimed) {
                        Button(
                            onClick = onClaimPrize,
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("claim_tournament_prize_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldWin, contentColor = ArenaMidnight)
                        ) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Claim ${tournament.prizePool} Prize Points!", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text("Standings (${participants.size}/${tournament.maxPlayers})", style = MaterialTheme.typography.titleLarge)
            }

            itemsIndexed(participants, key = { _, p -> p.userId }) { idx, p ->
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
                        Text("#${idx + 1}  @${p.username}", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${p.score} pts", style = MaterialTheme.typography.titleMedium, color = ChampionGold, fontWeight = FontWeight.Bold)
                            Text(if (p.finished) "Finished" else "Pending", style = MaterialTheme.typography.labelMedium, color = if (p.finished) EmeraldWin else ElectricCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateTournamentScreen(
    myProfile: PublicProfile,
    friendships: List<Friendship>,
    busy: Boolean,
    aiDraftBusy: Boolean,
    onGenerateAiQuestion: (topic: String, onReady: (QuizQuestion) -> Unit) -> Unit,
    onBack: () -> Unit,
    onSubmit: (
        title: String,
        category: String,
        entryFee: Int,
        maxPlayers: Int,
        inviteScope: String,
        invitedFriendUids: List<String>,
        questions: List<QuizQuestion>
    ) -> Unit
) {
    BackHandler { onBack() }

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var entryFeeStr by remember { mutableStateOf("10") }
    var maxPlayersStr by remember { mutableStateOf("16") }
    var inviteScope by remember { mutableStateOf("online_all") }
    val selectedFriends = remember { mutableStateListOf<String>() }
    val questions = remember { mutableStateListOf<QuizQuestion>() }

    // Question builder inputs
    var qText by remember { mutableStateOf("") }
    var opt0 by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }
    var opt3 by remember { mutableStateOf("") }
    var correctIdx by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
                Text("Host a Tournament", style = MaterialTheme.typography.headlineSmall, color = ChampionGold)
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(70) },
                label = { Text("Tournament Title") },
                placeholder = { Text("e.g. Weekend Trivia Championship") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_tournament_title")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it.take(35) },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = entryFeeStr,
                    onValueChange = { if (it.all(Char::isDigit)) entryFeeStr = it.take(5) },
                    label = { Text("Entry Fee (pts)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_tournament_fee")
                )
            }

            OutlinedTextField(
                value = maxPlayersStr,
                onValueChange = { if (it.all(Char::isDigit)) maxPlayersStr = it.take(2) },
                label = { Text("Max Players (2–50)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Who should receive invites?", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = inviteScope == "online_all",
                    onClick = { inviteScope = "online_all" },
                    label = { Text("All Online Users") }
                )
                FilterChip(
                    selected = inviteScope == "friends",
                    onClick = { inviteScope = "friends" },
                    label = { Text("Select Friends (${friendships.size})") }
                )
            }

            if (inviteScope == "friends") {
                if (friendships.isEmpty()) {
                    Text("No friends added yet — add friends in the Social tab or broadcast to all online users.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    friendships.forEach { f ->
                        val partnerUid = f.partnerUid(myProfile.userId)
                        val partnerName = f.partnerName(myProfile.userId)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (partnerUid in selectedFriends) selectedFriends.remove(partnerUid)
                                    else selectedFriends.add(partnerUid)
                                }
                        ) {
                            Checkbox(
                                checked = partnerUid in selectedFriends,
                                onCheckedChange = { checked ->
                                    if (checked) selectedFriends.add(partnerUid) else selectedFriends.remove(partnerUid)
                                }
                            )
                            Text("@$partnerName", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // Custom Questions Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tournament Questions (${questions.size})", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                        OutlinedButton(
                            onClick = {
                                onGenerateAiQuestion(category) { draft ->
                                    qText = draft.question
                                    opt0 = draft.options.getOrElse(0) { "" }
                                    opt1 = draft.options.getOrElse(1) { "" }
                                    opt2 = draft.options.getOrElse(2) { "" }
                                    opt3 = draft.options.getOrElse(3) { "" }
                                    correctIdx = draft.correctIndex
                                }
                            },
                            enabled = !aiDraftBusy
                        ) {
                            if (aiDraftBusy) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.size(4.dp))
                            Text("AI Draft")
                        }
                    }

                    OutlinedTextField(
                        value = qText,
                        onValueChange = { qText = it },
                        label = { Text("Question") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tournament_question_input")
                    )

                    listOf(
                        Triple(0, opt0) { v: String -> opt0 = v },
                        Triple(1, opt1) { v: String -> opt1 = v },
                        Triple(2, opt2) { v: String -> opt2 = v },
                        Triple(3, opt3) { v: String -> opt3 = v }
                    ).forEach { (idx, value, onChange) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = correctIdx == idx,
                                onClick = { correctIdx = idx }
                            )
                            OutlinedTextField(
                                value = value,
                                onValueChange = onChange,
                                label = { Text("Option ${'A' + idx}${if (correctIdx == idx) " (Correct)" else ""}") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (qText.isNotBlank() && opt0.isNotBlank() && opt1.isNotBlank() && opt2.isNotBlank() && opt3.isNotBlank()) {
                                questions.add(
                                    QuizQuestion(
                                        question = qText.trim(),
                                        options = listOf(opt0.trim(), opt1.trim(), opt2.trim(), opt3.trim()),
                                        correctIndex = correctIdx
                                    )
                                )
                                qText = ""; opt0 = ""; opt1 = ""; opt2 = ""; opt3 = ""; correctIdx = 0
                            }
                        },
                        enabled = qText.isNotBlank() && opt0.isNotBlank() && opt1.isNotBlank() && opt2.isNotBlank() && opt3.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_tournament_question_button")
                    ) {
                        Text("Add Question to Tournament")
                    }
                }
            }

            questions.forEachIndexed { idx, q ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ArenaSurfaceHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Q${idx + 1}: ${q.question}", style = MaterialTheme.typography.titleMedium)
                            Text("Answer: ${q.options.getOrElse(q.correctIndex) { "" }}", style = MaterialTheme.typography.bodyMedium, color = EmeraldWin)
                        }
                        IconButton(onClick = { questions.removeAt(idx) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove question")
                        }
                    }
                }
            }

            val fee = entryFeeStr.toIntOrNull() ?: 0
            val maxP = (maxPlayersStr.toIntOrNull() ?: 16).coerceIn(2, 50)
            Button(
                onClick = {
                    onSubmit(
                        title.trim(),
                        category.ifBlank { "General" },
                        fee,
                        maxP,
                        inviteScope,
                        selectedFriends.toList(),
                        questions.toList()
                    )
                },
                enabled = !busy && title.trim().length >= 3 && questions.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("launch_tournament_submit"),
                colors = ButtonDefaults.buttonColors(containerColor = ChampionGold, contentColor = ArenaMidnight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Publish Tournament & Send Invites", fontWeight = FontWeight.Bold)
            }
        }
    }
}
