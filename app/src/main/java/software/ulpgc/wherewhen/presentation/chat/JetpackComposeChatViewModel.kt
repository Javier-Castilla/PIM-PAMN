package software.ulpgc.wherewhen.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.model.Message
import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.usecases.chat.CreateOrGetChatUseCase
import software.ulpgc.wherewhen.domain.usecases.chat.GetChatMessagesUseCase
import software.ulpgc.wherewhen.domain.usecases.chat.MarkMessagesAsReadUseCase
import software.ulpgc.wherewhen.domain.usecases.chat.SendMessageUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.viewModels.ChatViewModel
import software.ulpgc.wherewhen.domain.exceptions.chat.*

data class ChatUiState(
    val otherUser: User? = null,
    val messages: List<Message> = emptyList(),
    val messageText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class JetpackComposeChatViewModel(
    private val createOrGetChatUseCase: CreateOrGetChatUseCase,
    private val getChatMessagesUseCase: GetChatMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase
) : ViewModel(), ChatViewModel {

    var uiState by mutableStateOf(ChatUiState())
        private set

    private var currentChatId: UUID? = null

    override fun showLoading() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
    }

    override fun hideLoading() {
        uiState = uiState.copy(isLoading = false)
    }

    override fun showMessages(messages: List<Message>) {
        uiState = uiState.copy(messages = messages, isLoading = false, errorMessage = null)
    }

    override fun showError(message: String) {
        uiState = uiState.copy(isLoading = false, errorMessage = message)
    }

    override fun setOtherUser(user: User) {
        uiState = uiState.copy(otherUser = user)
    }

    fun initChat(otherUser: User) {
        val currentUserId = getCurrentUserId() ?: return
        setOtherUser(otherUser)
        showLoading()

        viewModelScope.launch {
            createOrGetChatUseCase(currentUserId, otherUser.uuid)
                .onSuccess { chat ->
                    currentChatId = chat.id
                    markMessagesAsRead(chat.id, currentUserId)
                    observeMessages(chat.id, currentUserId)
                }
                .onFailure { error ->
                    val message = when (error) {
                        is ChatNotFoundException -> "Chat not found"
                        else -> error.message ?: "Error loading chat"
                    }
                    showError(message)
                }
        }
    }

    private fun observeMessages(chatId: UUID, userId: UUID) {
        viewModelScope.launch {
            getChatMessagesUseCase(chatId).collect { messages ->
                showMessages(messages)
                if (messages.isNotEmpty()) {
                    markMessagesAsRead(chatId, userId)
                }
            }
        }
    }

    private fun markMessagesAsRead(chatId: UUID, userId: UUID) {
        viewModelScope.launch {
            markMessagesAsReadUseCase(chatId, userId)
        }
    }

    fun onMessageTextChange(text: String) {
        uiState = uiState.copy(messageText = text)
    }

    fun sendMessage() {
        val currentUserId = getCurrentUserId() ?: return
        val chatId = currentChatId ?: return
        val text = uiState.messageText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            sendMessageUseCase(chatId, currentUserId, text)
                .onSuccess {
                    uiState = uiState.copy(messageText = "")
                }
                .onFailure { error ->
                    val message = when (error) {
                        is EmptyMessageException -> "Cannot send empty message"
                        is UnauthorizedChatAccessException -> "Not authorized to access chat"
                        is ChatNotFoundException -> "Chat not found"
                        else -> error.message ?: "Error sending message"
                    }
                    showError(message)
                }
        }
    }

    fun isOwnMessage(message: Message): Boolean {
        val currentUserId = getCurrentUserId() ?: return false
        return message.senderId == currentUserId
    }

    private fun getCurrentUserId(): UUID? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return null
        return UUID.parse(firebaseUser.uid).getOrNull()
    }
}
