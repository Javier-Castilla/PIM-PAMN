package software.ulpgc.wherewhen.presentation.social.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.usecases.user.SearchUsersUseCase
import software.ulpgc.wherewhen.domain.usecases.friendship.*
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.viewModels.SocialViewModel
import software.ulpgc.wherewhen.domain.exceptions.friendship.*
import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.friendship.SentFriendRequestWithUser

data class UserWithStatus(
    val user: User,
    val status: FriendshipStatus
)

data class SocialUiState(
    val searchQuery: String = "",
    val users: List<UserWithStatus> = emptyList(),
    val receivedRequests: List<FriendRequestWithUser> = emptyList(),
    val sentRequests: List<SentFriendRequestWithUser> = emptyList(),
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
    private val getSentFriendRequestsUseCase: GetSentFriendRequestsUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val rejectFriendRequestUseCase: RejectFriendRequestUseCase,
    private val cancelFriendRequestUseCase: CancelFriendRequestUseCase,
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
                    onFailure = { showError(it.message ?: "Error searching users") }
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
                    },
                    onFailure = { error ->
                        val message = when (error) {
                            is SelfFriendRequestException -> "Cannot send request to yourself"
                            is AlreadyFriendsException -> "Already friends"
                            is FriendRequestAlreadyExistsException -> "Request already sent"
                            is UserNotFoundException -> "User not found"
                            else -> error.message ?: "Error sending request"
                        }
                        showError(message)
                    }
                )
        }
    }

    fun loadPendingRequests() {
        val currentUserId = getCurrentUserId() ?: return

        viewModelScope.launch {
            launch {
                getPendingFriendRequestsUseCase(currentUserId)
                    .collect { received ->
                        uiState = uiState.copy(receivedRequests = received)
                    }
            }

            launch {
                getSentFriendRequestsUseCase(currentUserId)
                    .collect { sent ->
                        uiState = uiState.copy(sentRequests = sent)
                    }
            }
        }
    }

    fun cancelFriendRequest(requestId: UUID) {
        val currentUserId = getCurrentUserId() ?: return
        viewModelScope.launch {
            cancelFriendRequestUseCase(requestId, currentUserId)
                .fold(
                    onSuccess = { },
                    onFailure = { error ->
                        val message = when (error) {
                            is FriendRequestNotFoundException -> "Request not found"
                            is UnauthorizedFriendshipActionException -> "Not authorized to cancel"
                            else -> error.message ?: "Error canceling request"
                        }
                        showError(message)
                    }
                )
        }
    }

    fun acceptFriendRequest(requestId: UUID) {
        val currentUserId = getCurrentUserId() ?: return
        viewModelScope.launch {
            acceptFriendRequestUseCase(requestId, currentUserId)
                .fold(
                    onSuccess = { },
                    onFailure = { error ->
                        val message = when (error) {
                            is FriendRequestNotFoundException -> "Request not found"
                            is UnauthorizedFriendshipActionException -> "Not authorized to accept"
                            else -> error.message ?: "Error accepting request"
                        }
                        showError(message)
                    }
                )
        }
    }

    fun rejectFriendRequest(requestId: UUID) {
        val currentUserId = getCurrentUserId() ?: return
        viewModelScope.launch {
            rejectFriendRequestUseCase(requestId, currentUserId)
                .fold(
                    onSuccess = { },
                    onFailure = { error ->
                        val message = when (error) {
                            is FriendRequestNotFoundException -> "Request not found"
                            is UnauthorizedFriendshipActionException -> "Not authorized to reject"
                            else -> error.message ?: "Error rejecting request"
                        }
                        showError(message)
                    }
                )
        }
    }

    fun loadFriends() {
        val currentUserId = getCurrentUserId() ?: return

        viewModelScope.launch {
            getUserFriendsUseCase(currentUserId)
                .collect { friends ->
                    uiState = uiState.copy(friends = friends)
                }
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
                    },
                    onFailure = { error ->
                        hideRemoveFriendDialog()
                        val message = when (error) {
                            is FriendshipNotFoundException -> "Friendship not found"
                            else -> error.message ?: "Error removing friend"
                        }
                        showError(message)
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
