package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.model.friendship.SentFriendRequestWithUser
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetSentFriendRequestsUseCase(
    private val friendRequestRepository: FriendRequestRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: UUID): Result<List<SentFriendRequestWithUser>> {
        return friendRequestRepository.getSentRequestsFromUser(userId)
            .mapCatching { requests ->
                requests.mapNotNull { request ->
                    userRepository.getPublicUser(request.receiverId)
                        .map { user -> SentFriendRequestWithUser(request, user) }
                        .getOrNull()
                }
            }
    }
}
