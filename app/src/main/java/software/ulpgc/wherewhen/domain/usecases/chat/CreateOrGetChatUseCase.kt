package software.ulpgc.wherewhen.domain.usecases.chat

import software.ulpgc.wherewhen.domain.model.Chat
import software.ulpgc.wherewhen.domain.ports.repositories.ChatRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class CreateOrGetChatUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(userId1: UUID, userId2: UUID): Result<Chat> {
        return chatRepository.createOrGetChat(userId1, userId2)
    }
}
