package software.ulpgc.wherewhen.domain.usecases.user

import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

data class UpdateUserProfileDTO(
    val name: String? = null,
    val email: Email? = null,
    val description: String? = null
)

class UpdateUserProfileUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(uuid: UUID, dto: UpdateUserProfileDTO): Result<Profile> {
        val user = repository.getWith(uuid).mapCatching { it ->
            it
        }.getOrElse { error ->
            return Result.failure(error)
        }
        val updatedUser = user.copy(
            name = dto.name ?: user.name,
            email = dto.email ?: user.email,
            description = dto.description ?: user.description
        )
        return repository.update(updatedUser)
    }
}
