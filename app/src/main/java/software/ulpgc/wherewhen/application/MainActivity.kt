package software.ulpgc.wherewhen.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import software.ulpgc.wherewhen.WhereWhenApplication
import software.ulpgc.wherewhen.application.ui.theme.WhereWhenTheme
import software.ulpgc.wherewhen.presentation.auth.login.LoginScreen
import software.ulpgc.wherewhen.presentation.auth.login.LoginViewModel
import software.ulpgc.wherewhen.presentation.auth.login.LoginViewModelFactory
import software.ulpgc.wherewhen.presentation.auth.register.RegisterScreen
import software.ulpgc.wherewhen.presentation.auth.register.RegisterViewModel
import software.ulpgc.wherewhen.presentation.auth.register.RegisterViewModelFactory
import software.ulpgc.wherewhen.presentation.home.HomeScreen
import software.ulpgc.wherewhen.presentation.home.HomeViewModel
import software.ulpgc.wherewhen.presentation.home.HomeViewModelFactory

class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels {
        val appContainer = (application as WhereWhenApplication).container
        LoginViewModelFactory(appContainer.authenticateUserUseCase)
    }

    private val registerViewModel: RegisterViewModel by viewModels {
        val appContainer = (application as WhereWhenApplication).container
        RegisterViewModelFactory(appContainer.registerUserUseCase)
    }

    private val homeViewModel: HomeViewModel by viewModels {
        val appContainer = (application as WhereWhenApplication).container
        HomeViewModelFactory(appContainer.getUserUseCase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhereWhenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        loginViewModel = loginViewModel,
                        registerViewModel = registerViewModel,
                        homeViewModel = homeViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    homeViewModel: HomeViewModel
) {
    var currentScreen by remember { mutableStateOf("login") }

    when (currentScreen) {
        "login" -> LoginScreen(
            viewModel = loginViewModel,
            onLoginSuccess = {
                currentScreen = "home"
                homeViewModel.loadUserData()
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
        "home" -> HomeScreen(
            viewModel = homeViewModel,
            onLogout = {
                homeViewModel.onLogoutClick()
                loginViewModel.resetState()
                registerViewModel.resetState()
                currentScreen = "login"
            }
        )
    }
}
