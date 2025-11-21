package software.ulpgc.wherewhen.infrastructure.persistence

import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.persistence.repositories.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class InMemoryUserRepository : UserRepository {
    
    // Almacenamiento en memoria
    private val users = mutableMapOf<String, User>()
    
    override suspend fun register(user: User): Result<User> {
        return try {
            users[user.uuid.value] = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getWith(uuid: UUID): Result<User?> {
        return try {
            val user = users[uuid.value]
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getWith(email: Email): Result<User?> {
        return try {
            val user = users.values.find { it.email.value == email.value }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun update(user: User): Result<User> {
        return try {
            if (!users.containsKey(user.uuid.value)) {
                return Result.failure(IllegalArgumentException("User not found"))
            }
            users[user.uuid.value] = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun delete(uuid: UUID): Result<User> {
        return try {
            val user = users.remove(uuid.value)
                ?: return Result.failure(IllegalArgumentException("User not found"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun existsWith(email: Email): Boolean {
        return users.values.any { it.email.value == email.value }
    }
    
    override suspend fun existsWith(uuid: UUID): Boolean {
        return users.containsKey(uuid.value)
    }
    
    // Método helper para testing: pre-cargar usuarios
    fun preloadUser(user: User) {
        users[user.uuid.value] = user
    }
}
