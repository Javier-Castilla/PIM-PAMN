package software.ulpgc.wherewhen.domain.usecases.user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class UpdateUserProfileUseCaseTest {

    private lateinit var repository: UserRepository
    private lateinit var useCase: UpdateUserProfileUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = UpdateUserProfileUseCase(repository)
    }

    @Test
    fun `updates only provided fields`() = runTest {
        val uuid = UUID.random()
        val originalEmail = Email.create("original@example.com").getOrThrow()
        val original = Profile(
            uuid = uuid,
            email = originalEmail,
            name = "Original Name",
            description = "Original description"
        )
        val dto = UpdateUserProfileDTO(
            name = "New Name",
            email = null,
            description = "New description"
        )
        val updatedFromRepo = mockk<Profile>()
        val captured = slot<Profile>()

        coEvery { repository.getWith(uuid) } returns Result.success(original)
        coEvery { repository.update(capture(captured)) } returns Result.success(updatedFromRepo)

        val result = useCase.invoke(uuid, dto)

        assertTrue(result.isSuccess)
        assertEquals(updatedFromRepo, result.getOrNull())

        val updated = captured.captured
        assertEquals("New Name", updated.name)
        assertEquals(originalEmail, updated.email)
        assertEquals("New description", updated.description)

        coVerify { repository.getWith(uuid) }
        coVerify { repository.update(any<Profile>()) }
    }

    @Test
    fun `fails when user does not exist`() = runTest {
        val uuid = UUID.random()
        val dto = UpdateUserProfileDTO(name = "New Name")

        coEvery { repository.getWith(uuid) } returns Result.failure(UserNotFoundException(uuid))

        val result = useCase.invoke(uuid, dto)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UserNotFoundException)

        coVerify { repository.getWith(uuid) }
        coVerify(exactly = 0) { repository.update(any<Profile>()) }
    }
}
