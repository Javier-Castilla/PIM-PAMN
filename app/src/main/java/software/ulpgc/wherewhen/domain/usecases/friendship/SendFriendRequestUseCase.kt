package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.exceptions.friendship.SelfFriendRequestException
import software.ulpgc.wherewhen.domain.exceptions.friendship.AlreadyFriendsException
import software.ulpgc.wherewhen.domain.exceptions.friendship.FriendRequestAlreadyExistsException
import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import java.time.LocalDateTime

class SendFriendRequestUseCase(
    private val friendRequestRepository: FriendRequestRepository,
    private val friendshipRepository: FriendshipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(senderId: UUID, receiverId: UUID): Result<Unit> {
        return try {
            if (senderId == receiverId) {
                throw SelfFriendRequestException()
            }

            userRepository.getPublicUser(receiverId).getOrElse {
                throw UserNotFoundException(receiverId)
            }

            val areFriends = friendshipRepository.existsBetweenUsers(senderId, receiverId).getOrThrow()
            if (areFriends) {
                throw AlreadyFriendsException(senderId, receiverId)
            }

            val existingRequest = friendRequestRepository.getPendingBetweenUsers(senderId, receiverId).getOrNull()
            if (existingRequest != null) {
                throw FriendRequestAlreadyExistsException(senderId, receiverId)
            }

            val request = FriendRequest(
                id = UUID.random(),
                senderId = senderId,
                receiverId = receiverId,
                status = FriendRequestStatus.PENDING,
                createdAt = LocalDateTime.now()
            )

            friendRequestRepository.create(request).map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
