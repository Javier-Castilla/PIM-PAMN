package software.ulpgc.wherewhen.domain.persistence.repositories

import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

interface UserRepository {
    suspend fun register(user: User): Result<Unit>
    suspend fun getWith(uuid: UUID): Result<User?>
    suspend fun getWith(email: Email): Result<User?>
    suspend fun update(user: User): Result<Unit>
    suspend fun delete(uuid: UUID): Result<Unit>
    suspend fun exists(email: Email): Boolean
}
