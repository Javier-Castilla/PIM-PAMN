package software.ulpgc.wherewhen.domain.services

import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

interface TokenService {
    fun generateAccessToken(user: User): String
    fun generateRefreshToken(user: User): String
    fun generate(uuid: UUID, email: Email): String
    fun validate(token: String): Result<UUID>
}
