package software.ulpgc.wherewhen.domain.usecases.chat

import software.ulpgc.wherewhen.domain.exceptions.chat.ChatNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.chat.EmptyMessageException
import software.ulpgc.wherewhen.domain.model.chat.Message
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.ports.persistence.MessageRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.Instant

class SendMessageUseCase(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository
) {

    suspend operator fun invoke(
        chatId: UUID,
        senderId: UUID,
        content: String
    ): Result<Message> {
        return try {
            if (content.isBlank()) {
                throw EmptyMessageException()
            }

            val chat = chatRepository.getChat(chatId)
                .getOrElse { throw ChatNotFoundException(chatId) }

            val receiverId = chat.getOtherParticipant(senderId)
                ?: throw ChatNotFoundException(chatId)

            val message = Message(
                id = UUID.random(),
                chatId = chatId,
                senderId = senderId,
                content = content,
                timestamp = Instant.now(),
                isRead = false
            )

            messageRepository.sendMessage(message).getOrThrow()
            chatRepository.updateLastMessage(chatId, content, message.timestamp).getOrThrow()
            chatRepository.incrementUnreadCount(chatId, receiverId).getOrThrow()

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
