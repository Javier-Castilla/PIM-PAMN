package software.ulpgc.wherewhen.domain.model

import java.time.LocalDateTime

data class ChatWithUser(
    val chat: Chat,
    val otherUser: User,
    val lastMessage: String?,
    val lastMessageAt: LocalDateTime?,
    val unreadCount: Int
)
