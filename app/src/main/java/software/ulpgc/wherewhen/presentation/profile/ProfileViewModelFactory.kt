package software.ulpgc.wherewhen.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.usecases.user.GetUserUseCase
import software.ulpgc.wherewhen.domain.usecases.user.UpdateUserProfileUseCase

class ProfileViewModelFactory(
    private val getUserUseCase: GetUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JetpackComposeProfileViewModel::class.java)) {
            return JetpackComposeProfileViewModel(getUserUseCase, updateUserProfileUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
