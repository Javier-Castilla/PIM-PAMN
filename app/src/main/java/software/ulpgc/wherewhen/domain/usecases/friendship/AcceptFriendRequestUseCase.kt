package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.model.Friendship
import software.ulpgc.wherewhen.domain.model.FriendRequestStatus
import software.ulpgc.wherewhen.domain.ports.repositories.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.repositories.FriendshipRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class AcceptFriendRequestUseCase(
    private val friendRequestRepository: FriendRequestRepository,
    private val friendshipRepository: FriendshipRepository
) {
    suspend operator fun invoke(requestId: UUID): Result<Friendship> {
        return friendRequestRepository.getById(requestId)
            .mapCatching { request ->
                friendRequestRepository.updateStatus(requestId, FriendRequestStatus.ACCEPTED, LocalDateTime.now())
                    .getOrThrow()
                
                val friendship = Friendship(
                    id = UUID.random(),
                    user1Id = request.senderId,
                    user2Id = request.receiverId,
                    createdAt = LocalDateTime.now()
                )
                
                friendshipRepository.create(friendship).getOrThrow()
            }
    }
}
