package software.ulpgc.wherewhen.domain.usecases.chat

import software.ulpgc.wherewhen.domain.model.chat.Chat
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class CreateOrGetChatUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(userId1: UUID, userId2: UUID): Result<Chat> {
        return try {
            chatRepository.createOrGetChat(userId1, userId2)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

//aldo was here
