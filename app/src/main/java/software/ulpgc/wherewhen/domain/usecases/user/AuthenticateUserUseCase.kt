package software.ulpgc.wherewhen.domain.usecases.user

import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.ports.persistence.AuthenticationRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.services.TokenService
import software.ulpgc.wherewhen.domain.valueObjects.Email

data class AuthenticationResult(
    val profile: Profile,
    val accessToken: String
)

class AuthenticateUserUseCase(
    private val authRepository: AuthenticationRepository,
    private val userRepository: UserRepository,
    private val tokenService: TokenService
) {
    suspend operator fun invoke(email: Email, password: String): Result<AuthenticationResult> {
        val uuid = authRepository.login(email, password).getOrElse { error ->
            return Result.failure(error)
        }
        val user = userRepository.getWith(uuid).mapCatching {
            it ?: throw UserNotFoundException(uuid)
        }.getOrElse { error ->
            return Result.failure(error)
        }
        val token = tokenService.generateAccessToken(user)

        return Result.success(AuthenticationResult(user, token))
    }
}
