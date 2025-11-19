package software.ulpgc.wherewhen.domain.usecases.user

import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.persistence.repositories.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

data class UpdateUserProfileDTO(
    val name: String? = null,
    val email: Email? = null
) {}

class UpdateUserProfileUseCase(private val repository: UserRepository) {

    suspend operator fun invoke(uuid: UUID, dto: UpdateUserProfileDTO): Result<User> {
        val user = repository.getWith(uuid).mapCatching { it ->
            it ?: throw UserNotFoundException(uuid)
        }.getOrElse { error ->
            return Result.failure(error)
        }
        val updatedUser = user.copy(
            name = dto.name ?: user.name,
            email = dto.email ?: user.email
        )
        return repository.update(updatedUser)
    }
}