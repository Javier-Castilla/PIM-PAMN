package software.ulpgc.wherewhen.domain.services

import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

interface TokenService {
    fun generateAccessToken(profile: Profile): String
    fun generateRefreshToken(profile: Profile): String
    fun generate(uuid: UUID, email: Email): String
    fun validate(token: String): Result<UUID>
}
