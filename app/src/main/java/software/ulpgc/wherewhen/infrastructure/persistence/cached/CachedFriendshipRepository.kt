package software.ulpgc.wherewhen.infrastructure.persistence.cached

import software.ulpgc.wherewhen.domain.model.friendship.Friendship
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import kotlin.time.Duration.Companion.minutes

class CachedFriendshipRepository (
    private val delegate: FriendshipRepository
) : FriendshipRepository {

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(ttlMillis: Long): Boolean {
            return System.currentTimeMillis() - timestamp > ttlMillis
        }
    }

    private val friendshipsCache = mutableMapOf<UUID, CacheEntry<List<Friendship>>>()
    private val cacheTimeToLive = 5.minutes.inWholeMilliseconds

    override suspend fun create(friendship: Friendship): Result<Friendship> {
        return delegate.create(friendship).also {
            if (it.isSuccess) {
                invalidateCache(friendship.user1Id)
                invalidateCache(friendship.user2Id)
            }
        }
    }

    override suspend fun getById(id: UUID): Result<Friendship> {
        return delegate.getById(id)
    }

    override suspend fun getFriendshipsForUser(userId: UUID): Result<List<Friendship>> {
        val cached = friendshipsCache[userId]
        if (cached != null && !cached.isExpired(cacheTimeToLive)) {
            return Result.success(cached.data)
        }

        return delegate.getFriendshipsForUser(userId).also { result ->
            if (result.isSuccess) {
                friendshipsCache[userId] = CacheEntry(result.getOrThrow())
            }
        }
    }

    override suspend fun existsBetweenUsers(user1Id: UUID, user2Id: UUID): Result<Boolean> {
        return delegate.existsBetweenUsers(user1Id, user2Id)
    }

    override suspend fun deleteBetweenUsers(user1Id: UUID, user2Id: UUID): Result<Unit> {
        return delegate.deleteBetweenUsers(user1Id, user2Id).also {
            if (it.isSuccess) {
                invalidateCache(user1Id)
                invalidateCache(user2Id)
            }
        }
    }

    private fun invalidateCache(userId: UUID) {
        friendshipsCache.remove(userId)
    }

    fun clearCache() {
        friendshipsCache.clear()
    }
}
