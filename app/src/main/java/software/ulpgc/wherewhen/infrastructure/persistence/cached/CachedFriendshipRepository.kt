package software.ulpgc.wherewhen.infrastructure.persistence.cached

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import software.ulpgc.wherewhen.domain.model.friendship.Friendship
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class CachedFriendshipRepository (
    private val delegate: FriendshipRepository
) : FriendshipRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val friendshipsFlowCache = mutableMapOf<UUID, Flow<List<Friendship>>>()

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

    override fun getFriendshipsForUser(userId: UUID): Flow<List<Friendship>> {
        return friendshipsFlowCache.getOrPut(userId) {
            delegate.getFriendshipsForUser(userId)
                .shareIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(5000),
                    replay = 1
                )
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
        friendshipsFlowCache.remove(userId)
    }

    fun clearCache() {
        friendshipsFlowCache.clear()
    }
}
