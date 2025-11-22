package software.ulpgc.wherewhen.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.model.ChatWithUser
import software.ulpgc.wherewhen.domain.usecases.chat.GetUserChatsUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.viewModels.ChatsViewModel

data class ChatsUiState(
    val chats: List<ChatWithUser> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class JetpackComposeChatsViewModel(
    private val getUserChatsUseCase: GetUserChatsUseCase
) : ViewModel(), ChatsViewModel {
    var uiState by mutableStateOf(ChatsUiState())
        private set

    val totalUnreadCount: Int
        get() = uiState.chats.sumOf { it.unreadCount }

    override fun showLoading() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
    }

    override fun hideLoading() {
        uiState = uiState.copy(isLoading = false)
    }

    override fun showChats(chats: List<ChatWithUser>) {
        uiState = uiState.copy(chats = chats, isLoading = false, errorMessage = null)
    }

    override fun showError(message: String) {
        uiState = uiState.copy(isLoading = false, errorMessage = message)
    }

    fun loadChats() {
        val currentUserId = getCurrentUserId() ?: return

        showLoading()

        viewModelScope.launch {
            getUserChatsUseCase(currentUserId).collect { chats ->
                showChats(chats)
            }
        }
    }

    private fun getCurrentUserId(): UUID? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return null
        return UUID.parse(firebaseUser.uid).getOrNull()
    }
}
