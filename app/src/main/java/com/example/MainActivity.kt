package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.signOutUser
import com.example.data.AiQuizRepository
import com.example.data.ArenaMatchRepository
import com.example.data.ProfileRepository
import com.example.data.SocialRepository
import com.example.data.TournamentRepository
import com.example.model.UiState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingCenter
import com.example.ui.components.PointsPill
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HelpRulesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuizPlayScreen
import com.example.ui.screens.ShopAndWalletScreen
import com.example.ui.screens.SocialScreen
import com.example.ui.screens.TournamentsScreen
import com.example.ui.theme.ArenaMidnight
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.QuizArenaTheme
import com.example.ui.viewmodel.ProfileShopViewModel
import com.example.ui.viewmodel.QuizGameViewModel
import com.example.ui.viewmodel.QuizSessionState
import com.example.ui.viewmodel.SocialViewModel
import com.example.ui.viewmodel.TournamentViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // STEP 1: Mandatory App Check initialization with launch Intent before UI/ViewModels
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            QuizArenaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = ArenaMidnight) {
                    AppNavigation()
                }
            }
        }
    }
}

private enum class MainTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    ARENA(R.string.nav_arena, Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_arena"),
    TOURNAMENTS(R.string.nav_tournaments, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_tab_tournaments"),
    SOCIAL(R.string.nav_social, Icons.Filled.Group, Icons.Outlined.Group, "nav_tab_social"),
    PROFILE(R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile"),
    HELP(R.string.nav_help, Icons.Filled.HelpOutline, Icons.Outlined.HelpOutline, "nav_tab_help")
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedQuizArenaShell(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName.orEmpty().ifBlank { "Arena Player" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedQuizArenaShell(
    currentUserId: String,
    defaultDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val profileVm: ProfileShopViewModel = viewModel(
        key = "profile_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                ProfileShopViewModel(ProfileRepository(db, Firebase.auth), currentUserId)
            }
        }
    )
    val quizVm: QuizGameViewModel = viewModel(
        key = "quiz_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                QuizGameViewModel(
                    aiRepo = AiQuizRepository(),
                    matchRepo = ArenaMatchRepository(db, Firebase.auth),
                    profileRepo = ProfileRepository(db, Firebase.auth),
                    tournamentRepo = TournamentRepository(db, Firebase.auth),
                    currentUserId = currentUserId
                )
            }
        }
    )
    val tournamentVm: TournamentViewModel = viewModel(
        key = "tourney_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                TournamentViewModel(
                    tournamentRepo = TournamentRepository(db, Firebase.auth),
                    aiRepo = AiQuizRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )
    val socialVm: SocialViewModel = viewModel(
        key = "social_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                SocialViewModel(SocialRepository(db, Firebase.auth), currentUserId)
            }
        }
    )

    val profileState by profileVm.myProfileState.collectAsStateWithLifecycle()
    val privateAccountState by profileVm.privateAccountState.collectAsStateWithLifecycle()
    val leaderboardState by profileVm.leaderboardState.collectAsStateWithLifecycle()
    val matchHistoryState by profileVm.matchHistoryState.collectAsStateWithLifecycle()
    val transactionsState by profileVm.transactionsState.collectAsStateWithLifecycle()
    val profileBusy by profileVm.busy.collectAsStateWithLifecycle()
    val profileFeedback by profileVm.feedback.collectAsStateWithLifecycle()

    val sessionState by quizVm.sessionState.collectAsStateWithLifecycle()
    val roomsState by quizVm.publicRoomsState.collectAsStateWithLifecycle()
    val activeRoom by quizVm.activeRoom.collectAsStateWithLifecycle()
    val roomPlayers by quizVm.roomPlayers.collectAsStateWithLifecycle()
    val quizMessage by quizVm.actionMessage.collectAsStateWithLifecycle()

    val tournamentsState by tournamentVm.tournamentsState.collectAsStateWithLifecycle()
    val invitesState by tournamentVm.invitesState.collectAsStateWithLifecycle()
    val selectedTournament by tournamentVm.selectedTournament.collectAsStateWithLifecycle()
    val tournamentParticipants by tournamentVm.participants.collectAsStateWithLifecycle()
    val tournamentBusy by tournamentVm.busy.collectAsStateWithLifecycle()
    val aiDraftBusy by tournamentVm.aiDraftBusy.collectAsStateWithLifecycle()
    val tournamentFeedback by tournamentVm.feedback.collectAsStateWithLifecycle()

    val friendshipsState by socialVm.friendshipsState.collectAsStateWithLifecycle()
    val incomingRequestsState by socialVm.incomingRequestsState.collectAsStateWithLifecycle()
    val outgoingRequestsState by socialVm.outgoingRequestsState.collectAsStateWithLifecycle()
    val incomingGiftsState by socialVm.incomingGiftsState.collectAsStateWithLifecycle()
    val activeFriendship by socialVm.activeFriendship.collectAsStateWithLifecycle()
    val chatMessages by socialVm.activeChatMessages.collectAsStateWithLifecycle()
    val socialFeedback by socialVm.feedback.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(profileFeedback) {
        profileFeedback?.let { snackbarHostState.showSnackbar(it); profileVm.clearFeedback() }
    }
    LaunchedEffect(quizMessage) {
        quizMessage?.let { snackbarHostState.showSnackbar(it); quizVm.clearMessage() }
    }
    LaunchedEffect(tournamentFeedback) {
        tournamentFeedback?.let { snackbarHostState.showSnackbar(it); tournamentVm.clearFeedback() }
    }
    LaunchedEffect(socialFeedback) {
        socialFeedback?.let { snackbarHostState.showSnackbar(it); socialVm.clearFeedback() }
    }

    when (val pState = profileState) {
        UiState.Loading -> LoadingCenter("Loading your Arena profile...")
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp)) { ErrorBannerCard(pState.message) }
        is UiState.Success -> {
            val myProfile = pState.data
            if (myProfile == null) {
                OnboardingScreen(
                    defaultDisplayName = defaultDisplayName,
                    busy = profileBusy,
                    errorMessage = profileFeedback,
                    onCompleteSignUp = { u, d, pin -> profileVm.completeOnboarding(u, d, pin) }
                )
            } else {
                // Full-screen quiz play overlay takes priority when active
                if (sessionState !is QuizSessionState.Idle) {
                    QuizPlayScreen(
                        sessionState = sessionState,
                        profile = myProfile,
                        roomPlayers = roomPlayers,
                        onSubmitAnswer = { quizVm.submitAnswer(it) },
                        onNextQuestion = { quizVm.advanceToNextQuestion() },
                        onUseHint = { quizVm.useHintPowerUp() },
                        onUseFreeze = { quizVm.useFreezePowerUp() },
                        onRetryAiQuiz = { topic, diff -> quizVm.startRandomAiQuiz(topic, diff) },
                        onExit = { quizVm.exitQuizSession() }
                    )
                    return
                }

                var currentTab by remember { mutableStateOf(MainTab.ARENA) }
                var shopOverlayTab by remember { mutableStateOf<Int?>(null) } // null = closed, 0 = Shop, 1 = Wallet

                if (shopOverlayTab != null) {
                    Scaffold(
                        containerColor = ArenaMidnight,
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { pad ->
                        Box(Modifier.fillMaxSize().padding(pad)) {
                            ShopAndWalletScreen(
                                myProfile = myProfile,
                                privateAccount = (privateAccountState as? UiState.Success)?.data,
                                transactionsState = transactionsState,
                                initialTab = shopOverlayTab!!,
                                busy = profileBusy,
                                onBack = { shopOverlayTab = null },
                                onPurchaseItem = { profileVm.purchaseItem(it) },
                                onEquipItem = { profileVm.equipItem(it) }
                            )
                        }
                    }
                    return
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 600.dp

                    Scaffold(
                        containerColor = ArenaMidnight,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArenaSurface),
                                title = {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                actions = {
                                    PointsPill(
                                        points = myProfile.points,
                                        onClick = { shopOverlayTab = 0 },
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                }
                            )
                        },
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(containerColor = ArenaSurface) {
                                    MainTab.entries.forEach { tab ->
                                        val selected = currentTab == tab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = stringResource(tab.labelRes)
                                                )
                                            },
                                            label = { Text(stringResource(tab.labelRes)) },
                                            modifier = Modifier.testTag(tab.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail(containerColor = ArenaSurface) {
                                    MainTab.entries.forEach { tab ->
                                        val selected = currentTab == tab
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = stringResource(tab.labelRes)
                                                )
                                            },
                                            label = { Text(stringResource(tab.labelRes)) },
                                            modifier = Modifier.testTag(tab.testTag)
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                when (currentTab) {
                                    MainTab.ARENA -> ArenaScreen(
                                        myProfile = myProfile,
                                        leaderboardState = leaderboardState,
                                        roomsState = roomsState,
                                        activeRoom = activeRoom,
                                        roomPlayers = roomPlayers,
                                        onStartRandomQuiz = { topic, diff, doubleXp ->
                                            quizVm.startRandomAiQuiz(topic, diff, doubleXp)
                                        },
                                        onCreateMatchRoom = { title, topic, maxP ->
                                            quizVm.createAndJoinLiveRoom(myProfile.username, title, topic, maxP)
                                        },
                                        onJoinMatchRoom = { room ->
                                            quizVm.joinLiveRoom(room.id, myProfile.username)
                                        },
                                        onStartRoomByHost = { room -> quizVm.startLiveRoomByHost(room) },
                                        onLeaveRoomLobby = { quizVm.leaveRoomLobby() },
                                        onOpenShop = { shopOverlayTab = 0 }
                                    )

                                    MainTab.TOURNAMENTS -> TournamentsScreen(
                                        myProfile = myProfile,
                                        tournamentsState = tournamentsState,
                                        invitesState = invitesState,
                                        friendshipsState = friendshipsState,
                                        selectedTournament = selectedTournament,
                                        participants = tournamentParticipants,
                                        busy = tournamentBusy,
                                        aiDraftBusy = aiDraftBusy,
                                        onSelectTournament = { tournamentVm.selectTournament(it) },
                                        onClearSelectedTournament = { tournamentVm.clearSelectedTournament() },
                                        onJoinTournament = { tournamentVm.joinTournament(it, myProfile.username) },
                                        onStartTournament = { tournamentVm.startTournament(it) },
                                        onPlayTournament = { t ->
                                            quizVm.startTournamentQuiz(t.id, t.title, t.category, t.questions)
                                        },
                                        onFinalizeTournament = { tournamentVm.finalizeTournament(it) },
                                        onClaimPrize = { tournamentVm.claimPrize(it) },
                                        onGenerateAiQuestion = { topic, onReady ->
                                            tournamentVm.generateAiQuestionDraft(topic, onReady)
                                        },
                                        onCreateTournament = { title, cat, fee, maxP, scopeMode, friendUids, qs ->
                                            tournamentVm.createTournament(
                                                hostUsername = myProfile.username,
                                                title = title,
                                                category = cat,
                                                entryFee = fee,
                                                maxPlayers = maxP,
                                                inviteScope = scopeMode,
                                                invitedFriendUids = friendUids,
                                                questions = qs,
                                                onCreated = { id -> tournamentVm.selectTournament(id) }
                                            )
                                        }
                                    )

                                    MainTab.SOCIAL -> SocialScreen(
                                        myProfile = myProfile,
                                        privateAccount = (privateAccountState as? UiState.Success)?.data,
                                        allPlayersState = leaderboardState,
                                        friendshipsState = friendshipsState,
                                        incomingRequestsState = incomingRequestsState,
                                        outgoingRequestsState = outgoingRequestsState,
                                        incomingGiftsState = incomingGiftsState,
                                        activeFriendship = activeFriendship,
                                        chatMessages = chatMessages,
                                        onOpenChat = { socialVm.openChat(it) },
                                        onCloseChat = { socialVm.closeChat() },
                                        onSendChatMessage = { socialVm.sendChatMessage(myProfile.username, it) },
                                        onSendFriendRequest = { toUid, toName ->
                                            socialVm.sendFriendRequest(myProfile.username, toUid, toName)
                                        },
                                        onRespondRequest = { req, accept -> socialVm.respondToRequest(req, accept) },
                                        onSendPointsGift = { toUid, toName, amt, note ->
                                            socialVm.sendPointsGift(myProfile.username, toUid, toName, amt, note)
                                        },
                                        onClaimGift = { socialVm.claimGift(it) }
                                    )

                                    MainTab.PROFILE -> ProfileScreen(
                                        myProfile = myProfile,
                                        privateAccount = (privateAccountState as? UiState.Success)?.data,
                                        matchHistoryState = matchHistoryState,
                                        onToggleOnline = { profileVm.toggleOnlineStatus(it) },
                                        onUpdatePasscode = { enabled, pin -> profileVm.updateWalletPasscode(enabled, pin) },
                                        onOpenShop = { shopOverlayTab = 0 },
                                        onOpenWallet = { shopOverlayTab = 1 },
                                        onSignOut = {
                                            signOutUser(credentialManager, scope)
                                        }
                                    )

                                    MainTab.HELP -> HelpRulesScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
