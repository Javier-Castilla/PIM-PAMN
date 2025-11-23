package software.ulpgc.wherewhen.domain.usecases.chat

import kotlinx.coroutines.flow.Flow
import software.ulpgc.wherewhen.domain.model.chat.Message
import software.ulpgc.wherewhen.domain.ports.persistence.MessageRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetChatMessagesUseCase(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(chatId: UUID): Flow<List<Message>> {
        return messageRepository.observeMessages(chatId)
    }
}
