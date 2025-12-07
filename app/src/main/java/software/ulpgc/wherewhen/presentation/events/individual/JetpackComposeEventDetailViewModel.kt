package software.ulpgc.wherewhen.presentation.events.individual

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.exceptions.events.AlreadyAttendingEventException
import software.ulpgc.wherewhen.domain.exceptions.events.EventFullException
import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.events.NotAttendingEventException
import software.ulpgc.wherewhen.domain.exceptions.events.UnauthorizedEventAccessException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventSource
import software.ulpgc.wherewhen.domain.model.events.EventStatus
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import software.ulpgc.wherewhen.domain.usecases.events.DeleteUserEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetEventAttendeesUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetEventByIdUseCase
import software.ulpgc.wherewhen.domain.usecases.events.JoinEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.LeaveEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.UpdateUserEventStatusUseCase
import software.ulpgc.wherewhen.domain.usecases.user.GetUserUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.viewModels.EventDetailViewModel

class JetpackComposeEventDetailViewModel(
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val joinEventUseCase: JoinEventUseCase,
    private val leaveEventUseCase: LeaveEventUseCase,
    private val getEventAttendeesUseCase: GetEventAttendeesUseCase,
    private val deleteUserEventUseCase: DeleteUserEventUseCase,
    private val updateEventStatusUseCase: UpdateUserEventStatusUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val locationService: LocationService
) : ViewModel(), EventDetailViewModel {

    data class AttendeeUi(
        val id: UUID,
        val uuidValue: String,
        val name: String,
        val profileImageUrl: String?,
        val isOrganizer: Boolean,
        val isCurrentUser: Boolean
    )

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val event: Event,
            val isAttending: Boolean,
            val attendees: List<AttendeeUi>,
            val isOrganizer: Boolean,
            val isFull: Boolean,
            val organizerName: String? = null
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    var uiState: UiState by mutableStateOf<UiState>(UiState.Loading)
        private set

    var isJoining by mutableStateOf(false)
        private set

    var showDeleteDialog by mutableStateOf(false)
        private set

    var inlineErrorMessage by mutableStateOf<String?>(null)
        private set

    var showAttendeesDialog by mutableStateOf(false)
        private set

    var showStatusDialog by mutableStateOf(false)
        private set

    private var currentEventId: UUID? = null
    private var eventObserverJob: Job? = null
    private var onKnownCancelledCallback: ((UUID) -> Unit)? = null

    fun setOnKnownCancelledCallback(callback: (UUID) -> Unit) {
        onKnownCancelledCallback = callback
    }

    override fun loadEvent(eventId: UUID) {
        currentEventId = eventId
        eventObserverJob?.cancel()
        eventObserverJob = viewModelScope.launch {
            uiState = UiState.Loading
            isJoining = false

            val result = getEventByIdUseCase(eventId)
            if (result.isFailure) {
                uiState = UiState.Error("Event not found")
                return@launch
            }

            val event = result.getOrNull()!!
            if (event.source == EventSource.EXTERNAL_API) {
                val enriched = enrichWithDistance(event)
                uiState = UiState.Success(
                    event = enriched,
                    isAttending = false,
                    attendees = emptyList(),
                    isOrganizer = false,
                    isFull = false,
                    organizerName = null
                )
                if (enriched.status == EventStatus.CANCELLED) {
                    onKnownCancelledCallback?.invoke(enriched.id)
                }
                isJoining = false
                return@launch
            }

            getEventByIdUseCase.observe(eventId).collect { observed ->
                if (observed != null) {
                    val enriched = enrichWithDistance(observed)
                    loadAttendeesWithEvent(enriched)
                } else {
                    uiState = UiState.Error("Event not found")
                }
            }
        }
    }

    override fun onJoinEvent() {
        val eventId = currentEventId ?: return
        val state = uiState
        if (state is UiState.Success && state.event.source == EventSource.EXTERNAL_API) return
        val userId = getCurrentUserId() ?: return

        if (state is UiState.Success && state.isFull) {
            inlineErrorMessage = "Event is full"
            return
        }

        viewModelScope.launch {
            isJoining = true
            inlineErrorMessage = null

            joinEventUseCase(eventId, userId).fold(
                onSuccess = {
                    isJoining = false
                },
                onFailure = { exception ->
                    if (exception is EventFullException) {
                        inlineErrorMessage = handleException(exception)
                    } else {
                        uiState = UiState.Error(handleException(exception))
                    }
                    isJoining = false
                }
            )
        }
    }

    override fun onLeaveEvent() {
        val eventId = currentEventId ?: return
        val state = uiState
        if (state is UiState.Success && state.event.source == EventSource.EXTERNAL_API) return
        val userId = getCurrentUserId() ?: return

        viewModelScope.launch {
            isJoining = true
            inlineErrorMessage = null

            leaveEventUseCase(eventId, userId).fold(
                onSuccess = {
                    isJoining = false
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                    isJoining = false
                }
            )
        }
    }

    fun onDeleteEvent(onDeleted: () -> Unit) {
        val eventId = currentEventId ?: return
        val state = uiState
        if (state is UiState.Success && state.event.source == EventSource.EXTERNAL_API) return

        viewModelScope.launch {
            deleteUserEventUseCase(eventId).fold(
                onSuccess = {
                    onDeleted()
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    fun onUpdateStatus(newStatus: EventStatus) {
        val eventId = currentEventId ?: return
        val state = uiState
        if (state is UiState.Success && state.event.source == EventSource.EXTERNAL_API) return
        val userId = getCurrentUserId() ?: return

        viewModelScope.launch {
            updateEventStatusUseCase(eventId, newStatus, userId).fold(
                onSuccess = {
                    dismissStatusDialog()
                },
                onFailure = { exception ->
                    inlineErrorMessage = handleException(exception)
                    dismissStatusDialog()
                }
            )
        }
    }

    fun showDeleteConfirmation() {
        showDeleteDialog = true
    }

    fun dismissDeleteDialog() {
        showDeleteDialog = false
    }

    fun openAttendeesDialog() {
        showAttendeesDialog = true
    }

    fun dismissAttendeesDialog() {
        showAttendeesDialog = false
    }

    fun openAttendeesAfterReturn() {
        showAttendeesDialog = true
    }

    fun showStatusDialog() {
        showStatusDialog = true
    }

    fun dismissStatusDialog() {
        showStatusDialog = false
    }

    override fun loadAttendees() {
        val eventId = currentEventId ?: return
        viewModelScope.launch {
            getEventByIdUseCase(eventId).fold(
                onSuccess = { event ->
                    if (event.source == EventSource.EXTERNAL_API) {
                        val enriched = enrichWithDistance(event)
                        uiState = UiState.Success(
                            event = enriched,
                            isAttending = false,
                            attendees = emptyList(),
                            isOrganizer = false,
                            isFull = false,
                            organizerName = null
                        )
                        isJoining = false
                    } else {
                        val enriched = enrichWithDistance(event)
                        loadAttendeesWithEvent(enriched)
                    }
                },
                onFailure = {
                    uiState = UiState.Error("Event not found")
                }
            )
        }
    }

    private suspend fun loadAttendeesWithEvent(event: Event) {
        if (event.source == EventSource.EXTERNAL_API) {
            val enriched = enrichWithDistance(event)
            uiState = UiState.Success(
                event = enriched,
                isAttending = false,
                attendees = emptyList(),
                isOrganizer = false,
                isFull = false,
                organizerName = null
            )
            if (enriched.status == EventStatus.CANCELLED) {
                onKnownCancelledCallback?.invoke(enriched.id)
            }
            isJoining = false
            return
        }

        val eventId = event.id
        val currentUserId = getCurrentUserId() ?: return

        val attendeesResult = getEventAttendeesUseCase(eventId)
        if (attendeesResult.isSuccess) {
            val attendeeIds = attendeesResult.getOrNull()!!
            val attendees = attendeeIds.mapNotNull { attendeeId ->
                getUserUseCase(attendeeId).getOrNull()?.let { user ->
                    AttendeeUi(
                        id = attendeeId,
                        uuidValue = user.uuid.value,
                        name = user.name,
                        profileImageUrl = user.profileImageUrl,
                        isOrganizer = event.organizerId == attendeeId,
                        isCurrentUser = attendeeId == currentUserId
                    )
                }
            }

            val isAttending = attendees.any { it.id == currentUserId }
            val isOrganizer = event.organizerId == currentUserId
            val isFull = event.isFull(attendees.size)

            val organizerName = if (isOrganizer) {
                "You"
            } else {
                event.organizerId?.let { organizerId ->
                    attendees.firstOrNull { a -> a.id == organizerId }?.name
                }
            }

            val enriched = enrichWithDistance(event)
            uiState = UiState.Success(
                event = enriched,
                isAttending = isAttending,
                attendees = attendees,
                isOrganizer = isOrganizer,
                isFull = isFull,
                organizerName = organizerName
            )
        } else {
            val isOrganizer = event.organizerId == currentUserId
            val organizerName = if (isOrganizer) "You" else null
            val enriched = enrichWithDistance(event)
            uiState = UiState.Success(
                event = enriched,
                isAttending = false,
                attendees = emptyList(),
                isOrganizer = isOrganizer,
                isFull = false,
                organizerName = organizerName
            )
        }

        if (event.status == EventStatus.CANCELLED) {
            onKnownCancelledCallback?.invoke(event.id)
        }
        isJoining = false
    }

    private fun getCurrentUserId(): UUID? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return null
        return UUID.parse(firebaseUser.uid).getOrNull()
    }

    private fun handleException(exception: Throwable): String {
        return when (exception) {
            is EventNotFoundException -> "Event not found"
            is EventFullException -> "Event is full"
            is AlreadyAttendingEventException -> "You are already attending this event"
            is NotAttendingEventException -> "You are not attending this event"
            is UnauthorizedEventAccessException -> "You have no permission to access"
            else -> "Error processing request: ${exception.message}"
        }
    }

    private suspend fun enrichWithDistance(event: Event): Event {
        val locationResult = locationService.getCurrentLocation()
        if (locationResult.isFailure) return event

        val userLocation = locationResult.getOrNull() ?: return event
        val userLat = userLocation.latitude
        val userLon = userLocation.longitude
        val lat = event.location.latitude
        val lon = event.location.longitude

        if (userLat == null || userLon == null || lat == null || lon == null) {
            return event
        }

        val distance = calculateDistance(userLat, userLon, lat, lon)
        return event.copy(distance = distance)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    override fun onCleared() {
        super.onCleared()
        eventObserverJob?.cancel()
    }
}
