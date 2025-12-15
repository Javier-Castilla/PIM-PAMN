package software.ulpgc.wherewhen.infrastructure.persistence.cached

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class CachedFriendRequestRepository (
    private val delegate: FriendRequestRepository
) : FriendRequestRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val receivedRequestsFlowCache = mutableMapOf<UUID, Flow<List<FriendRequest>>>()
    private val sentRequestsFlowCache = mutableMapOf<UUID, Flow<List<FriendRequest>>>()

    override suspend fun create(request: FriendRequest): Result<FriendRequest> {
        return delegate.create(request).also {
            if (it.isSuccess) {
                invalidateCache(request.senderId)
                invalidateCache(request.receiverId)
            }
        }
    }

    override suspend fun getById(id: UUID): Result<FriendRequest> {
        return delegate.getById(id)
    }

    override fun getPendingRequestsForUser(userId: UUID): Flow<List<FriendRequest>> {
        return receivedRequestsFlowCache.getOrPut(userId) {
            delegate.getPendingRequestsForUser(userId)
                .shareIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(5000),
                    replay = 1
                )
        }
    }

    override fun getSentRequestsFromUser(userId: UUID): Flow<List<FriendRequest>> {
        return sentRequestsFlowCache.getOrPut(userId) {
            delegate.getSentRequestsFromUser(userId)
                .shareIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(5000),
                    replay = 1
                )
        }
    }

    override suspend fun getPendingBetweenUsers(senderId: UUID, receiverId: UUID): Result<FriendRequest?> {
        return delegate.getPendingBetweenUsers(senderId, receiverId)
    }

    override suspend fun updateStatus(id: UUID, status: FriendRequestStatus, respondedAt: LocalDateTime): Result<FriendRequest> {
        return delegate.updateStatus(id, status, respondedAt).also { result ->
            if (result.isSuccess) {
                val request = result.getOrThrow()
                invalidateCache(request.senderId)
                invalidateCache(request.receiverId)
            }
        }
    }

    override suspend fun delete(id: UUID): Result<Unit> {
        return delegate.getById(id).fold(
            onSuccess = { request ->
                delegate.delete(id).also {
                    if (it.isSuccess) {
                        invalidateCache(request.senderId)
                        invalidateCache(request.receiverId)
                    }
                }
            },
            onFailure = { delegate.delete(id) }
        )
    }

    private fun invalidateCache(userId: UUID) {
        receivedRequestsFlowCache.remove(userId)
        sentRequestsFlowCache.remove(userId)
    }

    fun clearCache() {
        receivedRequestsFlowCache.clear()
        sentRequestsFlowCache.clear()
    }
}
