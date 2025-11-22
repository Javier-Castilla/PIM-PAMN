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
                .onFailure { showError(it.message ?: "Failed to load chat") }
        }
    }

    private fun observeMessages(chatId: UUID, userId: UUID) {
        viewModelScope.launch {
            getChatMessagesUseCase(chatId).collect { messages ->
                showMessages(messages)
                println("DEBUG: Messages received: ${messages.size}")  // <-- QUITAR
                if (messages.isNotEmpty()) {
                    println("DEBUG: Marking messages as read for chatId: ${chatId.value}, userId: ${userId.value}")  // <-- QUITAR
                    markMessagesAsRead(chatId, userId)
                }
            }
        }
    }

    private fun markMessagesAsRead(chatId: UUID, userId: UUID) {
        viewModelScope.launch {
            markMessagesAsReadUseCase(chatId, userId)
                .onSuccess {
                    println("DEBUG: Messages marked as read successfully")  // <-- QUITAR
                }
                .onFailure {
                    println("DEBUG: Failed to mark as read: ${it.message}")  // <-- QUITAR
                }
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
                .onFailure {
                    showError(it.message ?: "Failed to send message")
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
