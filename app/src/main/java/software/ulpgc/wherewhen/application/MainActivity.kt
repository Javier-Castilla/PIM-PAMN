package software.ulpgc.wherewhen.application

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
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
import software.ulpgc.wherewhen.presentation.events.JetpackComposeEventsViewModel
import software.ulpgc.wherewhen.presentation.events.JetpackComposeEventDetailViewModel
import software.ulpgc.wherewhen.presentation.events.JetpackComposeCreateEventViewModel
import software.ulpgc.wherewhen.presentation.events.EventsViewModelFactory
import software.ulpgc.wherewhen.presentation.events.EventDetailViewModelFactory
import software.ulpgc.wherewhen.presentation.events.CreateEventViewModelFactory

class MainActivity : ComponentActivity() {

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                println("Location permission granted")
            }
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                println("Coarse location permission granted")
            }
            else -> {
                println("Location permissions denied")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intenta evitar el scrim de contraste que puede cambiar el color de la barra
        window.isNavigationBarContrastEnforced = false

        requestLocationPermissions()
        enableEdgeToEdge()

        setContent {
            WhereWhenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                println("Location permissions already granted")
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    @Composable
    fun AppNavigation() {
        val view = LocalView.current
        val isDark = isSystemInDarkTheme()
        val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()

        DisposableEffect(surfaceColor, isDark) {
            val window = (view.context as? ComponentActivity)?.window
            window?.let {
                it.statusBarColor = surfaceColor
                it.navigationBarColor = surfaceColor
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
            onDispose {}
        }

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
                        appContainer.getSentFriendRequestsUseCase,
                        appContainer.acceptFriendRequestUseCase,
                        appContainer.rejectFriendRequestUseCase,
                        appContainer.cancelFriendRequestUseCase,
                        appContainer.getUserFriendsUseCase,
                        appContainer.removeFriendUseCase
                    )
                )

                val profileViewModel: JetpackComposeProfileViewModel = viewModel(
                    key = "profile_$userId",
                    factory = ProfileViewModelFactory(
                        appContainer.getUserUseCase,
                        appContainer.updateUserProfileUseCase,
                        appContainer.updateProfileImageUseCase
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

                val eventsViewModel: JetpackComposeEventsViewModel = viewModel(
                    key = "events_$userId",
                    factory = EventsViewModelFactory(
                        appContainer.searchNearbyEventsUseCase,
                        appContainer.searchEventsByNameUseCase,
                        appContainer.searchEventsByCategoryUseCase,
                        appContainer.getUserJoinedEventsUseCase,
                        appContainer.getUserCreatedEventsUseCase,
                        appContainer.locationService
                    )
                )

                val eventDetailViewModel: JetpackComposeEventDetailViewModel = viewModel(
                    key = "event_detail_$userId",
                    factory = EventDetailViewModelFactory(
                        appContainer.getEventByIdUseCase,
                        appContainer.joinEventUseCase,
                        appContainer.leaveEventUseCase,
                        appContainer.getEventAttendeesUseCase,
                        appContainer.deleteUserEventUseCase
                    )
                )

                val createEventViewModel: JetpackComposeCreateEventViewModel = viewModel(
                    key = "create_event_$userId",
                    factory = CreateEventViewModelFactory(
                        application,
                        appContainer.createUserEventUseCase,
                        appContainer.updateUserEventUseCase,
                        appContainer.getEventByIdUseCase,
                        appContainer.locationService,
                        appContainer.imageUploadService
                    )
                )

                MainScreen(
                    socialViewModel = socialViewModel,
                    profileViewModel = profileViewModel,
                    chatsViewModel = chatsViewModel,
                    chatViewModel = chatViewModel,
                    eventsViewModel = eventsViewModel,
                    eventDetailViewModel = eventDetailViewModel,
                    createEventViewModel = createEventViewModel,
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
