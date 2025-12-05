package software.ulpgc.wherewhen.domain.model.user

import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

data class User(
    val uuid: UUID,
    val name: String,
    val description: String = "",
    val profileImageUrl: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
