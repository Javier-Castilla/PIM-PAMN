package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.exceptions.friendship.FriendRequestNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.friendship.UnauthorizedFriendshipActionException

class CancelFriendRequestUseCase(
    private val friendRequestRepository: FriendRequestRepository
) {
    suspend operator fun invoke(requestId: UUID, userId: UUID): Result<Unit> {
        return try {
            val request = friendRequestRepository.getById(requestId)
                .getOrElse { throw FriendRequestNotFoundException(requestId) }

            if (request.senderId != userId) {
                throw UnauthorizedFriendshipActionException("Cancel request for non known user")
            }

            friendRequestRepository.delete(requestId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
