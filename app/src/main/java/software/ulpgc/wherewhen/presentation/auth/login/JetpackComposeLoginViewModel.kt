package software.ulpgc.wherewhen.presentation.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.usecases.user.AuthenticateUserUseCase
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.viewModels.LoginViewModel
import software.ulpgc.wherewhen.domain.model.user.Profile

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null
)

class JetpackComposeLoginViewModel(
    private val authenticateUserUseCase: AuthenticateUserUseCase
) : ViewModel(), LoginViewModel {
    var uiState by mutableStateOf(LoginUiState())
        private set

    init {
        resetState()
    }

    override fun showLoading() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
    }

    override fun hideLoading() {
        uiState = uiState.copy(isLoading = false)
    }

    override fun showSuccess(profile: Profile) {
        uiState = uiState.copy(isLoading = false, isSuccess = true, errorMessage = null)
    }

    override fun showError(message: String) {
        uiState = uiState.copy(isLoading = false, errorMessage = message)
    }

    override fun showEmailError(message: String) {
        uiState = uiState.copy(isLoading = false, emailError = message)
    }

    override fun showPasswordError(message: String) {
        uiState = uiState.copy(isLoading = false, passwordError = message)
    }

    fun resetState() {
        uiState = LoginUiState()
    }

    fun onEmailChange(email: String) {
        uiState = uiState.copy(email = email, emailError = null, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        uiState = uiState.copy(password = password, passwordError = null, errorMessage = null)
    }

    fun onLoginClick() {
        if (uiState.email.isBlank()) {
            uiState = uiState.copy(emailError = "Email is required")
            return
        }

        if (uiState.password.isBlank()) {
            uiState = uiState.copy(passwordError = "Password is required")
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val emailResult = Email.create(uiState.email)
            if (emailResult.isFailure) {
                uiState = uiState.copy(isLoading = false, emailError = "Invalid email format")
                return@launch
            }

            val email = emailResult.getOrThrow()
            authenticateUserUseCase(email, uiState.password).fold(
                onSuccess = {
                    uiState = uiState.copy(isLoading = false, isSuccess = true, errorMessage = null)
                },
                onFailure = { error ->
                    uiState = uiState.copy(isLoading = false, errorMessage = error.message ?: "Login failed")
                }
            )
        }
    }
}
