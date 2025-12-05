package software.ulpgc.wherewhen.infrastructure.persistence.cached

import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes

class CachedFriendRequestRepository (
    private val delegate: FriendRequestRepository
) : FriendRequestRepository {

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(ttlMillis: Long): Boolean {
            return System.currentTimeMillis() - timestamp > ttlMillis
        }
    }

    private val receivedRequestsCache = mutableMapOf<UUID, CacheEntry<List<FriendRequest>>>()
    private val sentRequestsCache = mutableMapOf<UUID, CacheEntry<List<FriendRequest>>>()
    private val cacheTimeToLive = 5.minutes.inWholeMilliseconds

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

    override suspend fun getPendingRequestsForUser(userId: UUID): Result<List<FriendRequest>> {
        val cached = receivedRequestsCache[userId]
        if (cached != null && !cached.isExpired(cacheTimeToLive)) {
            return Result.success(cached.data)
        }

        return delegate.getPendingRequestsForUser(userId).also { result ->
            if (result.isSuccess) {
                receivedRequestsCache[userId] = CacheEntry(result.getOrThrow())
            }
        }
    }

    override suspend fun getSentRequestsFromUser(userId: UUID): Result<List<FriendRequest>> {
        val cached = sentRequestsCache[userId]
        if (cached != null && !cached.isExpired(cacheTimeToLive)) {
            return Result.success(cached.data)
        }

        return delegate.getSentRequestsFromUser(userId).also { result ->
            if (result.isSuccess) {
                sentRequestsCache[userId] = CacheEntry(result.getOrThrow())
            }
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
        receivedRequestsCache.remove(userId)
        sentRequestsCache.remove(userId)
    }

    fun clearCache() {
        receivedRequestsCache.clear()
        sentRequestsCache.clear()
    }
}
