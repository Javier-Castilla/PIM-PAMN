package software.ulpgc.wherewhen.domain.model.friendship

import software.ulpgc.wherewhen.domain.model.user.User

data class SentFriendRequestWithUser(
    val request: FriendRequest,
    val user: User
)