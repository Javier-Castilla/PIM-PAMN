package software.ulpgc.wherewhen.domain.ports.repositories

import kotlinx.coroutines.flow.Flow
import software.ulpgc.wherewhen.domain.model.Chat
import software.ulpgc.wherewhen.domain.valueObjects.UUID

interface ChatRepository {
    suspend fun createOrGetChat(userId1: UUID, userId2: UUID): Result<Chat>
    suspend fun getChat(chatId: UUID): Result<Chat>
    fun observeUserChats(userId: UUID): Flow<List<Chat>>
    suspend fun updateLastMessage(chatId: UUID, message: String, timestamp: java.time.LocalDateTime): Result<Unit>
    suspend fun incrementUnreadCount(chatId: UUID, userId: UUID): Result<Unit>
    suspend fun resetUnreadCount(chatId: UUID, userId: UUID): Result<Unit>
}
