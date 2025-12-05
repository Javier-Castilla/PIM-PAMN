package software.ulpgc.wherewhen.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.usecases.friendship.*
import software.ulpgc.wherewhen.domain.usecases.user.GetUserUseCase

class UserProfileViewModelFactory(
    private val getUserUseCase: GetUserUseCase,
    private val checkFriendshipStatusUseCase: CheckFriendshipStatusUseCase,
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val cancelFriendRequestUseCase: CancelFriendRequestUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val rejectFriendRequestUseCase: RejectFriendRequestUseCase,
    private val getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase,
    private val getSentFriendRequestsUseCase: GetSentFriendRequestsUseCase,
    private val removeFriendUseCase: RemoveFriendUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JetpackComposeUserProfileViewModel::class.java)) {
            return JetpackComposeUserProfileViewModel(
                getUserUseCase,
                checkFriendshipStatusUseCase,
                sendFriendRequestUseCase,
                cancelFriendRequestUseCase,
                acceptFriendRequestUseCase,
                rejectFriendRequestUseCase,
                getPendingFriendRequestsUseCase,
                getSentFriendRequestsUseCase,
                removeFriendUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
