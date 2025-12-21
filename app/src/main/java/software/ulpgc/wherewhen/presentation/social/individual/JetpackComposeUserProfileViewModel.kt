package software.ulpgc.wherewhen.presentation.social.individual

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.usecases.friendship.*
import software.ulpgc.wherewhen.domain.usecases.user.GetUserUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.viewModels.UserProfileViewModel

data class UserProfileUiState(
    val profile: Profile? = null,
    val friendshipStatus: FriendshipStatus = FriendshipStatus.NOT_FRIENDS,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showRemoveDialog: Boolean = false,
    val showCancelRequestDialog: Boolean = false
)

class JetpackComposeUserProfileViewModel(
    private val getUserUseCase: GetUserUseCase,
    private val checkFriendshipStatusUseCase: CheckFriendshipStatusUseCase,
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val cancelFriendRequestUseCase: CancelFriendRequestUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val rejectFriendRequestUseCase: RejectFriendRequestUseCase,
    private val getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase,
    private val getSentFriendRequestsUseCase: GetSentFriendRequestsUseCase,
    private val removeFriendUseCase: RemoveFriendUseCase
) : ViewModel(), UserProfileViewModel {

    var uiState by mutableStateOf(UserProfileUiState())
        private set

    private var currentPendingRequestId: UUID? = null

    override fun showLoading() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
    }

    override fun hideLoading() {
        uiState = uiState.copy(isLoading = false)
    }

    override fun showUserProfile(profile: Profile, status: FriendshipStatus) {
        uiState = uiState.copy(
            profile = profile,
            friendshipStatus = status,
            isLoading = false,
            errorMessage = null
        )
    }

    override fun showError(message: String) {
        uiState = uiState.copy(isLoading = false, errorMessage = message)
    }

    override fun updateFriendshipStatus(status: FriendshipStatus) {
        uiState = uiState.copy(friendshipStatus = status)
    }

    fun loadUserProfile(userId: String) {
        val currentUserId = getCurrentUserId() ?: return

        showLoading()

        viewModelScope.launch {
            val userIdUuid = UUID.parse(userId).getOrNull()
            if (userIdUuid == null) {
                showError("Invalid user ID")
                return@launch
            }

            getUserUseCase(userIdUuid).fold(
                onSuccess = { profile ->
                    loadFriendshipStatus(currentUserId, userIdUuid, profile)
                },
                onFailure = { error ->
                    showError(error.message ?: "Error loading user profile")
                }
            )
        }
    }

    private suspend fun loadFriendshipStatus(currentUserId: UUID, targetUserId: UUID, profile: Profile) {
        val status = checkFriendshipStatusUseCase(currentUserId, targetUserId)
            .getOrDefault(FriendshipStatus.NOT_FRIENDS)

        if (status == FriendshipStatus.REQUEST_RECEIVED) {
            try {
                val requests = getPendingFriendRequestsUseCase(currentUserId).first()
                currentPendingRequestId = requests
                    .find { it.request.senderId == targetUserId }
                    ?.request?.id
            } catch (e: Exception) {
            }
        }

        showUserProfile(profile, status)
    }

    fun sendFriendRequest() {
        val currentUserId = getCurrentUserId() ?: return
        val targetUserId = uiState.profile?.uuid ?: return

        viewModelScope.launch {
            sendFriendRequestUseCase(currentUserId, targetUserId).fold(
                onSuccess = {
                    updateFriendshipStatus(FriendshipStatus.REQUEST_SENT)
                },
                onFailure = { error ->
                    showError(error.message ?: "Error sending friend request")
                }
            )
        }
    }

    fun showCancelRequestDialog() {
        uiState = uiState.copy(showCancelRequestDialog = true)
    }

    fun hideCancelRequestDialog() {
        uiState = uiState.copy(showCancelRequestDialog = false)
    }

    fun confirmCancelRequest() {
        val currentUserId = getCurrentUserId() ?: return
        val targetUserId = uiState.profile?.uuid ?: return

        viewModelScope.launch {
            try {
                val requests = getSentFriendRequestsUseCase(currentUserId).first()
                val requestId = requests
                    .find { it.request.receiverId == targetUserId }
                    ?.request?.id

                if (requestId != null) {
                    cancelFriendRequestUseCase(requestId, currentUserId).fold(
                        onSuccess = {
                            updateFriendshipStatus(FriendshipStatus.NOT_FRIENDS)
                            hideCancelRequestDialog()
                        },
                        onFailure = { error ->
                            showError(error.message ?: "Error canceling friend request")
                            hideCancelRequestDialog()
                        }
                    )
                }
            } catch (e: Exception) {
                showError(e.message ?: "Error loading requests")
                hideCancelRequestDialog()
            }
        }
    }

    fun acceptFriendRequest() {
        val currentUserId = getCurrentUserId() ?: return
        val requestId = currentPendingRequestId ?: return

        viewModelScope.launch {
            acceptFriendRequestUseCase(requestId, currentUserId).fold(
                onSuccess = {
                    updateFriendshipStatus(FriendshipStatus.FRIENDS)
                },
                onFailure = { error ->
                    showError(error.message ?: "Error accepting friend request")
                }
            )
        }
    }

    fun rejectFriendRequest() {
        val currentUserId = getCurrentUserId() ?: return
        val requestId = currentPendingRequestId ?: return

        viewModelScope.launch {
            rejectFriendRequestUseCase(requestId, currentUserId).fold(
                onSuccess = {
                    updateFriendshipStatus(FriendshipStatus.NOT_FRIENDS)
                },
                onFailure = { error ->
                    showError(error.message ?: "Error rejecting friend request")
                }
            )
        }
    }

    fun showRemoveFriendDialog() {
        uiState = uiState.copy(showRemoveDialog = true)
    }

    fun hideRemoveFriendDialog() {
        uiState = uiState.copy(showRemoveDialog = false)
    }

    fun confirmRemoveFriend() {
        val currentUserId = getCurrentUserId() ?: return
        val targetUserId = uiState.profile?.uuid ?: return

        viewModelScope.launch {
            removeFriendUseCase(currentUserId, targetUserId).fold(
                onSuccess = {
                    updateFriendshipStatus(FriendshipStatus.NOT_FRIENDS)
                    hideRemoveFriendDialog()
                },
                onFailure = { error ->
                    showError(error.message ?: "Error removing friend")
                    hideRemoveFriendDialog()
                }
            )
        }
    }

    private fun getCurrentUserId(): UUID? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return null
        return UUID.parse(firebaseUser.uid).getOrNull()
    }
}
