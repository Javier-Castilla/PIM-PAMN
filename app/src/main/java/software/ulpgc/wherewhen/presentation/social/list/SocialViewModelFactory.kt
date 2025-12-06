package software.ulpgc.wherewhen.presentation.social.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.usecases.user.SearchUsersUseCase
import software.ulpgc.wherewhen.domain.usecases.friendship.*

class SocialViewModelFactory(
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
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JetpackComposeSocialViewModel::class.java)) {
            return JetpackComposeSocialViewModel(
                searchUsersUseCase,
                sendFriendRequestUseCase,
                checkFriendshipStatusUseCase,
                getPendingFriendRequestsUseCase,
                getSentFriendRequestsUseCase,
                acceptFriendRequestUseCase,
                rejectFriendRequestUseCase,
                cancelFriendRequestUseCase,
                getUserFriendsUseCase,
                removeFriendUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
