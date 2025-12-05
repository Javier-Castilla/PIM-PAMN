package software.ulpgc.wherewhen.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.presentation.events.EventsScreen
import software.ulpgc.wherewhen.presentation.events.EventDetailScreen
import software.ulpgc.wherewhen.presentation.events.CreateEventScreen
import software.ulpgc.wherewhen.presentation.events.JetpackComposeEventsViewModel
import software.ulpgc.wherewhen.presentation.events.JetpackComposeEventDetailViewModel
import software.ulpgc.wherewhen.presentation.events.JetpackComposeCreateEventViewModel
import software.ulpgc.wherewhen.presentation.social.SocialScreen
import software.ulpgc.wherewhen.presentation.social.JetpackComposeSocialViewModel
import software.ulpgc.wherewhen.presentation.profile.ProfileScreen
import software.ulpgc.wherewhen.presentation.profile.JetpackComposeProfileViewModel
import software.ulpgc.wherewhen.presentation.chat.ChatsScreen
import software.ulpgc.wherewhen.presentation.chat.ChatScreen
import software.ulpgc.wherewhen.presentation.chat.JetpackComposeChatsViewModel
import software.ulpgc.wherewhen.presentation.chat.JetpackComposeChatViewModel

@Composable
fun MainScreen(
    socialViewModel: JetpackComposeSocialViewModel,
    profileViewModel: JetpackComposeProfileViewModel,
    chatsViewModel: JetpackComposeChatsViewModel,
    chatViewModel: JetpackComposeChatViewModel,
    eventsViewModel: JetpackComposeEventsViewModel,
    eventDetailViewModel: JetpackComposeEventDetailViewModel,
    createEventViewModel: JetpackComposeCreateEventViewModel,
    onLogout: () -> Unit,
    onBackPressed: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedChatUser by remember { mutableStateOf<User?>(null) }
    var selectedEventId by remember { mutableStateOf<UUID?>(null) }
    var editingEventId by remember { mutableStateOf<UUID?>(null) }
    var isCreatingEvent by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        when {
            isCreatingEvent || editingEventId != null -> {
                isCreatingEvent = false
                editingEventId = null
            }
            selectedEventId != null -> selectedEventId = null
            selectedChatUser != null -> selectedChatUser = null
            else -> onBackPressed()
        }
    }

    LaunchedEffect(Unit) {
        chatsViewModel.loadChats()
        socialViewModel.loadPendingRequests()
    }

    when {
        isCreatingEvent || editingEventId != null -> {
            CreateEventScreen(
                viewModel = createEventViewModel,
                eventIdToEdit = editingEventId,
                onNavigateBack = {
                    isCreatingEvent = false
                    editingEventId = null
                },
                onEventCreated = {
                    isCreatingEvent = false
                    editingEventId = null
                    eventsViewModel.onRefresh()
                }
            )
        }

        selectedEventId != null -> {
            EventDetailScreen(
                viewModel = eventDetailViewModel,
                eventId = selectedEventId!!,
                onNavigateBack = { selectedEventId = null },
                onEditEvent = { eventId ->
                    selectedEventId = null
                    editingEventId = eventId
                },
                onEventDeleted = {
                    selectedEventId = null
                    eventsViewModel.onRefresh()
                }
            )
        }

        selectedChatUser != null -> {
            ChatScreen(
                viewModel = chatViewModel,
                otherUser = selectedChatUser!!,
                onBackClick = { selectedChatUser = null }
            )
        }

        else -> {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    Surface(
                        shadowElevation = 8.dp
                    ) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text("Events") },
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 }
                            )

                            NavigationBarItem(
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (socialViewModel.uiState.receivedRequests.isNotEmpty()) {
                                                Badge {
                                                    Text("${socialViewModel.uiState.receivedRequests.size}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    }
                                },
                                label = { Text("Social") },
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 }
                            )

                            NavigationBarItem(
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (chatsViewModel.totalUnreadCount > 0) {
                                                Badge {
                                                    Text("${chatsViewModel.totalUnreadCount}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null)
                                    }
                                },
                                label = { Text("Messages") },
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 }
                            )

                            NavigationBarItem(
                                icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                                label = { Text("Profile") },
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp)
                ) {
                    when (selectedTab) {
                        0 -> EventsScreen(
                            viewModel = eventsViewModel,
                            onEventClick = { eventId -> selectedEventId = eventId },
                            onCreateEventClick = { isCreatingEvent = true }
                        )

                        1 -> SocialScreen(
                            viewModel = socialViewModel,
                            onMessageClick = { friend ->
                                selectedChatUser = friend
                            }
                        )

                        2 -> ChatsScreen(
                            viewModel = chatsViewModel,
                            onChatClick = { chatWithUser ->
                                selectedChatUser = chatWithUser.otherUser
                            }
                        )

                        3 -> ProfileScreen(
                            viewModel = profileViewModel,
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}
