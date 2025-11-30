package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

data class FriendRequestWithUser(
    val request: FriendRequest,
    val user: User
)

class GetPendingFriendRequestsUseCase(
    private val friendRequestRepository: FriendRequestRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: UUID): Result<List<FriendRequestWithUser>> {
        return friendRequestRepository.getPendingRequestsForUser(userId)
            .mapCatching { requests ->
                requests.mapNotNull { request ->
                    userRepository.getPublicUser(request.senderId)
                        .map { user -> FriendRequestWithUser(request, user) }
                        .getOrNull()
                }
            }
    }
}
