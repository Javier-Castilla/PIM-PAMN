package software.ulpgc.wherewhen.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.usecases.user.AuthenticateUserUseCase

class LoginViewModelFactory(
    private val authenticateUserUseCase: AuthenticateUserUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JetpackComposeLoginViewModel::class.java)) {
            return JetpackComposeLoginViewModel(authenticateUserUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
