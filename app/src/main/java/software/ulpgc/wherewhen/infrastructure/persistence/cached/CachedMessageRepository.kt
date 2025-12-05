package software.ulpgc.wherewhen.infrastructure.persistence.cached

import kotlinx.coroutines.flow.Flow
import software.ulpgc.wherewhen.domain.model.chat.Message
import software.ulpgc.wherewhen.domain.ports.persistence.MessageRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class CachedMessageRepository(
    private val delegate: MessageRepository
) : MessageRepository {

    override suspend fun sendMessage(message: Message): Result<Message> {
        return delegate.sendMessage(message)
    }

    override fun observeMessages(chatId: UUID): Flow<List<Message>> {
        return delegate.observeMessages(chatId)
    }

    override suspend fun markAsRead(messageId: UUID): Result<Unit> {
        return delegate.markAsRead(messageId)
    }

    override suspend fun markAllAsRead(chatId: UUID, userId: UUID): Result<Unit> {
        return delegate.markAllAsRead(chatId, userId)
    }

    fun clearCache() {
    }
}
