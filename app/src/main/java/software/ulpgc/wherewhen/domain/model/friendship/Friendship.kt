package software.ulpgc.wherewhen.domain.model.friendship

import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

data class Friendship(
    val id: UUID,
    val user1Id: UUID,
    val user2Id: UUID,
    val createdAt: LocalDateTime
) {
    fun contains(userId: UUID): Boolean = user1Id == userId || user2Id == userId
    
    fun getOtherUserId(userId: UUID): UUID? = when (userId) {
        user1Id -> user2Id
        user2Id -> user1Id
        else -> null
    }
}
