package software.ulpgc.wherewhen.domain.model.chat

import software.ulpgc.wherewhen.domain.model.user.User
import java.time.Instant

data class ChatWithUser(
    val chat: Chat,
    val otherUser: User,
    val lastMessage: String?,
    val lastMessageAt: Instant?,
    val unreadCount: Int
)
