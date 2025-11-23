package software.ulpgc.wherewhen.domain.model.chat

import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

data class Message(
    val id: UUID,
    val chatId: UUID,
    val senderId: UUID,
    val content: String,
    val timestamp: LocalDateTime,
    val isRead: Boolean = false
)
