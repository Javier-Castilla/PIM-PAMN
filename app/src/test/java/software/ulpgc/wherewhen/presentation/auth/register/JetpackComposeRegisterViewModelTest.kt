package software.ulpgc.wherewhen.presentation.auth.register

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
import software.ulpgc.wherewhen.domain.usecases.user.RegisterUserUseCase
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class JetpackComposeRegisterViewModelTest {

    private lateinit var registerUserUseCase: RegisterUserUseCase
    private lateinit var viewModel: JetpackComposeRegisterViewModel
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        registerUserUseCase = mockk()
        viewModel = JetpackComposeRegisterViewModel(registerUserUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState

        assertEquals("", state.name)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertNull(state.errorMessage)
        assertNull(state.nameError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.confirmPasswordError)
    }

    @Test
    fun `onNameChange updates name and clears errors`() {
        viewModel.showError("error")
        viewModel.showNameError("name error")

        viewModel.onNameChange("Javi")

        val state = viewModel.uiState
        assertEquals("Javi", state.name)
        assertNull(state.nameError)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onEmailChange updates email and clears errors`() {
        viewModel.showError("error")
        viewModel.showEmailError("email error")

        viewModel.onEmailChange("test@example.com")

        val state = viewModel.uiState
        assertEquals("test@example.com", state.email)
        assertNull(state.emailError)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onPasswordChange updates password and clears errors`() {
        viewModel.showError("error")
        viewModel.showPasswordError("password error")

        viewModel.onPasswordChange("password123")

        val state = viewModel.uiState
        assertEquals("password123", state.password)
        assertNull(state.passwordError)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onConfirmPasswordChange updates confirmPassword and clears errors`() {
        viewModel.showError("error")
        viewModel.showConfirmPasswordError("confirm error")

        viewModel.onConfirmPasswordChange("password123")

        val state = viewModel.uiState
        assertEquals("password123", state.confirmPassword)
        assertNull(state.confirmPasswordError)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onRegisterClick with blank name sets name error and does not call use case`() = runTest {
        viewModel.onNameChange("")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.onRegisterClick()

        val state = viewModel.uiState
        assertEquals("Name is required", state.nameError)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { registerUserUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `onRegisterClick with blank email sets email error and does not call use case`() = runTest {
        viewModel.onNameChange("Test User")
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.onRegisterClick()

        val state = viewModel.uiState
        assertEquals("Email is required", state.emailError)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { registerUserUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `onRegisterClick with blank password sets password error and does not call use case`() = runTest {
        viewModel.onNameChange("Test User")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("")
        viewModel.onConfirmPasswordChange("")

        viewModel.onRegisterClick()

        val state = viewModel.uiState
        assertEquals("Password is required", state.passwordError)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { registerUserUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `onRegisterClick with short password sets password length error`() = runTest {
        viewModel.onNameChange("Test User")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("123")
        viewModel.onConfirmPasswordChange("123")

        viewModel.onRegisterClick()

        val state = viewModel.uiState
        assertEquals("Password must be at least 6 characters", state.passwordError)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { registerUserUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `onRegisterClick with mismatched passwords sets confirm password error`() = runTest {
        viewModel.onNameChange("Test User")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password456")

        viewModel.onRegisterClick()

        val state = viewModel.uiState
        assertEquals("Passwords do not match", state.confirmPasswordError)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { registerUserUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `onRegisterClick with invalid email sets email format error and does not call use case`() = runTest {
        viewModel.onNameChange("Test User")
        viewModel.onEmailChange("invalid-email")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.onRegisterClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState
        assertEquals("Invalid email format", state.emailError)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { registerUserUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `onRegisterClick with valid data and success sets success state`() = runTest {
        val email = Email.create("test@example.com").getOrThrow()
        val name = "Test User"
        val password = "password123"
        val profile = Profile(
            uuid = UUID.random(),
            email = email,
            name = name
        )

        viewModel.onNameChange(name)
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(password)

        coEvery { registerUserUseCase.invoke(email, name, password) } returns Result.success(profile)

        viewModel.onRegisterClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)

        coVerify { registerUserUseCase.invoke(email, name, password) }
    }

    @Test
    fun `onRegisterClick with valid data and failure sets error message`() = runTest {
        val email = Email.create("test@example.com").getOrThrow()
        val name = "Test User"
        val password = "password123"
        val error = RuntimeException("User already exists")

        viewModel.onNameChange(name)
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(password)

        coEvery { registerUserUseCase.invoke(email, name, password) } returns Result.failure(error)

        viewModel.onRegisterClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertEquals("User already exists", state.errorMessage)

        coVerify { registerUserUseCase.invoke(email, name, password) }
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
    fun `showNameError sets nameError and stops loading`() {
        viewModel.showLoading()

        viewModel.showNameError("name error")

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals("name error", state.nameError)
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
    fun `showConfirmPasswordError sets confirmPasswordError and stops loading`() {
        viewModel.showLoading()

        viewModel.showConfirmPasswordError("confirm error")

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals("confirm error", state.confirmPasswordError)
    }

    @Test
    fun `resetState restores default ui state`() {
        viewModel.onNameChange("Test User")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password")
        viewModel.onConfirmPasswordChange("password")
        viewModel.showLoading()
        viewModel.showError("error")
        viewModel.showNameError("n")
        viewModel.showEmailError("e")
        viewModel.showPasswordError("p")
        viewModel.showConfirmPasswordError("c")

        viewModel.resetState()

        val state = viewModel.uiState
        assertEquals(RegisterUiState(), state)
    }
}
