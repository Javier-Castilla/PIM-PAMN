package software.ulpgc.wherewhen.domain.usecases.user

import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.persistence.repositories.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetUserUseCase(private val repository: UserRepository) {

    suspend operator fun invoke(uuid: UUID): Result<User> {
        return repository.getWith(uuid).mapCatching { user ->
            user ?: throw UserNotFoundException(uuid)
        }
    }

    suspend operator fun invoke(email: Email): Result<User> {
        return repository.getWith(email).mapCatching { user ->
            user ?: throw UserNotFoundException(email)
        }
    }
}
