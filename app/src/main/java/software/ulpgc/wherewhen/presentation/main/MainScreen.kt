package software.ulpgc.wherewhen.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.presentation.events.EventsScreen
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
    onLogout: () -> Unit,
    onBackPressed: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedChatUser by remember { mutableStateOf<User?>(null) }

    BackHandler(enabled = true) {
        if (selectedChatUser != null) {
            selectedChatUser = null
        } else {
            onBackPressed()
        }
    }

    LaunchedEffect(Unit) {
        chatsViewModel.loadChats()
        socialViewModel.loadPendingRequests()
    }

    if (selectedChatUser != null) {
        ChatScreen(
            viewModel = chatViewModel,
            otherUser = selectedChatUser!!,
            onBackClick = { selectedChatUser = null }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
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
                                    if (socialViewModel.uiState.pendingRequests.isNotEmpty()) {
                                        Badge {
                                            Text("${socialViewModel.uiState.pendingRequests.size}")
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
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> EventsScreen()
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
