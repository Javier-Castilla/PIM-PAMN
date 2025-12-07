package software.ulpgc.wherewhen.domain.usecases.user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class DeleteUserUseCaseTest {

    private lateinit var repository: UserRepository
    private lateinit var useCase: DeleteUserUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = DeleteUserUseCase(repository)
    }

    @Test
    fun `fails when user does not exist`() = runTest {
        val uuid = UUID.Companion.random()

        coEvery { repository.existsWith(uuid) } returns false

        val result = useCase.invoke(uuid)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is UserNotFoundException)
        coVerify { repository.existsWith(uuid) }
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `deletes user successfully`() = runTest {
        val uuid = UUID.Companion.random()

        coEvery { repository.existsWith(uuid) } returns true
        coEvery { repository.delete(uuid) } returns Result.success(any())

        val result = useCase.invoke(uuid)

        Assert.assertTrue(result.isSuccess)
        coVerify { repository.existsWith(uuid) }
        coVerify { repository.delete(uuid) }
    }

    @Test
    fun `propagates delete error`() = runTest {
        val uuid = UUID.Companion.random()
        val error = RuntimeException("db error")

        coEvery { repository.existsWith(uuid) } returns true
        coEvery { repository.delete(uuid) } returns Result.failure(error)

        val result = useCase.invoke(uuid)

        Assert.assertTrue(result.isFailure)
        Assert.assertEquals(error, result.exceptionOrNull())
        coVerify { repository.existsWith(uuid) }
        coVerify { repository.delete(uuid) }
    }
}