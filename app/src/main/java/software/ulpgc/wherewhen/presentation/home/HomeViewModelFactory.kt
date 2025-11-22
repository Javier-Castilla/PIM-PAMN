package software.ulpgc.wherewhen.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.usecases.user.GetUserUseCase

class HomeViewModelFactory(
    private val getUserUseCase: GetUserUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(getUserUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
