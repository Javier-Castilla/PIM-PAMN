package software.ulpgc.wherewhen.domain.usecases.chat

import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.chat.Chat
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class CreateOrGetChatUseCase(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: UUID, otherUserId: UUID): Result<Chat> {
        return try {
            if (!userRepository.existsWith(userId)) {
                throw UserNotFoundException(userId.value)
            }

            if (!userRepository.existsWith(otherUserId)) {
                throw UserNotFoundException(otherUserId.value)
            }

            chatRepository.createOrGetChat(userId, otherUserId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
