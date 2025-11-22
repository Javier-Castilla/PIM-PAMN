package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.model.FriendRequest
import software.ulpgc.wherewhen.domain.model.FriendRequestStatus
import software.ulpgc.wherewhen.domain.ports.repositories.FriendRequestRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class SendFriendRequestUseCase(
    private val repository: FriendRequestRepository
) {
    suspend operator fun invoke(senderId: UUID, receiverId: UUID): Result<FriendRequest> {
        val requestId = UUID.random()
        val request = FriendRequest(
            id = requestId,
            senderId = senderId,
            receiverId = receiverId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )
        return repository.create(request)
    }
}
