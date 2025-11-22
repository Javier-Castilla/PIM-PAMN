package software.ulpgc.wherewhen.domain.usecases.user

import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.ports.repositories.UserRepository

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
