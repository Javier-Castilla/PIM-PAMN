package software.ulpgc.wherewhen.domain.usecases.friendship

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetUserFriendsUseCase(
    private val friendshipRepository: FriendshipRepository,
    private val userRepository: UserRepository
) {
    operator fun invoke(userId: UUID): Flow<List<User>> {
        return friendshipRepository.getFriendshipsForUser(userId)
            .map { friendships ->
                friendships.mapNotNull { friendship ->
                    friendship.getOtherUserId(userId)?.let { friendId ->
                        userRepository.getPublicUser(friendId).getOrNull()
                    }
                }
            }
    }
}
