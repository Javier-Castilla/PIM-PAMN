package software.ulpgc.wherewhen.domain.ports.persistence

import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

interface UserRepository {
    suspend fun register(profile: Profile): Result<Profile>
    suspend fun getWith(uuid: UUID): Result<Profile>
    suspend fun getWith(email: Email): Result<Profile>
    suspend fun getPublicUser(uuid: UUID): Result<User>
    suspend fun update(profile: Profile): Result<Profile>
    suspend fun delete(uuid: UUID): Result<Profile>
    suspend fun existsWith(email: Email): Boolean
    suspend fun existsWith(uuid: UUID): Boolean
    suspend fun searchByName(query: String): Result<List<User>>
}
