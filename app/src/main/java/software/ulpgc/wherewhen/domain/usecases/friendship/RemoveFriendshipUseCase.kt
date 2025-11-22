package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.ports.repositories.FriendshipRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.exceptions.friendship.FriendshipNotFoundException

class RemoveFriendUseCase(
    private val friendshipRepository: FriendshipRepository
) {
    suspend operator fun invoke(userId: UUID, friendId: UUID): Result<Unit> {
        return try {
            val exists = friendshipRepository.existsBetweenUsers(userId, friendId).getOrThrow()
            if (!exists) {
                throw FriendshipNotFoundException(userId, friendId)
            }

            friendshipRepository.deleteBetweenUsers(userId, friendId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
