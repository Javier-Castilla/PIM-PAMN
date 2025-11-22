package software.ulpgc.wherewhen.infrastructure.persistence

import software.ulpgc.wherewhen.domain.persistence.repositories.AuthenticationRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class FirebaseAuthenticationRepository : AuthenticationRepository {
    override suspend fun register(
        email: Email,
        password: String
    ): Result<UUID> {
        TODO("Not yet implemented")
    }

    override suspend fun login(
        email: Email,
        password: String
    ): Result<UUID> {
        TODO("Not yet implemented")
    }

    override suspend fun exists(email: Email): Boolean {
        TODO("Not yet implemented")
    }
}