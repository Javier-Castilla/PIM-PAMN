package software.ulpgc.wherewhen.presentation.auth.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.usecases.user.RegisterUserUseCase
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.viewModels.RegisterViewModel
import software.ulpgc.wherewhen.domain.model.user.Profile

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

class JetpackComposeRegisterViewModel(
    private val registerUserUseCase: RegisterUserUseCase
) : ViewModel(), RegisterViewModel {
    var uiState by mutableStateOf(RegisterUiState())
        private set

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

    override fun showNameError(message: String) {
        uiState = uiState.copy(isLoading = false, nameError = message)
    }

    override fun showEmailError(message: String) {
        uiState = uiState.copy(isLoading = false, emailError = message)
    }

    override fun showPasswordError(message: String) {
        uiState = uiState.copy(isLoading = false, passwordError = message)
    }

    override fun showConfirmPasswordError(message: String) {
        uiState = uiState.copy(isLoading = false, confirmPasswordError = message)
    }

    fun resetState() {
        uiState = RegisterUiState()
    }

    fun onNameChange(name: String) {
        uiState = uiState.copy(name = name, nameError = null, errorMessage = null)
    }

    fun onEmailChange(email: String) {
        uiState = uiState.copy(email = email, emailError = null, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        uiState = uiState.copy(password = password, passwordError = null, errorMessage = null)
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        uiState = uiState.copy(confirmPassword = confirmPassword, confirmPasswordError = null, errorMessage = null)
    }

    fun onRegisterClick() {
        if (uiState.name.isBlank()) {
            uiState = uiState.copy(nameError = "Name is required")
            return
        }

        if (uiState.email.isBlank()) {
            uiState = uiState.copy(emailError = "Email is required")
            return
        }

        if (uiState.password.isBlank()) {
            uiState = uiState.copy(passwordError = "Password is required")
            return
        }

        if (uiState.password.length < 6) {
            uiState = uiState.copy(passwordError = "Password must be at least 6 characters")
            return
        }

        if (uiState.confirmPassword != uiState.password) {
            uiState = uiState.copy(confirmPasswordError = "Passwords do not match")
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
            registerUserUseCase(email, uiState.name, uiState.password).fold(
                onSuccess = {
                    uiState = uiState.copy(isLoading = false, isSuccess = true, errorMessage = null)
                },
                onFailure = { error ->
                    uiState = uiState.copy(isLoading = false, errorMessage = error.message ?: "Registration failed")
                }
            )
        }
    }
}
