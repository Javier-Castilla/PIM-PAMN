package software.ulpgc.wherewhen.domain.usecases.user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.user.InvalidUserException
import software.ulpgc.wherewhen.domain.exceptions.user.UserAlreadyExistsException
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.ports.persistence.AuthenticationRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class RegisterUserUseCaseTest {

    private lateinit var authRepo: AuthenticationRepository
    private lateinit var userRepo: UserRepository
    private lateinit var useCase: RegisterUserUseCase

    @Before
    fun setup() {
        authRepo = mockk()
        userRepo = mockk()
        useCase = RegisterUserUseCase(authRepo, userRepo)
    }

    @Test
    fun `successful registration returns profile`() = runTest {
        val email = Email.create("test@example.com").getOrThrow()
        val name = "Test User"
        val password = "password123"
        val userId = UUID.random()

        coEvery { authRepo.exists(email) } returns false
        coEvery { authRepo.register(email, password) } returns Result.success(userId)
        coEvery { userRepo.register(any()) } returns Result.success(Profile(userId, email, name))

        val result = useCase.invoke(email, name, password)

        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals(userId, profile?.uuid)
        assertEquals(email, profile?.email)
        assertEquals(name, profile?.name)

        coVerify { authRepo.exists(email) }
        coVerify { authRepo.register(email, password) }
        coVerify { userRepo.register(any()) }
    }

    @Test
    fun `registration with existing email fails`() = runTest {
        val email = Email.create("existing@example.com").getOrThrow()
        val name = "Test User"
        val password = "password123"

        coEvery { authRepo.exists(email) } returns true

        val result = useCase.invoke(email, name, password)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UserAlreadyExistsException)

        coVerify { authRepo.exists(email) }
        coVerify(exactly = 0) { authRepo.register(any(), any()) }
        coVerify(exactly = 0) { userRepo.register(any()) }
    }

    @Test
    fun `registration with blank name fails`() = runTest {
        val email = Email.create("test@example.com").getOrThrow()
        val name = ""
        val password = "password123"

        coEvery { authRepo.exists(email) } returns false

        val result = useCase.invoke(email, name, password)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidUserException)

        coVerify { authRepo.exists(email) }
        coVerify(exactly = 0) { authRepo.register(any(), any()) }
        coVerify(exactly = 0) { userRepo.register(any()) }
    }
}
