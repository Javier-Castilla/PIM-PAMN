package software.ulpgc.wherewhen.domain.usecases.chat

import kotlinx.coroutines.flow.Flow
import software.ulpgc.wherewhen.domain.model.Message
import software.ulpgc.wherewhen.domain.ports.repositories.MessageRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetChatMessagesUseCase(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(chatId: UUID): Flow<List<Message>> {
        return messageRepository.observeMessages(chatId)
    }
}
