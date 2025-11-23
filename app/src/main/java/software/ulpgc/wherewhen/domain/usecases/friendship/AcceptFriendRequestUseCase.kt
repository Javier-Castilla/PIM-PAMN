package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.model.friendship.Friendship
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.exceptions.friendship.FriendRequestNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.friendship.UnauthorizedFriendshipActionException
import java.time.LocalDateTime

class AcceptFriendRequestUseCase(
    private val friendRequestRepository: FriendRequestRepository,
    private val friendshipRepository: FriendshipRepository
) {
    suspend operator fun invoke(requestId: UUID, userId: UUID): Result<Unit> {
        return try {
            val request = friendRequestRepository.getById(requestId).getOrElse {
                throw FriendRequestNotFoundException(requestId)
            }

            if (request.receiverId != userId) {
                throw UnauthorizedFriendshipActionException("accept")
            }

            friendRequestRepository.updateStatus(requestId, FriendRequestStatus.ACCEPTED, LocalDateTime.now()).getOrThrow()

            val friendship = Friendship(
                id = UUID.random(),
                user1Id = request.senderId,
                user2Id = request.receiverId,
                createdAt = LocalDateTime.now()
            )

            friendshipRepository.create(friendship).map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
