package software.ulpgc.wherewhen.presentation.auth.login

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.usecases.user.AuthenticateUserUseCase
import software.ulpgc.wherewhen.domain.usecases.user.AuthenticationResult
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class JetpackComposeLoginViewModelTest {

    private lateinit var authenticateUserUseCase: AuthenticateUserUseCase
    private lateinit var viewModel: JetpackComposeLoginViewModel
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authenticateUserUseCase = mockk()
        viewModel = JetpackComposeLoginViewModel(authenticateUserUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState

        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertNull(state.errorMessage)
        assertNull(state.emailError)
        assertNull(state.passwordError)
    }

    @Test
    fun `onEmailChange updates email and clears errors`() {
        viewModel.showError("msg")
        viewModel.showEmailError("error")

        viewModel.onEmailChange("test@example.com")

        val state = viewModel.uiState
        assertEquals("test@example.com", state.email)
        assertNull(state.emailError)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onPasswordChange updates password and clears errors`() {
        viewModel.showError("msg")
        viewModel.showPasswordError("error")

        viewModel.onPasswordChange("password123")

        val state = viewModel.uiState
        assertEquals("password123", state.password)
        assertNull(state.passwordError)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onLoginClick with blank email sets email error and does not call use case`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("password123")

        viewModel.onLoginClick()

        val state = viewModel.uiState
        assertEquals("Email is required", state.emailError)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { authenticateUserUseCase.invoke(any(), any()) }
    }

    @Test
    fun `onLoginClick with blank password sets password error and does not call use case`() = runTest {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("")

        viewModel.onLoginClick()

        val state = viewModel.uiState
        assertEquals("Password is required", state.passwordError)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { authenticateUserUseCase.invoke(any(), any()) }
    }

    @Test
    fun `onLoginClick with invalid email sets email error and does not call use case`() = runTest {
        viewModel.onEmailChange("invalid-email")
        viewModel.onPasswordChange("password123")

        viewModel.onLoginClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState
        assertEquals("Invalid email format", state.emailError)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { authenticateUserUseCase.invoke(any(), any()) }
    }

    @Test
    fun `onLoginClick with valid credentials and success sets success state`() = runTest {
        val email = Email.create("test@example.com").getOrThrow()
        val profile = Profile(
            uuid = UUID.random(),
            email = email,
            name = "Test User"
        )
        val authResult = AuthenticationResult(
            profile = profile,
            accessToken = "token123"
        )

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")

        coEvery { authenticateUserUseCase.invoke(email, "password123") } returns Result.success(authResult)

        viewModel.onLoginClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)

        coVerify { authenticateUserUseCase.invoke(email, "password123") }
    }

    @Test
    fun `onLoginClick with valid credentials and failure sets error message`() = runTest {
        val email = Email.create("test@example.com").getOrThrow()
        val error = RuntimeException("Invalid credentials")

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")

        coEvery { authenticateUserUseCase.invoke(email, "password123") } returns Result.failure(error)

        viewModel.onLoginClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertEquals("Invalid credentials", state.errorMessage)

        coVerify { authenticateUserUseCase.invoke(email, "password123") }
    }

    @Test
    fun `showLoading sets loading true and clears global error`() {
        viewModel.showError("error")

        viewModel.showLoading()

        val state = viewModel.uiState
        assertTrue(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `hideLoading sets loading false`() {
        viewModel.showLoading()

        viewModel.hideLoading()

        val state = viewModel.uiState
        assertFalse(state.isLoading)
    }

    @Test
    fun `showSuccess sets success true and clears error`() {
        val email = Email.create("test@example.com").getOrThrow()
        val profile = Profile(
            uuid = UUID.random(),
            email = email,
            name = "Test User"
        )

        viewModel.showError("error")
        viewModel.showLoading()

        viewModel.showSuccess(profile)

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)
    }

    @Test
    fun `showError sets error message and stops loading`() {
        viewModel.showLoading()

        viewModel.showError("error msg")

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals("error msg", state.errorMessage)
    }

    @Test
    fun `showEmailError sets emailError and stops loading`() {
        viewModel.showLoading()

        viewModel.showEmailError("email error")

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals("email error", state.emailError)
    }

    @Test
    fun `showPasswordError sets passwordError and stops loading`() {
        viewModel.showLoading()

        viewModel.showPasswordError("password error")

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals("password error", state.passwordError)
    }

    @Test
    fun `resetState restores default ui state`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password")
        viewModel.showLoading()
        viewModel.showError("error")
        viewModel.showEmailError("e")
        viewModel.showPasswordError("p")

        viewModel.resetState()

        val state = viewModel.uiState
        assertEquals(LoginUiState(), state)
    }
}
