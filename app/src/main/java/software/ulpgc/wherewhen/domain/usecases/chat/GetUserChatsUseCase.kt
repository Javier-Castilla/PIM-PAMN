package software.ulpgc.wherewhen.domain.usecases.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import software.ulpgc.wherewhen.domain.model.chat.ChatWithUser
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetUserChatsUseCase(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) {
    operator fun invoke(userId: UUID): Flow<List<ChatWithUser>> {
        return chatRepository.observeUserChats(userId).map { chats ->
            chats.mapNotNull { chat ->
                val otherUserId = chat.getOtherParticipant(userId) ?: return@mapNotNull null
                val otherUser = userRepository.getPublicUser(otherUserId).getOrNull() ?: return@mapNotNull null

                ChatWithUser(
                    chat = chat,
                    otherUser = otherUser,
                    lastMessage = chat.lastMessage,
                    lastMessageAt = chat.lastMessageAt,
                    unreadCount = chat.getUnreadCount(userId)
                )
            }.sortedByDescending { it.lastMessageAt }
        }
    }
}
