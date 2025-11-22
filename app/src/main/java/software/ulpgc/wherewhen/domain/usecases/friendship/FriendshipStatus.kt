package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.ports.repositories.FriendshipRepository
import software.ulpgc.wherewhen.domain.ports.repositories.FriendRequestRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

enum class FriendshipStatus {
    FRIENDS,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    NOT_FRIENDS
}

class CheckFriendshipStatusUseCase(
    private val friendshipRepository: FriendshipRepository,
    private val friendRequestRepository: FriendRequestRepository
) {
    suspend operator fun invoke(currentUserId: UUID, targetUserId: UUID): Result<FriendshipStatus> {
        return runCatching {
            val areFriends = friendshipRepository.existsBetweenUsers(currentUserId, targetUserId).getOrThrow()
            if (areFriends) return@runCatching FriendshipStatus.FRIENDS
            
            val sentRequest = friendRequestRepository.getPendingBetweenUsers(currentUserId, targetUserId).getOrNull()
            if (sentRequest != null) return@runCatching FriendshipStatus.REQUEST_SENT
            
            val receivedRequest = friendRequestRepository.getPendingBetweenUsers(targetUserId, currentUserId).getOrNull()
            if (receivedRequest != null) return@runCatching FriendshipStatus.REQUEST_RECEIVED
            
            FriendshipStatus.NOT_FRIENDS
        }
    }
}
