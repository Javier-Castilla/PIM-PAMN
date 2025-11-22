package software.ulpgc.wherewhen.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.usecases.user.GetUserUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID

data class HomeUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {
    var uiState by mutableStateOf(HomeUiState())
        private set

    fun loadUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            uiState = HomeUiState(
                isLoading = false,
                errorMessage = "No authenticated user"
            )
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = null, user = null)

        viewModelScope.launch {
            val uuidResult = UUID.parse(currentUser.uid)
            if (uuidResult.isFailure) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Invalid user ID",
                    user = null
                )
                return@launch
            }

            getUserUseCase(uuidResult.getOrThrow()).fold(
                onSuccess = { user ->
                    uiState = uiState.copy(
                        user = user,
                        isLoading = false,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load user data",
                        user = null
                    )
                }
            )
        }
    }

    fun onLogoutClick() {
        FirebaseAuth.getInstance().signOut()
        uiState = HomeUiState()
    }

    fun onRefresh() {
        loadUserData()
    }
}
