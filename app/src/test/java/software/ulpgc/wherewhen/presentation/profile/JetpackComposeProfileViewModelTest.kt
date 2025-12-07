package software.ulpgc.wherewhen.presentation.profile

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.usecases.user.GetUserUseCase
import software.ulpgc.wherewhen.domain.usecases.user.UpdateProfileImageUseCase
import software.ulpgc.wherewhen.domain.usecases.user.UpdateUserProfileDTO
import software.ulpgc.wherewhen.domain.usecases.user.UpdateUserProfileUseCase
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackComposeProfileViewModelTest {

    private lateinit var getUserUseCase: GetUserUseCase
    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase
    private lateinit var updateProfileImageUseCase: UpdateProfileImageUseCase
    private lateinit var viewModel: JetpackComposeProfileViewModel

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private val dispatcher = UnconfinedTestDispatcher()

    private val userUuid: UUID = UUID.random()
    private val userIdString: String = userUuid.toString()
    private val email: Email = Email.create("test@example.com").getOrThrow()
    private val profile: Profile = Profile(
        uuid = userUuid,
        email = email,
        name = "Test User",
        description = "Bio"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        mockkStatic(FirebaseAuth::class)
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userIdString

        getUserUseCase = mockk()
        updateUserProfileUseCase = mockk()
        updateProfileImageUseCase = mockk()

        coEvery { getUserUseCase.invoke(userUuid) } returns Result.success(profile)

        viewModel = JetpackComposeProfileViewModel(
            getUserUseCase,
            updateUserProfileUseCase,
            updateProfileImageUseCase
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads profile when user authenticated`() {
        val state = viewModel.uiState

        assertEquals(profile, state.profile)
        assertEquals("Test User", state.editName)
        assertEquals("Bio", state.editDescription)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `init with no authenticated user sets error`() {
        unmockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseAuth::class)

        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns null

        val vm = JetpackComposeProfileViewModel(
            getUserUseCase,
            updateUserProfileUseCase,
            updateProfileImageUseCase
        )

        val state = vm.uiState
        assertNull(state.profile)
        assertEquals("No authenticated user", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `startEdit copies profile data to edit fields`() {
        viewModel.showUserProfile(profile)

        viewModel.startEdit()

        val state = viewModel.uiState
        assertEquals(profile.name, state.editName)
        assertEquals(profile.description, state.editDescription)
        assertNull(state.selectedImageUri)
    }

    @Test
    fun `cancelEdit restores edit fields from profile`() {
        viewModel.showUserProfile(profile)
        viewModel.startEdit()

        viewModel.onNameChange("Changed")
        viewModel.onDescriptionChange("Changed bio")

        viewModel.cancelEdit()

        val state = viewModel.uiState
        assertEquals(profile.name, state.editName)
        assertEquals(profile.description, state.editDescription)
        assertNull(state.selectedImageUri)
    }

    @Test
    fun `onNameChange updates editName and clears error`() {
        viewModel.showError("error")

        viewModel.onNameChange("New Name")

        val state = viewModel.uiState
        assertEquals("New Name", state.editName)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onDescriptionChange updates editDescription and clears error`() {
        viewModel.showError("error")

        viewModel.onDescriptionChange("New Bio")

        val state = viewModel.uiState
        assertEquals("New Bio", state.editDescription)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onImageSelected sets selectedImageUri and clears error`() {
        viewModel.showError("error")
        val uri = mockk<Uri>()

        viewModel.onImageSelected(uri)

        val state = viewModel.uiState
        assertEquals(uri, state.selectedImageUri)
        assertNull(state.errorMessage)
    }

    @Test
    fun `saveProfile with no changes does nothing`() {
        viewModel.showUserProfile(profile)

        viewModel.saveProfile()

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertFalse(state.isUploadingImage)

        coVerify(exactly = 0) { updateUserProfileUseCase.invoke(any(), any()) }
        coVerify(exactly = 0) { updateProfileImageUseCase.invoke(any(), any()) }
    }

    @Test
    fun `saveProfile with text changes updates profile without uploading image`() {
        viewModel.showUserProfile(profile)
        viewModel.onNameChange("New Name")
        viewModel.onDescriptionChange("New Bio")

        coEvery {
            updateUserProfileUseCase.invoke(
                userUuid,
                any<UpdateUserProfileDTO>()
            )
        } returns Result.success(profile)

        viewModel.saveProfile()

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertFalse(state.isUploadingImage)
        assertNull(state.errorMessage)

        coVerify(exactly = 1) {
            updateUserProfileUseCase.invoke(
                userUuid,
                match {
                    it.name == "New Name" && it.description == "New Bio"
                }
            )
        }
        coVerify(exactly = 0) { updateProfileImageUseCase.invoke(any(), any()) }
    }

    @Test
    fun `saveProfile with image selected uploads image then updates profile`() {
        viewModel.showUserProfile(profile)
        viewModel.onNameChange("New Name")
        viewModel.onDescriptionChange("New Bio")
        val uri = mockk<Uri>()
        viewModel.onImageSelected(uri)

        coEvery { updateProfileImageUseCase.invoke(userUuid, uri) } returns Result.success(any())
        coEvery {
            updateUserProfileUseCase.invoke(
                userUuid,
                any<UpdateUserProfileDTO>()
            )
        } returns Result.success(profile)

        viewModel.saveProfile()

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertFalse(state.isUploadingImage)
        assertNull(state.errorMessage)

        coVerify(exactly = 1) { updateProfileImageUseCase.invoke(userUuid, uri) }
        coVerify(exactly = 1) {
            updateUserProfileUseCase.invoke(
                userUuid,
                match {
                    it.name == "New Name" && it.description == "New Bio"
                }
            )
        }
    }

    @Test
    fun `showError sets error message and stops loading and uploading`() {
        viewModel.showLoading()

        viewModel.showError("error msg")

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertFalse(state.isUploadingImage)
        assertEquals("error msg", state.errorMessage)
    }

    @Test
    fun `showUpdateSuccess stops loading and clears error and reloads profile`() {
        coEvery { getUserUseCase.invoke(userUuid) } returns Result.success(profile)

        viewModel.showLoading()
        viewModel.showUpdateSuccess()

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(profile, state.profile)
    }
}
