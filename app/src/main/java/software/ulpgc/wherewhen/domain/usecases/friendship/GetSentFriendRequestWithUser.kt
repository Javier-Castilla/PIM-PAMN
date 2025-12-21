package software.ulpgc.wherewhen.domain.usecases.friendship

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import software.ulpgc.wherewhen.domain.model.friendship.SentFriendRequestWithUser
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetSentFriendRequestsUseCase(
    private val friendRequestRepository: FriendRequestRepository,
    private val userRepository: UserRepository
) {
    operator fun invoke(userId: UUID): Flow<List<SentFriendRequestWithUser>> {
        return friendRequestRepository.getSentRequestsFromUser(userId)
            .map { requests ->
                requests.mapNotNull { request ->
                    userRepository.getPublicUser(request.receiverId)
                        .map { user -> SentFriendRequestWithUser(request, user) }
                        .getOrNull()
                }
            }
    }
}
