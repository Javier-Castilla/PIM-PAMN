package software.ulpgc.wherewhen.presentation.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.model.Profile
import software.ulpgc.wherewhen.domain.usecases.user.GetUserUseCase
import software.ulpgc.wherewhen.domain.usecases.user.UpdateUserProfileUseCase
import software.ulpgc.wherewhen.domain.usecases.user.UpdateUserProfileDTO
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.viewModels.ProfileViewModel

data class ProfileUiState(
    val profile: Profile? = null,
    val editName: String = "",
    val editDescription: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class JetpackComposeProfileViewModel(
    private val getUserUseCase: GetUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModel(), ProfileViewModel {
    var uiState by mutableStateOf(ProfileUiState())
        private set

    override fun showLoading() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
    }

    override fun hideLoading() {
        uiState = uiState.copy(isLoading = false)
    }

    override fun showUserProfile(profile: Profile) {
        uiState = uiState.copy(
            profile = profile,
            editName = profile.name,
            editDescription = profile.description,
            isLoading = false,
            errorMessage = null
        )
    }

    override fun showUpdateSuccess() {
        uiState = uiState.copy(isLoading = false, errorMessage = null)
        loadProfile()
    }

    override fun showError(message: String) {
        uiState = uiState.copy(isLoading = false, errorMessage = message)
    }

    fun loadProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showError("No authenticated user")
            return
        }

        showLoading()

        viewModelScope.launch {
            val uuidResult = UUID.parse(currentUser.uid)
            if (uuidResult.isFailure) {
                showError("Invalid user ID")
                return@launch
            }

            getUserUseCase(uuidResult.getOrThrow())
                .fold(
                    onSuccess = { showUserProfile(it) },
                    onFailure = { showError(it.message ?: "Failed to load profile") }
                )
        }
    }

    fun startEdit() {
        uiState.profile?.let {
            uiState = uiState.copy(
                editName = it.name,
                editDescription = it.description
            )
        }
    }

    fun cancelEdit() {
        uiState.profile?.let {
            uiState = uiState.copy(
                editName = it.name,
                editDescription = it.description
            )
        }
    }

    fun onNameChange(name: String) {
        uiState = uiState.copy(editName = name, errorMessage = null)
    }

    fun onDescriptionChange(description: String) {
        uiState = uiState.copy(editDescription = description, errorMessage = null)
    }

    fun saveProfile() {
        val profile = uiState.profile ?: return
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        if (uiState.editName == profile.name && uiState.editDescription == profile.description) {
            return
        }

        showLoading()

        viewModelScope.launch {
            val uuidResult = UUID.parse(currentUser.uid)
            if (uuidResult.isFailure) {
                showError("Invalid user ID")
                return@launch
            }

            val dto = UpdateUserProfileDTO(
                name = uiState.editName,
                description = uiState.editDescription
            )

            updateUserProfileUseCase(uuidResult.getOrThrow(), dto)
                .fold(
                    onSuccess = { showUpdateSuccess() },
                    onFailure = { showError(it.message ?: "Failed to update profile") }
                )
        }
    }
}
