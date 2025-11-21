package software.ulpgc.wherewhen.application

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import software.ulpgc.wherewhen.WhereWhenApplication
import software.ulpgc.wherewhen.application.ui.theme.WhereWhenTheme
import software.ulpgc.wherewhen.presentation.auth.LoginScreen
import software.ulpgc.wherewhen.presentation.auth.LoginViewModel
import software.ulpgc.wherewhen.presentation.auth.LoginViewModelFactory

class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels {
        val appContainer = (application as WhereWhenApplication).container
        LoginViewModelFactory(appContainer.authenticateUserUseCase)
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
                    LoginScreen(
                        viewModel = loginViewModel,
                        onLoginSuccess = {
                            Toast.makeText(
                                this,
                                "Login exitoso!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
        }
    }
}
