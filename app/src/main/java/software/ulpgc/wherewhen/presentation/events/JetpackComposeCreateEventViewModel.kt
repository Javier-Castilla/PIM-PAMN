package software.ulpgc.wherewhen.presentation.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.exceptions.events.InvalidEventException
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import software.ulpgc.wherewhen.domain.usecases.events.CreateUserEventUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.viewModels.CreateEventViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class JetpackComposeCreateEventViewModel(
    private val createUserEventUseCase: CreateUserEventUseCase,
    private val locationService: LocationService
) : ViewModel(), CreateEventViewModel {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    var title by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var selectedCategory by mutableStateOf(EventCategory.OTHER)
        private set

    var selectedDate by mutableStateOf(LocalDate.now().plusDays(1))
        private set

    var selectedTime by mutableStateOf(LocalTime.of(18, 0))
        private set

    var selectedEndDate by mutableStateOf<LocalDate?>(null)
        private set

    var selectedEndTime by mutableStateOf<LocalTime?>(null)
        private set

    var maxAttendees by mutableStateOf("")
        private set

    var locationAddress by mutableStateOf("")
        private set

    var currentLocation by mutableStateOf<Location?>(null)
        private set

    var useCurrentLocation by mutableStateOf(true)
        private set

    init {
        loadCurrentLocation()
    }

    override fun onTitleChange(value: String) {
        title = value
    }

    override fun onDescriptionChange(value: String) {
        description = value
    }

    override fun onCategoryChange(category: EventCategory) {
        selectedCategory = category
    }

    override fun onDateChange(date: LocalDate) {
        selectedDate = date
    }

    override fun onTimeChange(time: LocalTime) {
        selectedTime = time
    }

    override fun onEndDateChange(date: LocalDate?) {
        selectedEndDate = date
    }

    override fun onEndTimeChange(time: LocalTime?) {
        selectedEndTime = time
    }

    override fun onMaxAttendeesChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            maxAttendees = value
        }
    }

    override fun onLocationAddressChange(value: String) {
        locationAddress = value
    }

    override fun onUseCurrentLocationChange(value: Boolean) {
        useCurrentLocation = value
        if (value) {
            loadCurrentLocation()
        }
    }

    private fun loadCurrentLocation() {
        viewModelScope.launch {
            locationService.getCurrentLocation().fold(
                onSuccess = { location ->
                    currentLocation = location
                    if (useCurrentLocation) {
                        locationAddress = location.formatAddress()
                    }
                },
                onFailure = {
                    currentLocation = null
                }
            )
        }
    }

    override fun createEvent(onSuccess: () -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            uiState = UiState.Error("User not authenticated")
            return
        }

        if (useCurrentLocation && currentLocation == null) {
            uiState = UiState.Error("Current location not available. Please wait or enter address manually.")
            return
        }

        val location = if (useCurrentLocation && currentLocation != null) {
            currentLocation!!
        } else if (locationAddress.isNotBlank()) {
            Location(
                latitude = 0.0,
                longitude = 0.0,
                address = locationAddress,
                placeName = null,
                city = null,
                country = null
            )
        } else {
            uiState = UiState.Error("Please provide a location")
            return
        }

        val dateTime = LocalDateTime.of(selectedDate, selectedTime)
        val endDateTime = if (selectedEndDate != null && selectedEndTime != null) {
            LocalDateTime.of(selectedEndDate, selectedEndTime)
        } else null

        val maxAttendeesInt = maxAttendees.toIntOrNull()

        viewModelScope.launch {
            uiState = UiState.Loading
            createUserEventUseCase(
                title = title,
                description = description.takeIf { it.isNotBlank() },
                location = location,
                dateTime = dateTime,
                endDateTime = endDateTime,
                category = selectedCategory,
                organizerId = userId,
                maxAttendees = maxAttendeesInt
            ).fold(
                onSuccess = {
                    uiState = UiState.Success
                    onSuccess()
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    override fun clearError() {
        uiState = UiState.Idle
    }

    private fun getCurrentUserId(): UUID? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return null
        return UUID.parse(firebaseUser.uid).getOrNull()
    }

    private fun handleException(exception: Throwable): String {
        return when (exception) {
            is InvalidEventException -> exception.message ?: "Invalid event data"
            else -> "Error creating event: ${exception.message}"
        }
    }
}
