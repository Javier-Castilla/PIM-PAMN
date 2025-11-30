package software.ulpgc.wherewhen.infrastructure.services

import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.services.TokenService
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class MockTokenService : TokenService {

    override fun generateAccessToken(profile: Profile): String {
        return "TOKEN_${profile.uuid.value}_${System.currentTimeMillis()}"
    }

    override fun generateRefreshToken(profile: Profile): String {
        return "REFRESH_${profile.uuid.value}_${System.currentTimeMillis()}"
    }

    override fun generate(uuid: UUID, email: Email): String {
        return "TOKEN_${uuid.value}_${email.value}"
    }

    override fun validate(token: String): Result<UUID> {
        return try {
            // Extraer UUID del token (formato: TOKEN_uuid_timestamp)
            val parts = token.split("_")
            if (parts.size >= 2 && parts[0] == "TOKEN") {
                UUID.parse(parts[1])  // ✅ Usa parse() que devuelve Result<UUID>
            } else {
                Result.failure(IllegalArgumentException("Invalid token format"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
