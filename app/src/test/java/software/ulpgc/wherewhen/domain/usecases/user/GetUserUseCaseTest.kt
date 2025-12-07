package software.ulpgc.wherewhen.domain.usecases.user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetUserUseCaseTest {

    private lateinit var repository: UserRepository
    private lateinit var useCase: GetUserUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetUserUseCase(repository)
    }

    @Test
    fun `get user by uuid returns profile`() = runTest {
        val uuid = UUID.Companion.random()
        val profile = Profile(
            uuid = uuid,
            email = Email.Companion.create("test@example.com").getOrThrow(),
            name = "Test"
        )

        coEvery { repository.getWith(uuid) } returns Result.success(profile)

        val result = useCase.invoke(uuid)

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(profile, result.getOrNull())
        coVerify { repository.getWith(uuid) }
    }

    @Test
    fun `get user by uuid propagates error`() = runTest {
        val uuid = UUID.Companion.random()
        val error = RuntimeException("db error")

        coEvery { repository.getWith(uuid) } returns Result.failure(error)

        val result = useCase.invoke(uuid)

        Assert.assertTrue(result.isFailure)
        Assert.assertEquals(error, result.exceptionOrNull())
        coVerify { repository.getWith(uuid) }
    }

    @Test
    fun `get user by email returns profile`() = runTest {
        val email = Email.Companion.create("test@example.com").getOrThrow()
        val profile = Profile(uuid = UUID.Companion.random(), email = email, name = "Test")

        coEvery { repository.getWith(email) } returns Result.success(profile)

        val result = useCase.invoke(email)

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(profile, result.getOrNull())
        coVerify { repository.getWith(email) }
    }

    @Test
    fun `get user by email propagates error`() = runTest {
        val email = Email.Companion.create("test@example.com").getOrThrow()
        val error = RuntimeException("db error")

        coEvery { repository.getWith(email) } returns Result.failure(error)

        val result = useCase.invoke(email)

        Assert.assertTrue(result.isFailure)
        Assert.assertEquals(error, result.exceptionOrNull())
        coVerify { repository.getWith(email) }
    }
}