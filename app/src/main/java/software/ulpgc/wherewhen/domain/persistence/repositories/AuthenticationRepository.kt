package software.ulpgc.wherewhen.domain.persistence.repositories

import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

interface AuthenticationRepository {
    suspend fun register(email: Email, password: String): Result<UUID>
    suspend fun login(email: Email, password: String): Result<UUID>
    suspend fun exists(email: Email): Boolean
}