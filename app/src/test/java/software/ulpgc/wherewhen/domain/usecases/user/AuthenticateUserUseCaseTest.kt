package software.ulpgc.wherewhen.domain.usecases.user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.ports.persistence.AuthenticationRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.services.TokenService
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class AuthenticateUserUseCaseTest {

    private lateinit var authRepository: AuthenticationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var tokenService: TokenService
    private lateinit var useCase: AuthenticateUserUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        userRepository = mockk()
        tokenService = mockk()
        useCase = AuthenticateUserUseCase(authRepository, userRepository, tokenService)
    }

    @Test
    fun `successful authentication returns AuthenticationResult`() = runTest {
        val email = Email.create("test@example.com").getOrThrow()
        val password = "password123"
        val uuid = UUID.random()
        val profile = Profile(uuid = uuid, email = email, name = "Test User")

        coEvery { authRepository.login(email, password) } returns Result.success(uuid)
        coEvery { userRepository.getWith(uuid) } returns Result.success(profile)
        io.mockk.every { tokenService.generateAccessToken(profile) } returns "token-123"

        val result = useCase.invoke(email, password)

        assertTrue(result.isSuccess)
        val authResult = result.getOrNull()
        assertNotNull(authResult)
        assertEquals(profile, authResult?.profile)
        assertEquals("token-123", authResult?.accessToken)

        coVerify { authRepository.login(email, password) }
        coVerify { userRepository.getWith(uuid) }
        verify { tokenService.generateAccessToken(profile) }
    }

    @Test
    fun `authentication fails when user not found`() = runTest {
        val email = Email.create("test@example.com").getOrThrow()
        val password = "password123"
        val uuid = UUID.random()

        coEvery { authRepository.login(email, password) } returns Result.success(uuid)
        coEvery { userRepository.getWith(uuid) } returns Result.failure(UserNotFoundException(uuid))

        val result = useCase.invoke(email, password)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UserNotFoundException)

        coVerify { authRepository.login(email, password) }
        coVerify { userRepository.getWith(uuid) }
        verify(exactly = 0) { tokenService.generateAccessToken(any<Profile>()) }
    }

    @Test
    fun `authentication propagates login error`() = runTest {
        val email = Email.create("test@example.com").getOrThrow()
        val password = "password123"
        val error = RuntimeException("login error")

        coEvery { authRepository.login(email, password) } returns Result.failure(error)

        val result = useCase.invoke(email, password)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())

        coVerify { authRepository.login(email, password) }
        coVerify(exactly = 0) { userRepository.getWith(any<UUID>()) }
        verify(exactly = 0) { tokenService.generateAccessToken(any<Profile>()) }
    }
}
