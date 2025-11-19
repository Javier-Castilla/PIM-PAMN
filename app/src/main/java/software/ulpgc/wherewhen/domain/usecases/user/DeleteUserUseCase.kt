package software.ulpgc.wherewhen.domain.usecases.user

import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.persistence.repositories.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class DeleteUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(uuid: UUID): Result<User> {
        if (!repository.existsWith(uuid)) return Result.failure(UserNotFoundException(uuid))
        return repository.delete(uuid)
    }
}