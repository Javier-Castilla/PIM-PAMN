package software.ulpgc.wherewhen.presentation.events

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.exceptions.events.*
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.usecases.events.*
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.viewModels.EventDetailViewModel

class JetpackComposeEventDetailViewModel(
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val joinEventUseCase: JoinEventUseCase,
    private val leaveEventUseCase: LeaveEventUseCase,
    private val getEventAttendeesUseCase: GetEventAttendeesUseCase
) : ViewModel(), EventDetailViewModel {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val event: Event, val isAttending: Boolean, val attendeesCount: Int) : UiState()
        data class Error(val message: String) : UiState()
    }

    var uiState by mutableStateOf<UiState>(UiState.Loading)
        private set

    var isJoining by mutableStateOf(false)
        private set

    private var currentEventId: UUID? = null
    private var currentEvent: Event? = null

    override fun loadEvent(eventId: UUID) {
        currentEventId = eventId
        viewModelScope.launch {
            uiState = UiState.Loading
            Log.d("EventDetailViewModel", "Loading event: $eventId")
            getEventByIdUseCase(eventId).fold(
                onSuccess = { event ->
                    Log.d("EventDetailViewModel", "Event loaded: ${event.title}")
                    currentEvent = event
                    loadAttendees()
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
        viewModelScope.launch {
            isJoining = true
            Log.d("EventDetailViewModel", "Joining event: $eventId")
            joinEventUseCase(eventId, userId).fold(
                onSuccess = {
                    Log.d("EventDetailViewModel", "Joined correctly")
                    loadEvent(eventId)
                },
                onFailure = { exception ->
                    Log.e("EventDetailViewModel", "Error joining", exception)
                    uiState = UiState.Error(handleException(exception))
                }
            )
            isJoining = false
        }
    }

    override fun onLeaveEvent() {
        val eventId = currentEventId ?: return
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            isJoining = true
            Log.d("EventDetailViewModel", "Leaving event: $eventId")
            leaveEventUseCase(eventId, userId).fold(
                onSuccess = {
                    Log.d("EventDetailViewModel", "Left correctly")
                    loadEvent(eventId)
                },
                onFailure = { exception ->
                    Log.e("EventDetailViewModel", "Error leaving", exception)
                    uiState = UiState.Error(handleException(exception))
                }
            )
            isJoining = false
        }
    }

    override fun loadAttendees() {
        val eventId = currentEventId ?: return
        val event = currentEvent ?: return
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            Log.d("EventDetailViewModel", "Loading attendees for: $eventId")
            val attendeesResult = getEventAttendeesUseCase(eventId)

            if (attendeesResult.isSuccess) {
                val attendees = attendeesResult.getOrNull()!!
                val isAttending = attendees.contains(userId)
                Log.d("EventDetailViewModel", "Attendees: ${attendees.size}, isAttending: $isAttending")
                uiState = UiState.Success(event, isAttending, attendees.size)
            } else {
                Log.e("EventDetailViewModel", "Error loading attendees", attendeesResult.exceptionOrNull())
                uiState = UiState.Success(event, false, 0)
            }
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
            is UnauthorizedEventAccessException -> "You have no permission you access"
            else -> "Error processing request: ${exception.message}"
        }
    }
}
