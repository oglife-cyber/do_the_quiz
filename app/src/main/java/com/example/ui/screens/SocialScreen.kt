package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ChatMessage
import com.example.model.FriendGift
import com.example.model.FriendRequest
import com.example.model.Friendship
import com.example.model.PrivateAccount
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingCenter
import com.example.ui.components.PasscodeVerifyDialog
import com.example.ui.components.PlayerAvatar
import com.example.ui.theme.ArenaMidnight
import com.example.ui.theme.ArenaSurfaceHigh
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldWin

@Composable
fun SocialScreen(
    myProfile: PublicProfile,
    privateAccount: PrivateAccount?,
    allPlayersState: UiState<List<PublicProfile>>,
    friendshipsState: UiState<List<Friendship>>,
    incomingRequestsState: UiState<List<FriendRequest>>,
    outgoingRequestsState: UiState<List<FriendRequest>>,
    incomingGiftsState: UiState<List<FriendGift>>,
    activeFriendship: Friendship?,
    chatMessages: List<ChatMessage>,
    onOpenChat: (Friendship) -> Unit,
    onCloseChat: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onSendFriendRequest: (toUid: String, toUsername: String) -> Unit,
    onRespondRequest: (FriendRequest, Boolean) -> Unit,
    onSendPointsGift: (toUid: String, toUsername: String, amount: Int, note: String) -> Unit,
    onClaimGift: (FriendGift) -> Unit
) {
    if (activeFriendship != null) {
        FriendChatScreen(
            myProfile = myProfile,
            friendship = activeFriendship,
            messages = chatMessages,
            onBack = onCloseChat,
            onSend = onSendChatMessage
        )
        return
    }

    var subTab by remember { mutableIntStateOf(0) } // 0 = Friends, 1 = Requests & Gifts, 2 = Find Players
    var searchQuery by remember { mutableStateOf("") }
    var giftTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // uid to username
    var pendingVerifiedGift by remember { mutableStateOf<Triple<String, String, Pair<Int, String>>?>(null) }

    val friendships = (friendshipsState as? UiState.Success)?.data.orEmpty()
    val friendUids = remember(friendships, myProfile.userId) {
        friendships.map { it.partnerUid(myProfile.userId) }.toSet()
    }
    val incomingReqs = (incomingRequestsState as? UiState.Success)?.data.orEmpty()
    val outgoingUids = remember(outgoingRequestsState) {
        (outgoingRequestsState as? UiState.Success)?.data?.map { it.toUid }.orEmpty().toSet()
    }
    val incomingGifts = (incomingGiftsState as? UiState.Success)?.data.orEmpty()
    val allPlayers = (allPlayersState as? UiState.Success)?.data.orEmpty()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Friends & Messaging", style = MaterialTheme.typography.headlineMedium, color = ElectricCyan)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = subTab == 0,
                        onClick = { subTab = 0 },
                        label = { Text("Friends (${friendships.size})") },
                        modifier = Modifier.testTag("social_tab_friends")
                    )
                    FilterChip(
                        selected = subTab == 1,
                        onClick = { subTab = 1 },
                        label = { Text("Inbox (${incomingReqs.size + incomingGifts.size})") },
                        modifier = Modifier.testTag("social_tab_inbox")
                    )
                    FilterChip(
                        selected = subTab == 2,
                        onClick = { subTab = 2 },
                        label = { Text("Find Players") },
                        modifier = Modifier.testTag("social_tab_find")
                    )
                }
            }

            when (subTab) {
                0 -> {
                    when (friendshipsState) {
                        UiState.Loading -> item { LoadingCenter("Loading friends...") }
                        is UiState.Error -> item { ErrorBannerCard(friendshipsState.message) }
                        is UiState.Success -> {
                            if (friendships.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_empty_social),
                                                contentDescription = stringResource(R.string.cd_social_empty),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(150.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                            )
                                            Text("No Friends Yet", style = MaterialTheme.typography.titleLarge)
                                            Text(
                                                "Add players in 'Find Players' to unlock 1-on-1 direct messaging, tournament invites, and point gifts!",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Button(onClick = { subTab = 2 }) {
                                                Text("Find Online Players")
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(friendships, key = { it.id }) { friendship ->
                                    val pUid = friendship.partnerUid(myProfile.userId)
                                    val pName = friendship.partnerName(myProfile.userId)
                                    val partnerProfile = allPlayers.firstOrNull { it.userId == pUid }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            PlayerAvatar(
                                                username = pName,
                                                frameId = partnerProfile?.equippedFrame ?: "starter",
                                                online = partnerProfile?.online == true,
                                                size = 46.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("@$pName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = if (partnerProfile?.online == true) "Online • Lv.${partnerProfile.level}" else "Offline",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (partnerProfile?.online == true) EmeraldWin else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { giftTarget = pUid to pName },
                                                modifier = Modifier.testTag("gift_friend_$pUid")
                                            ) {
                                                Icon(Icons.Filled.CardGiftcard, contentDescription = "Send points gift", tint = ChampionGold)
                                            }
                                            Button(
                                                onClick = { onOpenChat(friendship) },
                                                modifier = Modifier.testTag("chat_friend_$pUid")
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.size(4.dp))
                                                Text("Chat")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Text("Incoming Point Gifts (${incomingGifts.size})", style = MaterialTheme.typography.titleLarge, color = ChampionGold)
                    }
                    if (incomingGifts.isEmpty()) {
                        item {
                            Text("No pending point gifts.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(incomingGifts, key = { it.id }) { gift ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ArenaSurfaceHigh),
                                border = BorderStroke(1.dp, ChampionGold.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("+${gift.amount} pts from @${gift.fromUsername}", style = MaterialTheme.typography.titleMedium, color = ChampionGold, fontWeight = FontWeight.Bold)
                                        if (gift.note.isNotBlank()) {
                                            Text("\"${gift.note}\"", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    Button(
                                        onClick = { onClaimGift(gift) },
                                        modifier = Modifier.testTag("claim_gift_${gift.id}")
                                    ) {
                                        Text("Claim")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(6.dp))
                        Text("Friend Requests (${incomingReqs.size})", style = MaterialTheme.typography.titleLarge, color = ElectricCyan)
                    }
                    if (incomingReqs.isEmpty()) {
                        item {
                            Text("No pending friend requests.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(incomingReqs, key = { it.id }) { req ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("@${req.fromUsername} wants to be friends", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = { onRespondRequest(req, true) },
                                            modifier = Modifier.testTag("accept_request_${req.id}")
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = "Accept", tint = EmeraldWin)
                                        }
                                        IconButton(
                                            onClick = { onRespondRequest(req, false) },
                                            modifier = Modifier.testTag("decline_request_${req.id}")
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search by @username") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_players_input")
                        )
                    }
                    val filtered = allPlayers.filter {
                        it.userId != myProfile.userId &&
                            (searchQuery.isBlank() || it.username.contains(searchQuery.trim(), ignoreCase = true) ||
                                it.displayName.contains(searchQuery.trim(), ignoreCase = true))
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text("No matching players found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(filtered, key = { it.userId }) { player ->
                            val isFriend = player.userId in friendUids
                            val alreadySent = player.userId in outgoingUids
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PlayerAvatar(
                                        username = player.username,
                                        frameId = player.equippedFrame,
                                        online = player.online,
                                        size = 44.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("@${player.username}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${if (player.online) "Online" else "Offline"} • Lv.${player.level} • ${player.points} pts",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (player.online) EmeraldWin else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    when {
                                        isFriend -> Text("Friends", style = MaterialTheme.typography.labelLarge, color = EmeraldWin)
                                        alreadySent -> Text("Sent", style = MaterialTheme.typography.labelLarge, color = ChampionGold)
                                        else -> OutlinedButton(
                                            onClick = { onSendFriendRequest(player.userId, player.username) },
                                            modifier = Modifier.testTag("add_friend_${player.userId}")
                                        ) {
                                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.size(4.dp))
                                            Text("Add")
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

    // Send Points Gift Dialog
    if (giftTarget != null) {
        val (toUid, toUsername) = giftTarget!!
        var amountStr by remember { mutableStateOf("10") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { giftTarget = null },
            containerColor = ArenaSurfaceHigh,
            icon = { Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = ChampionGold) },
            title = { Text("Send Points to @$toUsername") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your balance: ${myProfile.points} pts", color = ChampionGold)
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { if (it.all(Char::isDigit)) amountStr = it.take(5) },
                        label = { Text("Points Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gift_amount_input")
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(90) },
                        label = { Text("Optional Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toIntOrNull() ?: 0
                        if (amt > 0) {
                            giftTarget = null
                            if (privateAccount?.passcodeEnabled == true && privateAccount.passcodeHash.isNotBlank()) {
                                pendingVerifiedGift = Triple(toUid, toUsername, amt to note)
                            } else {
                                onSendPointsGift(toUid, toUsername, amt, note)
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_send_gift_button")
                ) {
                    Text("Send Gift")
                }
            },
            dismissButton = {
                TextButton(onClick = { giftTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (pendingVerifiedGift != null && privateAccount != null) {
        val (toUid, toUsername, payload) = pendingVerifiedGift!!
        PasscodeVerifyDialog(
            expectedHash = privateAccount.passcodeHash,
            actionLabel = "send ${payload.first} pts to @$toUsername",
            onVerified = {
                pendingVerifiedGift = null
                onSendPointsGift(toUid, toUsername, payload.first, payload.second)
            },
            onDismiss = { pendingVerifiedGift = null }
        )
    }
}

@Composable
private fun FriendChatScreen(
    myProfile: PublicProfile,
    friendship: Friendship,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSend: (String) -> Unit
) {
    BackHandler { onBack() }
    val partnerName = friendship.partnerName(myProfile.userId)
    var messageInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            Surface(color = ArenaSurfaceHigh, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                    Column {
                        Text("@$partnerName", style = MaterialTheme.typography.titleLarge, color = ElectricCyan)
                        Text("Friends Direct Message", style = MaterialTheme.typography.labelMedium, color = EmeraldWin)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(ArenaMidnight),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            text = "Say hello to @$partnerName! Invite them to a tournament or discuss trivia strategies.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        val isMine = msg.senderUid == myProfile.userId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isMine) MaterialTheme.colorScheme.primaryContainer else ArenaSurfaceHigh,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Surface(color = ArenaSurfaceHigh) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it.take(500) },
                        placeholder = { Text("Message @$partnerName...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )
                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                onSend(messageInput)
                                messageInput = ""
                            }
                        },
                        modifier = Modifier.testTag("chat_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.cd_send_message), tint = ElectricCyan)
                    }
                }
            }
        }
    }
}
