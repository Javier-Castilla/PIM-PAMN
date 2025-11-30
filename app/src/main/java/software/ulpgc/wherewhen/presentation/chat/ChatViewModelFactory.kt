package software.ulpgc.wherewhen.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.usecases.chat.*

class ChatViewModelFactory(
    private val createOrGetChatUseCase: CreateOrGetChatUseCase,
    private val getChatMessagesUseCase: GetChatMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JetpackComposeChatViewModel::class.java)) {
            return JetpackComposeChatViewModel(
                createOrGetChatUseCase,
                getChatMessagesUseCase,
                sendMessageUseCase,
                markMessagesAsReadUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
