package software.ulpgc.wherewhen.domain.usecases.chat

import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.ports.persistence.MessageRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class MarkMessagesAsReadUseCase(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: UUID, userId: UUID): Result<Unit> {
        return try {
            messageRepository.markAllAsRead(chatId, userId).onSuccess {
                chatRepository.resetUnreadCount(chatId, userId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
