package software.ulpgc.wherewhen.domain.usecases.chat

import software.ulpgc.wherewhen.domain.ports.repositories.ChatRepository
import software.ulpgc.wherewhen.domain.ports.repositories.MessageRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class MarkMessagesAsReadUseCase(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: UUID, userId: UUID): Result<Unit> {
        return messageRepository.markAllAsRead(chatId, userId).onSuccess {
            chatRepository.resetUnreadCount(chatId, userId)
        }
    }
}
