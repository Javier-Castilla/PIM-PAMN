package software.ulpgc.wherewhen.domain.model.chat

import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.Instant

data class Message(
    val id: UUID,
    val chatId: UUID,
    val senderId: UUID,
    val content: String,
    val timestamp: Instant,
    val isRead: Boolean = false
)
