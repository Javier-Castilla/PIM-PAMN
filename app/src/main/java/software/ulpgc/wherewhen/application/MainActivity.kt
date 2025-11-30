package software.ulpgc.wherewhen.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import software.ulpgc.wherewhen.WhereWhenApplication
import software.ulpgc.wherewhen.application.ui.theme.WhereWhenTheme
import software.ulpgc.wherewhen.presentation.auth.login.LoginScreen
import software.ulpgc.wherewhen.presentation.auth.login.JetpackComposeLoginViewModel
import software.ulpgc.wherewhen.presentation.auth.login.LoginViewModelFactory
import software.ulpgc.wherewhen.presentation.auth.register.RegisterScreen
import software.ulpgc.wherewhen.presentation.auth.register.JetpackComposeRegisterViewModel
import software.ulpgc.wherewhen.presentation.auth.register.RegisterViewModelFactory
import software.ulpgc.wherewhen.presentation.main.MainScreen
import software.ulpgc.wherewhen.presentation.social.JetpackComposeSocialViewModel
import software.ulpgc.wherewhen.presentation.social.SocialViewModelFactory
import software.ulpgc.wherewhen.presentation.profile.JetpackComposeProfileViewModel
import software.ulpgc.wherewhen.presentation.profile.ProfileViewModelFactory
import software.ulpgc.wherewhen.presentation.chat.JetpackComposeChatViewModel
import software.ulpgc.wherewhen.presentation.chat.JetpackComposeChatsViewModel
import software.ulpgc.wherewhen.presentation.chat.ChatViewModelFactory
import software.ulpgc.wherewhen.presentation.chat.ChatsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WindowInsets.safeDrawing
            WhereWhenTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    @Composable
    fun AppNavigation() {
        val appContainer = (application as WhereWhenApplication).container
        var authState by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
        var showRegister by remember { mutableStateOf(false) }
        var loginKey by remember { mutableStateOf(0) }
        val userId = authState?.uid

        when {
            authState == null && !showRegister -> {
                val loginViewModel: JetpackComposeLoginViewModel = viewModel(
                    key = "login_$loginKey",
                    factory = LoginViewModelFactory(appContainer.authenticateUserUseCase)
                )

                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = {
                        authState = FirebaseAuth.getInstance().currentUser
                    },
                    onNavigateToRegister = {
                        showRegister = true
                    }
                )
            }

            authState == null && showRegister -> {
                val registerViewModel: JetpackComposeRegisterViewModel = viewModel(
                    factory = RegisterViewModelFactory(appContainer.registerUserUseCase)
                )

                RegisterScreen(
                    viewModel = registerViewModel,
                    onRegisterSuccess = {
                        showRegister = false
                    },
                    onNavigateToLogin = {
                        showRegister = false
                    }
                )
            }

            else -> {
                val socialViewModel: JetpackComposeSocialViewModel = viewModel(
                    key = "social_$userId",
                    factory = SocialViewModelFactory(
                        appContainer.searchUsersUseCase,
                        appContainer.sendFriendRequestUseCase,
                        appContainer.checkFriendshipStatusUseCase,
                        appContainer.getPendingFriendRequestsUseCase,
                        appContainer.acceptFriendRequestUseCase,
                        appContainer.rejectFriendRequestUseCase,
                        appContainer.getUserFriendsUseCase,
                        appContainer.removeFriendUseCase
                    )
                )

                val profileViewModel: JetpackComposeProfileViewModel = viewModel(
                    key = "profile_$userId",
                    factory = ProfileViewModelFactory(
                        appContainer.getUserUseCase,
                        appContainer.updateUserProfileUseCase
                    )
                )

                val chatsViewModel: JetpackComposeChatsViewModel = viewModel(
                    key = "chats_$userId",
                    factory = ChatsViewModelFactory(appContainer.getUserChatsUseCase)
                )

                val chatViewModel: JetpackComposeChatViewModel = viewModel(
                    key = "chat_$userId",
                    factory = ChatViewModelFactory(
                        appContainer.createOrGetChatUseCase,
                        appContainer.getChatMessagesUseCase,
                        appContainer.sendMessageUseCase,
                        appContainer.markMessagesAsReadUseCase
                    )
                )

                MainScreen(
                    socialViewModel = socialViewModel,
                    profileViewModel = profileViewModel,
                    chatsViewModel = chatsViewModel,
                    chatViewModel = chatViewModel,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        authState = null
                        loginKey++
                    },
                    onBackPressed = { finish() }
                )
            }
        }
    }
}
