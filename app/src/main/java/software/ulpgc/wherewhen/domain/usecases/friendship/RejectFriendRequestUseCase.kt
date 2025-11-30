package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.exceptions.friendship.FriendRequestNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.friendship.UnauthorizedFriendshipActionException
import java.time.LocalDateTime

class RejectFriendRequestUseCase(
    private val friendRequestRepository: FriendRequestRepository
) {
    suspend operator fun invoke(requestId: UUID, userId: UUID): Result<Unit> {
        return try {
            val request = friendRequestRepository.getById(requestId).getOrElse {
                throw FriendRequestNotFoundException(requestId)
            }

            if (request.receiverId != userId) {
                throw UnauthorizedFriendshipActionException("reject")
            }

            friendRequestRepository.updateStatus(requestId, FriendRequestStatus.REJECTED, LocalDateTime.now()).map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
