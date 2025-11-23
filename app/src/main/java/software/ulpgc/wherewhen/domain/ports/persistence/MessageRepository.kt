package software.ulpgc.wherewhen.domain.ports.persistence

import kotlinx.coroutines.flow.Flow
import software.ulpgc.wherewhen.domain.model.chat.Message
import software.ulpgc.wherewhen.domain.valueObjects.UUID

interface MessageRepository {
    suspend fun sendMessage(message: Message): Result<Message>
    fun observeMessages(chatId: UUID): Flow<List<Message>>
    suspend fun markAsRead(messageId: UUID): Result<Unit>
    suspend fun markAllAsRead(chatId: UUID, userId: UUID): Result<Unit>
}
