package software.ulpgc.wherewhen.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    private val loginViewModel: JetpackComposeLoginViewModel by viewModels {
        val appContainer = (application as WhereWhenApplication).container
        LoginViewModelFactory(appContainer.authenticateUserUseCase)
    }

    private val registerViewModel: JetpackComposeRegisterViewModel by viewModels {
        val appContainer = (application as WhereWhenApplication).container
        RegisterViewModelFactory(appContainer.registerUserUseCase)
    }

    private val socialViewModel: JetpackComposeSocialViewModel by viewModels {
        val appContainer = (application as WhereWhenApplication).container
        SocialViewModelFactory(
            appContainer.searchUsersUseCase,
            appContainer.sendFriendRequestUseCase,
            appContainer.checkFriendshipStatusUseCase,
            appContainer.getPendingFriendRequestsUseCase,
            appContainer.acceptFriendRequestUseCase,
            appContainer.rejectFriendRequestUseCase,
            appContainer.getUserFriendsUseCase,
            appContainer.removeFriendUseCase
        )
    }

    private val profileViewModel: JetpackComposeProfileViewModel by viewModels {
        val appContainer = (application as WhereWhenApplication).container
        ProfileViewModelFactory(appContainer.getUserUseCase, appContainer.updateUserProfileUseCase)
    }

    private val chatsViewModel: JetpackComposeChatsViewModel by viewModels {
        val appContainer = (application as WhereWhenApplication).container
        ChatsViewModelFactory(appContainer.getUserChatsUseCase)
    }

    private val chatViewModel: JetpackComposeChatViewModel by viewModels {
        val appContainer = (application as WhereWhenApplication).container
        ChatViewModelFactory(
            appContainer.createOrGetChatUseCase,
            appContainer.getChatMessagesUseCase,
            appContainer.sendMessageUseCase,
            appContainer.markMessagesAsReadUseCase
        )
    }

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
                    AppNavigation(
                        loginViewModel = loginViewModel,
                        registerViewModel = registerViewModel,
                        socialViewModel = socialViewModel,
                        profileViewModel = profileViewModel,
                        chatsViewModel = chatsViewModel,
                        chatViewModel = chatViewModel
                    )
                }
            }
        }
    }

    @Composable
    fun AppNavigation(
        loginViewModel: JetpackComposeLoginViewModel,
        registerViewModel: JetpackComposeRegisterViewModel,
        socialViewModel: JetpackComposeSocialViewModel,
        profileViewModel: JetpackComposeProfileViewModel,
        chatsViewModel: JetpackComposeChatsViewModel,
        chatViewModel: JetpackComposeChatViewModel
    ) {
        var currentScreen by remember { mutableStateOf("login") }
        when (currentScreen) {
            "login" -> LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    currentScreen = "main"
                    profileViewModel.loadProfile()
                },
                onNavigateToRegister = {
                    registerViewModel.resetState()
                    currentScreen = "register"
                }
            )
            "register" -> RegisterScreen(
                viewModel = registerViewModel,
                onRegisterSuccess = {
                    loginViewModel.resetState()
                    currentScreen = "login"
                },
                onNavigateToLogin = {
                    loginViewModel.resetState()
                    currentScreen = "login"
                }
            )
            "main" -> MainScreen(
                socialViewModel = socialViewModel,
                profileViewModel = profileViewModel,
                chatsViewModel = chatsViewModel,
                chatViewModel = chatViewModel,
                onLogout = {
                    loginViewModel.resetState()
                    registerViewModel.resetState()
                    currentScreen = "login"
                }
            )
        }
    }
}
