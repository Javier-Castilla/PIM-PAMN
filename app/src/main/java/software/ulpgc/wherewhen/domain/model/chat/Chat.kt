package software.ulpgc.wherewhen.domain.model.chat

import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

data class Chat(
    val id: UUID,
    val participant1Id: UUID,
    val participant2Id: UUID,
    val lastMessage: String? = null,
    val lastMessageAt: LocalDateTime? = null,
    val unreadCount1: Int = 0,
    val unreadCount2: Int = 0
) {
    fun getOtherParticipant(userId: UUID): UUID? {
        return when (userId) {
            participant1Id -> participant2Id
            participant2Id -> participant1Id
            else -> null
        }
    }

    fun isParticipant(userId: UUID): Boolean {
        return userId == participant1Id || userId == participant2Id
    }

    fun getUnreadCount(userId: UUID): Int {
        return when (userId) {
            participant1Id -> unreadCount1
            participant2Id -> unreadCount2
            else -> 0
        }
    }
}
