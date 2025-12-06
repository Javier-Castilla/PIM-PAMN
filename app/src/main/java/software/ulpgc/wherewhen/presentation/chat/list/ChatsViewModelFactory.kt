package software.ulpgc.wherewhen.presentation.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.usecases.chat.GetUserChatsUseCase

class ChatsViewModelFactory(
    private val getUserChatsUseCase: GetUserChatsUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JetpackComposeChatsViewModel::class.java)) {
            return JetpackComposeChatsViewModel(getUserChatsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
