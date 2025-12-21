package software.ulpgc.wherewhen.domain.ports.persistence

import kotlinx.coroutines.flow.Flow
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

interface FriendRequestRepository {
    suspend fun create(request: FriendRequest): Result<FriendRequest>
    suspend fun getById(id: UUID): Result<FriendRequest>
    fun getPendingRequestsForUser(userId: UUID): Flow<List<FriendRequest>>
    suspend fun getPendingBetweenUsers(senderId: UUID, receiverId: UUID): Result<FriendRequest?>
    suspend fun updateStatus(id: UUID, status: FriendRequestStatus, respondedAt: LocalDateTime): Result<FriendRequest>
    suspend fun delete(id: UUID): Result<Unit>
    fun getSentRequestsFromUser(userId: UUID): Flow<List<FriendRequest>>

}
