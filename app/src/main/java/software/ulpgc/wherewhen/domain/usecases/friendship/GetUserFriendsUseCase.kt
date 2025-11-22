package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.ports.repositories.FriendshipRepository
import software.ulpgc.wherewhen.domain.ports.repositories.UserRepository
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
