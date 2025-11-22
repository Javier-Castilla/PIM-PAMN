package software.ulpgc.wherewhen.presentation.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.usecases.user.SearchUsersUseCase
import software.ulpgc.wherewhen.domain.usecases.friendship.*
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.viewModels.SocialViewModel

data class UserWithStatus(
    val user: User,
    val status: FriendshipStatus
)

data class SocialUiState(
    val searchQuery: String = "",
    val users: List<UserWithStatus> = emptyList(),
    val pendingRequests: List<FriendRequestWithUser> = emptyList(),
    val friends: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val friendToRemove: User? = null
)

class JetpackComposeSocialViewModel(
    private val searchUsersUseCase: SearchUsersUseCase,
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val checkFriendshipStatusUseCase: CheckFriendshipStatusUseCase,
    private val getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val rejectFriendRequestUseCase: RejectFriendRequestUseCase,
    private val getUserFriendsUseCase: GetUserFriendsUseCase,
    private val removeFriendUseCase: RemoveFriendUseCase
) : ViewModel(), SocialViewModel {
    var uiState by mutableStateOf(SocialUiState())
        private set

    override fun showLoading() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
    }

    override fun hideLoading() {
        uiState = uiState.copy(isLoading = false)
    }

    override fun showUsers(users: List<User>) {
        val currentUserId = getCurrentUserId() ?: return

        viewModelScope.launch {
            val filteredUsers = users.filter { it.uuid != currentUserId }

            val usersWithStatus = filteredUsers.map { user ->
                val status = checkFriendshipStatusUseCase(currentUserId, user.uuid)
                    .getOrDefault(FriendshipStatus.NOT_FRIENDS)
                UserWithStatus(user, status)
            }
            uiState = uiState.copy(users = usersWithStatus, isLoading = false, errorMessage = null)
        }
    }

    override fun showError(message: String) {
        uiState = uiState.copy(isLoading = false, errorMessage = message)
    }

    override fun showEmptyResults() {
        uiState = uiState.copy(users = emptyList(), isLoading = false, errorMessage = null)
    }

    fun onSearchQueryChange(query: String) {
        uiState = uiState.copy(searchQuery = query, errorMessage = null)
        searchUsers()
    }

    private fun searchUsers() {
        if (uiState.searchQuery.isBlank()) {
            showEmptyResults()
            return
        }

        showLoading()

        viewModelScope.launch {
            searchUsersUseCase(uiState.searchQuery)
                .fold(
                    onSuccess = { users ->
                        if (users.isEmpty()) {
                            showEmptyResults()
                        } else {
                            showUsers(users)
                        }
                    },
                    onFailure = { showError(it.message ?: "Search failed") }
                )
        }
    }

    fun sendFriendRequest(targetUserId: UUID) {
        val currentUserId = getCurrentUserId() ?: return

        viewModelScope.launch {
            sendFriendRequestUseCase(currentUserId, targetUserId)
                .fold(
                    onSuccess = {
                        searchUsers()
                        loadPendingRequests()
                    },
                    onFailure = { showError(it.message ?: "Failed to send request") }
                )
        }
    }

    fun loadPendingRequests() {
        val currentUserId = getCurrentUserId() ?: return

        viewModelScope.launch {
            getPendingFriendRequestsUseCase(currentUserId)
                .fold(
                    onSuccess = { requests ->
                        uiState = uiState.copy(pendingRequests = requests)
                    },
                    onFailure = { showError(it.message ?: "Failed to load requests") }
                )
        }
    }

    fun acceptFriendRequest(requestId: UUID) {
        viewModelScope.launch {
            acceptFriendRequestUseCase(requestId)
                .fold(
                    onSuccess = {
                        loadPendingRequests()
                        loadFriends()
                    },
                    onFailure = { showError(it.message ?: "Failed to accept request") }
                )
        }
    }

    fun rejectFriendRequest(requestId: UUID) {
        viewModelScope.launch {
            rejectFriendRequestUseCase(requestId)
                .fold(
                    onSuccess = { loadPendingRequests() },
                    onFailure = { showError(it.message ?: "Failed to reject request") }
                )
        }
    }

    fun loadFriends() {
        val currentUserId = getCurrentUserId() ?: return

        viewModelScope.launch {
            getUserFriendsUseCase(currentUserId)
                .fold(
                    onSuccess = { friends ->
                        uiState = uiState.copy(friends = friends)
                    },
                    onFailure = { showError(it.message ?: "Failed to load friends") }
                )
        }
    }

    fun showRemoveFriendDialog(friend: User) {
        uiState = uiState.copy(friendToRemove = friend)
    }

    fun hideRemoveFriendDialog() {
        uiState = uiState.copy(friendToRemove = null)
    }

    fun confirmRemoveFriend() {
        val currentUserId = getCurrentUserId() ?: return
        val friendToRemove = uiState.friendToRemove ?: return

        viewModelScope.launch {
            removeFriendUseCase(currentUserId, friendToRemove.uuid)
                .fold(
                    onSuccess = {
                        hideRemoveFriendDialog()
                        loadFriends()
                    },
                    onFailure = {
                        hideRemoveFriendDialog()
                        showError(it.message ?: "Failed to remove friend")
                    }
                )
        }
    }

    fun clearSearch() {
        uiState = uiState.copy(
            searchQuery = "",
            users = emptyList(),
            errorMessage = null
        )
    }


    private fun getCurrentUserId(): UUID? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return null
        return UUID.parse(firebaseUser.uid).getOrNull()
    }
}
