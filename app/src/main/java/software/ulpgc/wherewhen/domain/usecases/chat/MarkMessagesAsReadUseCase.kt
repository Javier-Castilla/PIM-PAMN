package software.ulpgc.wherewhen.domain.usecases.chat

import software.ulpgc.wherewhen.domain.exceptions.chat.ChatNotFoundException
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.ports.persistence.MessageRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class MarkMessagesAsReadUseCase(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: UUID, userId: UUID): Result<Unit> {
        return try {
            chatRepository.getChat(chatId)
                .getOrElse { throw ChatNotFoundException(chatId) }

            messageRepository.markAllAsRead(chatId, userId)
                .onSuccess {
                    chatRepository.resetUnreadCount(chatId, userId)
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
