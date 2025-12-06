package software.ulpgc.wherewhen.presentation.events.individual

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.exceptions.events.AlreadyAttendingEventException
import software.ulpgc.wherewhen.domain.exceptions.events.EventFullException
import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.events.NotAttendingEventException
import software.ulpgc.wherewhen.domain.exceptions.events.UnauthorizedEventAccessException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventStatus
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
    private val getUserUseCase: GetUserUseCase
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

    var uiState: UiState by mutableStateOf(UiState.Loading)
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

    override fun loadEvent(eventId: UUID) {
        currentEventId = eventId
        viewModelScope.launch {
            uiState = UiState.Loading
            isJoining = false
            Log.d("EventDetailViewModel", "Loading event $eventId")
            getEventByIdUseCase(eventId).fold(
                onSuccess = { event ->
                    Log.d("EventDetailViewModel", "Event loaded ${event.title}")
                    loadAttendeesWithEvent(event)
                },
                onFailure = { exception ->
                    Log.e("EventDetailViewModel", "Error loading event", exception)
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    override fun onJoinEvent() {
        val eventId = currentEventId ?: return
        val userId = getCurrentUserId() ?: return
        val state = uiState
        if (state is UiState.Success && state.isFull) {
            inlineErrorMessage = "Event is full"
            return
        }

        viewModelScope.launch {
            isJoining = true
            inlineErrorMessage = null
            Log.d("EventDetailViewModel", "Joining event $eventId")
            joinEventUseCase(eventId, userId).fold(
                onSuccess = {
                    Log.d("EventDetailViewModel", "Joined correctly")
                    loadEvent(eventId)
                },
                onFailure = { exception ->
                    Log.e("EventDetailViewModel", "Error joining", exception)
                    if (exception is EventFullException) {
                        inlineErrorMessage = handleException(exception)
                        loadEvent(eventId)
                    } else {
                        uiState = UiState.Error(handleException(exception))
                        isJoining = false
                    }
                }
            )
        }
    }

    override fun onLeaveEvent() {
        val eventId = currentEventId ?: return
        val userId = getCurrentUserId() ?: return

        viewModelScope.launch {
            isJoining = true
            inlineErrorMessage = null
            Log.d("EventDetailViewModel", "Leaving event $eventId")
            leaveEventUseCase(eventId, userId).fold(
                onSuccess = {
                    Log.d("EventDetailViewModel", "Left correctly")
                    loadEvent(eventId)
                },
                onFailure = { exception ->
                    Log.e("EventDetailViewModel", "Error leaving", exception)
                    uiState = UiState.Error(handleException(exception))
                    isJoining = false
                }
            )
        }
    }

    fun onDeleteEvent(onDeleted: () -> Unit) {
        val eventId = currentEventId ?: return
        viewModelScope.launch {
            Log.d("EventDetailViewModel", "Deleting event $eventId")
            deleteUserEventUseCase(eventId).fold(
                onSuccess = {
                    Log.d("EventDetailViewModel", "Event deleted")
                    onDeleted()
                },
                onFailure = { exception ->
                    Log.e("EventDetailViewModel", "Error deleting", exception)
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    fun onUpdateStatus(newStatus: EventStatus) {
        val eventId = currentEventId ?: return
        val userId = getCurrentUserId() ?: return

        viewModelScope.launch {
            Log.d("EventDetailViewModel", "Updating event status to $newStatus")
            updateEventStatusUseCase(eventId, newStatus, userId).fold(
                onSuccess = { updatedEvent ->
                    Log.d("EventDetailViewModel", "Status updated successfully")
                    val state = uiState
                    if (state is UiState.Success) {
                        uiState = state.copy(event = updatedEvent)
                    }
                    dismissStatusDialog()
                },
                onFailure = { exception ->
                    Log.e("EventDetailViewModel", "Error updating status", exception)
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
                    loadAttendeesWithEvent(event)
                },
                onFailure = { }
            )
        }
    }

    private suspend fun loadAttendeesWithEvent(event: Event) {
        val eventId = event.id
        val currentUserId = getCurrentUserId() ?: return

        Log.d("EventDetailViewModel", "Loading attendees for $eventId")
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
            Log.d(
                "EventDetailViewModel",
                "Attendees ${attendees.size}, isAttending=$isAttending, isOrganizer=$isOrganizer, isFull=$isFull"
            )

            val organizerName = if (isOrganizer) {
                "You"
            } else {
                event.organizerId?.let { organizerId ->
                    attendees.firstOrNull { a -> a.id == organizerId }?.name
                }
            }

            uiState = UiState.Success(
                event = event,
                isAttending = isAttending,
                attendees = attendees,
                isOrganizer = isOrganizer,
                isFull = isFull,
                organizerName = organizerName
            )
        } else {
            Log.e(
                "EventDetailViewModel",
                "Error loading attendees",
                attendeesResult.exceptionOrNull()
            )

            val isOrganizer = event.organizerId == currentUserId
            val organizerName = if (isOrganizer) "You" else null

            uiState = UiState.Success(
                event = event,
                isAttending = false,
                attendees = emptyList(),
                isOrganizer = isOrganizer,
                isFull = false,
                organizerName = organizerName
            )
        }
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
}
