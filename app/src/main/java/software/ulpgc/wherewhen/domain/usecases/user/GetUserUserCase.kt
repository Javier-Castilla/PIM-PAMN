package software.ulpgc.wherewhen.domain.usecases.user

import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(uuid: UUID): Result<Profile> {
        return repository.getWith(uuid).mapCatching { user ->
            user ?: throw UserNotFoundException(uuid)
        }
    }

    suspend operator fun invoke(email: Email): Result<Profile> {
        return repository.getWith(email).mapCatching { user ->
            user ?: throw UserNotFoundException(email)
        }
    }
}
