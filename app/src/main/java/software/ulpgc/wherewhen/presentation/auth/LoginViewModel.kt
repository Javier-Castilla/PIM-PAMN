package software.ulpgc.wherewhen.presentation.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.usecases.user.AuthenticateUserUseCase
import software.ulpgc.wherewhen.domain.valueObjects.Email

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null
)

class LoginViewModel(
    private val authenticateUserUseCase: AuthenticateUserUseCase
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onEmailChange(email: String) {
        uiState = uiState.copy(
            email = email,
            emailError = null,
            errorMessage = null
        )
    }

    fun onPasswordChange(password: String) {
        uiState = uiState.copy(
            password = password,
            passwordError = null,
            errorMessage = null
        )
    }

    fun onLoginClick() {
        // Validar campos básicos
        if (uiState.email.isBlank()) {
            uiState = uiState.copy(emailError = "Email is required")
            return
        }

        if (uiState.password.isBlank()) {
            uiState = uiState.copy(passwordError = "Password is required")
            return
        }

        // Iniciar login
        uiState = uiState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            // ✅ Validar y crear Email value object
            val emailResult = Email.create(uiState.email)

            if (emailResult.isFailure) {
                uiState = uiState.copy(
                    isLoading = false,
                    emailError = "Invalid email format"
                )
                return@launch
            }

            val email = emailResult.getOrThrow()

            // Autenticar
            authenticateUserUseCase(email, uiState.password).fold(
                onSuccess = { result ->
                    uiState = uiState.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                    // Aquí guardarías el token: saveToken(result.accessToken)
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Authentication failed"
                    )
                }
            )
        }
    }
}
