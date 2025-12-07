package software.ulpgc.wherewhen.presentation.chat.individual

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.model.chat.Message
import software.ulpgc.wherewhen.domain.model.user.User
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
    private var messagesCollectionJob: Job? = null

    fun getChatId(): UUID? = currentChatId

    override fun initChat(otherUser: User) {
        val currentUserId = getCurrentUserId() ?: return

        cleanupChat()

        setOtherUser(otherUser)
        showLoading()

        viewModelScope.launch {
            createOrGetChatUseCase(currentUserId, otherUser.uuid)
                .onSuccess { chat ->
                    currentChatId = chat.id
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
        messagesCollectionJob?.cancel()

        messagesCollectionJob = viewModelScope.launch {
            getChatMessagesUseCase(chatId).collect { messages ->
                showMessages(messages)
            }
        }
    }

    override fun sendMessage() {
        val chatId = currentChatId ?: return
        val currentUserId = getCurrentUserId() ?: return
        val content = uiState.messageText.trim()

        if (content.isBlank()) return

        clearMessageText()

        viewModelScope.launch {
            sendMessageUseCase(chatId, currentUserId, content)
                .onFailure { error ->
                    val message = when (error) {
                        is MessageNotFoundException -> "Message not found"
                        else -> error.message ?: "Failed to send message"
                    }
                    showError(message)
                }
        }
    }

    fun markMessagesAsRead(chatId: UUID, userId: UUID) {
        viewModelScope.launch {
            markMessagesAsReadUseCase(chatId, userId)
        }
    }

    override fun isOwnMessage(message: Message): Boolean {
        val currentUserId = getCurrentUserId() ?: return false
        return message.senderId == currentUserId
    }

    private fun getCurrentUserId(): UUID? {
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return UUID.parse(firebaseUid).getOrNull()
    }

    override fun onMessageTextChange(text: String) {
        uiState = uiState.copy(messageText = text)
    }

    private fun setOtherUser(user: User) {
        uiState = uiState.copy(otherUser = user)
    }

    private fun showMessages(messages: List<Message>) {
        uiState = uiState.copy(
            messages = messages,
            isLoading = false,
            errorMessage = null
        )
    }

    private fun clearMessageText() {
        uiState = uiState.copy(messageText = "")
    }

    private fun showLoading() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
    }

    private fun showError(message: String) {
        uiState = uiState.copy(
            isLoading = false,
            errorMessage = message
        )
    }

    fun cleanupChat() {
        messagesCollectionJob?.cancel()
        messagesCollectionJob = null
        currentChatId = null
        uiState = uiState.copy(
            messages = emptyList(),
            messageText = "",
            errorMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        cleanupChat()
    }
}
