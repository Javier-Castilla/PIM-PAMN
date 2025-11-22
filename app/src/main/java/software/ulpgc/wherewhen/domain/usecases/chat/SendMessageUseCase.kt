package software.ulpgc.wherewhen.domain.usecases.chat

import software.ulpgc.wherewhen.domain.model.Message
import software.ulpgc.wherewhen.domain.ports.repositories.ChatRepository
import software.ulpgc.wherewhen.domain.ports.repositories.MessageRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
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
        val message = Message(
            id = UUID.random(),
            chatId = chatId,
            senderId = senderId,
            content = content,
            timestamp = LocalDateTime.now(),
            isRead = false
        )

        return messageRepository.sendMessage(message).onSuccess {
            chatRepository.updateLastMessage(chatId, content, message.timestamp)

            val chat = chatRepository.getChat(chatId).getOrNull()
            if (chat != null) {
                val receiverId = chat.getOtherParticipant(senderId)
                if (receiverId != null) {
                    chatRepository.incrementUnreadCount(chatId, receiverId)
                }
            }
        }
    }
}
