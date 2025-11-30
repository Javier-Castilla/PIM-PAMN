package software.ulpgc.wherewhen.domain.model.friendship

import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

data class FriendRequest(
    val id: UUID,
    val senderId: UUID,
    val receiverId: UUID,
    val status: FriendRequestStatus,
    val createdAt: LocalDateTime,
    val respondedAt: LocalDateTime? = null
)
