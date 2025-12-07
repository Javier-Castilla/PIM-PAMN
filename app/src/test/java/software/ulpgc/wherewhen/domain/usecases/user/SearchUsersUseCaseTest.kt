package software.ulpgc.wherewhen.domain.usecases.user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class SearchUsersUseCaseTest {

    private lateinit var repository: UserRepository
    private lateinit var useCase: SearchUsersUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = SearchUsersUseCase(repository)
    }

    @Test
    fun `returns empty list when query is blank`() = runTest {
        val result = useCase.invoke("   ")

        assertTrue(result.isSuccess)
        val list = result.getOrNull()
        assertNotNull(list)
        assertTrue(list!!.isEmpty())
        coVerify(exactly = 0) { repository.searchByName(any()) }
    }

    @Test
    fun `delegates to repository when query is not blank`() = runTest {
        val query = "Javi"
        val users = listOf(
            User(uuid = UUID.random(), name = "Javi"),
            User(uuid = UUID.random(), name = "Javi 2")
        )

        coEvery { repository.searchByName(query) } returns Result.success(users)

        val result = useCase.invoke(query)

        assertTrue(result.isSuccess)
        assertEquals(users, result.getOrNull())
        coVerify { repository.searchByName(query) }
    }

    @Test
    fun `propagates repository error`() = runTest {
        val query = "Javi"
        val error = RuntimeException("db error")

        coEvery { repository.searchByName(query) } returns Result.failure(error)

        val result = useCase.invoke(query)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        coVerify { repository.searchByName(query) }
    }
}

