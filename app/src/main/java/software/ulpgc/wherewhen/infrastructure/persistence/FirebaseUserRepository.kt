package software.ulpgc.wherewhen.infrastructure.persistence

import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.persistence.repositories.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class FirebaseUserRepository : UserRepository {
    override suspend fun register(user: User): Result<User> {
        TODO("Not yet implemented")
    }

    override suspend fun getWith(uuid: UUID): Result<User?> {
        TODO("Not yet implemented")
    }

    override suspend fun getWith(email: Email): Result<User?> {
        TODO("Not yet implemented")
    }

    override suspend fun update(user: User): Result<User> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(uuid: UUID): Result<User> {
        TODO("Not yet implemented")
    }

    override suspend fun existsWith(email: Email): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun existsWith(uuid: UUID): Boolean {
        TODO("Not yet implemented")
    }
}