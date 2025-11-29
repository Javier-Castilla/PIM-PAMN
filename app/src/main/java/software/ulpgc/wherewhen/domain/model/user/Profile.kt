package software.ulpgc.wherewhen.domain.model.user

import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID;
import java.time.LocalDateTime

data class Profile(
    val uuid: UUID,
    val email: Email,
    val name: String,
    val description: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun toPublicUser() = User(
        uuid = uuid,
        name = name,
        description = description,
        createdAt = createdAt
    )
}
