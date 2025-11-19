package software.ulpgc.wherewhen.domain.persistence.repositories

import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

interface UserRepository {
    suspend fun register(user: User): Result<User>
    suspend fun getWith(uuid: UUID): Result<User?>
    suspend fun getWith(email: Email): Result<User?>
    suspend fun update(user: User): Result<User>
    suspend fun delete(uuid: UUID): Result<User>
    suspend fun existsWith(email: Email): Boolean
    suspend fun existsWith(uuid: UUID): Boolean
}
