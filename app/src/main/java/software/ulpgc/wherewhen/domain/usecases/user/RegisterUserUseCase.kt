package software.ulpgc.wherewhen.domain.usecases.user

import software.ulpgc.wherewhen.domain.exceptions.user.InvalidUserException
import software.ulpgc.wherewhen.domain.exceptions.user.UserAlreadyExistsException
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.ports.persistence.AuthenticationRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import java.time.LocalDateTime

class RegisterUserUseCase(
    private val authRepository: AuthenticationRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: Email, name: String, password: String): Result<Profile> {
        if (authRepository.exists(email)) return Result.failure(UserAlreadyExistsException(email))
        if (name.isBlank()) return Result.failure(InvalidUserException("Name cannot be blank"))
        val userId = authRepository.register(email, password)
            .getOrElse { error ->
                return Result.failure(error)
            }
        val profile = Profile(uuid = userId, email = email, name = name, createdAt = LocalDateTime.now())
        userRepository.register(profile)
            .onFailure { error ->
                return Result.failure(error)
            }
        return Result.success(profile)
    }
}
