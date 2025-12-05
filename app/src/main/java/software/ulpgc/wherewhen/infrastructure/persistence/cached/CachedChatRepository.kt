package software.ulpgc.wherewhen.infrastructure.persistence.cached

import kotlinx.coroutines.flow.Flow
import software.ulpgc.wherewhen.domain.model.chat.Chat
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes

class CachedChatRepository(
    private val delegate: ChatRepository
) : ChatRepository {

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(ttlMillis: Long): Boolean {
            return System.currentTimeMillis() - timestamp > ttlMillis
        }
    }

    private val chatCache = mutableMapOf<UUID, CacheEntry<Chat>>()
    private val cacheTimeToLive = 5.minutes.inWholeMilliseconds

    override suspend fun createOrGetChat(userId1: UUID, userId2: UUID): Result<Chat> {
        return delegate.createOrGetChat(userId1, userId2).also { result ->
            if (result.isSuccess) {
                val chat = result.getOrThrow()
                chatCache[chat.id] = CacheEntry(chat)
            }
        }
    }

    override suspend fun getChat(chatId: UUID): Result<Chat> {
        val cached = chatCache[chatId]
        if (cached != null && !cached.isExpired(cacheTimeToLive)) {
            return Result.success(cached.data)
        }

        return delegate.getChat(chatId).also { result ->
            if (result.isSuccess) {
                chatCache[chatId] = CacheEntry(result.getOrThrow())
            }
        }
    }

    override fun observeUserChats(userId: UUID): Flow<List<Chat>> {
        return delegate.observeUserChats(userId)
    }

    override suspend fun updateLastMessage(chatId: UUID, message: String, timestamp: LocalDateTime): Result<Unit> {
        return delegate.updateLastMessage(chatId, message, timestamp).also {
            if (it.isSuccess) {
                invalidateCache(chatId)
            }
        }
    }

    override suspend fun incrementUnreadCount(chatId: UUID, userId: UUID): Result<Unit> {
        return delegate.incrementUnreadCount(chatId, userId).also {
            if (it.isSuccess) {
                invalidateCache(chatId)
            }
        }
    }

    override suspend fun resetUnreadCount(chatId: UUID, userId: UUID): Result<Unit> {
        return delegate.resetUnreadCount(chatId, userId).also {
            if (it.isSuccess) {
                invalidateCache(chatId)
            }
        }
    }

    private fun invalidateCache(chatId: UUID) {
        chatCache.remove(chatId)
    }

    fun clearCache() {
        chatCache.clear()
    }
}
