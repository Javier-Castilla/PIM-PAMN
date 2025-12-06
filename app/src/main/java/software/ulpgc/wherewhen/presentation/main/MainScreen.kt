package software.ulpgc.wherewhen.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.presentation.chat.individual.ChatScreen
import software.ulpgc.wherewhen.presentation.chat.individual.JetpackComposeChatViewModel
import software.ulpgc.wherewhen.presentation.chat.list.ChatsScreen
import software.ulpgc.wherewhen.presentation.chat.list.JetpackComposeChatsViewModel
import software.ulpgc.wherewhen.presentation.events.CreateEventScreen
import software.ulpgc.wherewhen.presentation.events.EventsScreen
import software.ulpgc.wherewhen.presentation.events.JetpackComposeCreateEventViewModel
import software.ulpgc.wherewhen.presentation.events.JetpackComposeEventsViewModel
import software.ulpgc.wherewhen.presentation.events.individual.EventDetailScreen
import software.ulpgc.wherewhen.presentation.events.individual.JetpackComposeEventDetailViewModel
import software.ulpgc.wherewhen.presentation.profile.JetpackComposeProfileViewModel
import software.ulpgc.wherewhen.presentation.profile.ProfileScreen
import software.ulpgc.wherewhen.presentation.social.individual.JetpackComposeUserProfileViewModel
import software.ulpgc.wherewhen.presentation.social.individual.UserProfileScreen
import software.ulpgc.wherewhen.presentation.social.list.JetpackComposeSocialViewModel
import software.ulpgc.wherewhen.presentation.social.list.SocialScreen

@Composable
fun MainScreen(
    socialViewModel: JetpackComposeSocialViewModel,
    userProfileViewModel: JetpackComposeUserProfileViewModel,
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
    var selectedUserProfileId by remember { mutableStateOf<String?>(null) }
    var returnToChatUser by remember { mutableStateOf<User?>(null) }
    var returnToEvent by remember { mutableStateOf(false) }
    var socialSelectedTab by remember { mutableStateOf(0) }

    BackHandler(enabled = true) {
        when {
            isCreatingEvent || editingEventId != null -> {
                isCreatingEvent = false
                editingEventId = null
            }
            selectedUserProfileId != null -> {
                selectedUserProfileId = null
                if (returnToEvent) {
                    returnToEvent = false
                } else if (returnToChatUser != null) {
                    selectedChatUser = returnToChatUser
                    returnToChatUser = null
                }
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
        selectedUserProfileId != null -> {
            UserProfileScreen(
                viewModel = userProfileViewModel,
                profileId = selectedUserProfileId!!,
                onBackClick = {
                    selectedUserProfileId = null
                    if (returnToEvent) {
                        returnToEvent = false
                    } else if (returnToChatUser != null) {
                        selectedChatUser = returnToChatUser
                        returnToChatUser = null
                    }
                },
                onMessageClick = { user ->
                    selectedUserProfileId = null
                    returnToEvent = false
                    returnToChatUser = null
                    selectedChatUser = user
                }
            )
        }
        selectedChatUser != null -> {
            ChatScreen(
                viewModel = chatViewModel,
                otherUser = selectedChatUser!!,
                onBackClick = { selectedChatUser = null },
                onUserClick = { userId ->
                    returnToEvent = false
                    returnToChatUser = selectedChatUser
                    selectedChatUser = null
                    selectedUserProfileId = userId
                }
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
                                icon = { androidx.compose.material3.Icon(Icons.Default.Home, contentDescription = null) },
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
                                        androidx.compose.material3.Icon(Icons.Default.Person, contentDescription = null)
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
                                        androidx.compose.material3.Icon(Icons.Default.Email, contentDescription = null)
                                    }
                                },
                                label = { Text("Messages") },
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 }
                            )
                            NavigationBarItem(
                                icon = { androidx.compose.material3.Icon(Icons.Default.AccountCircle, contentDescription = null) },
                                label = { Text("Profile") },
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 }
                            )
                        }
                    }
                }
            ) { _ ->
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
                            selectedTab = socialSelectedTab,
                            onTabSelected = { index ->
                                socialSelectedTab = index
                            },
                            onMessageClick = { friend ->
                                selectedChatUser = friend
                            },
                            onUserClick = { userId ->
                                returnToEvent = false
                                selectedUserProfileId = userId
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
