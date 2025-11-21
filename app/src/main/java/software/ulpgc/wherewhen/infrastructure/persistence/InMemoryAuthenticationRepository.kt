package software.ulpgc.wherewhen.infrastructure.persistence

import software.ulpgc.wherewhen.domain.persistence.repositories.AuthenticationRepository
import software.ulpgc.wherewhen.domain.services.PasswordService
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class InMemoryAuthenticationRepository(
    private val passwordService: PasswordService
) : AuthenticationRepository {

    // Almacenamiento en memoria
    private val credentials = mutableMapOf<String, Credential>()

    data class Credential(
        val userUuid: UUID,
        val email: Email,
        val hashedPassword: String
    )

    override suspend fun register(email: Email, password: String): Result<UUID> {
        return try {
            // Verificar si ya existe
            if (credentials.containsKey(email.value)) {
                return Result.failure(IllegalArgumentException("Email already registered"))
            }

            // Crear nuevo usuario
            val uuid = UUID.random()
            val hashedPassword = passwordService.hash(password)

            credentials[email.value] = Credential(uuid, email, hashedPassword)

            Result.success(uuid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: Email, password: String): Result<UUID> {
        return try {
            val credential = credentials[email.value]
                ?: return Result.failure(IllegalArgumentException("Invalid credentials"))

            // Verificar contraseña
            if (!passwordService.verify(password, credential.hashedPassword)) {
                return Result.failure(IllegalArgumentException("Invalid credentials"))
            }

            Result.success(credential.userUuid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exists(email: Email): Boolean {
        return credentials.containsKey(email.value)
    }

    // Método helper para testing: pre-registrar usuarios
    fun preRegisterUser(email: Email, password: String, uuid: UUID) {
        val hashedPassword = passwordService.hash(password)
        credentials[email.value] = Credential(uuid, email, hashedPassword)
    }
}
