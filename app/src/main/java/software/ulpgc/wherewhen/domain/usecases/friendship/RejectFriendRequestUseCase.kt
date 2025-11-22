package software.ulpgc.wherewhen.domain.usecases.friendship

import software.ulpgc.wherewhen.domain.model.FriendRequestStatus
import software.ulpgc.wherewhen.domain.ports.repositories.FriendRequestRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class RejectFriendRequestUseCase(
    private val repository: FriendRequestRepository
) {
    suspend operator fun invoke(requestId: UUID): Result<Unit> {
        return repository.updateStatus(requestId, FriendRequestStatus.REJECTED, LocalDateTime.now())
            .map { }
    }
}
