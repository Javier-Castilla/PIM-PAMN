package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetUserFriendsUseCase(
    private val friendshipRepository: FriendshipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: UUID): Result<List<User>> {
        return friendshipRepository.getFriendshipsForUser(userId)
            .mapCatching { friendships ->
                friendships.mapNotNull { friendship ->
                    friendship.getOtherUserId(userId)?.let { friendId ->
                        userRepository.getPublicUser(friendId).getOrNull()
                    }
                }
            }
    }
}
