package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.ports.repositories.FriendshipRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class RemoveFriendUseCase(
    private val friendshipRepository: FriendshipRepository
) {
    suspend operator fun invoke(userId: UUID, friendId: UUID): Result<Unit> {
        return friendshipRepository.deleteBetweenUsers(userId, friendId)
    }
}
