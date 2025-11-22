package software.ulpgc.wherewhen.domain.usecases.chat

import software.ulpgc.wherewhen.domain.model.Message
import software.ulpgc.wherewhen.domain.ports.repositories.ChatRepository
import software.ulpgc.wherewhen.domain.ports.repositories.MessageRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.exceptions.chat.EmptyMessageException
import software.ulpgc.wherewhen.domain.exceptions.chat.ChatNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.chat.UnauthorizedChatAccessException
import java.time.LocalDateTime

class SendMessageUseCase(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository
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

            val chat = chatRepository.getChat(chatId).getOrElse {
                throw ChatNotFoundException(chatId)
            }

            if (!chat.isParticipant(senderId)) {
                throw UnauthorizedChatAccessException(senderId, chatId)
            }

            val message = Message(
                id = UUID.random(),
                chatId = chatId,
                senderId = senderId,
                content = content,
                timestamp = LocalDateTime.now(),
                isRead = false
            )

            messageRepository.sendMessage(message).onSuccess {
                chatRepository.updateLastMessage(chatId, content, message.timestamp)
                val receiverId = chat.getOtherParticipant(senderId)
                if (receiverId != null) {
                    chatRepository.incrementUnreadCount(chatId, receiverId)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
