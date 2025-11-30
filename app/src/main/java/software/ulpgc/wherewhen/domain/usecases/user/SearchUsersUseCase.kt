package software.ulpgc.wherewhen.domain.usecases.user

import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository

class SearchUsersUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(query: String): Result<List<User>> {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }
        return repository.searchByName(query)
    }
}
