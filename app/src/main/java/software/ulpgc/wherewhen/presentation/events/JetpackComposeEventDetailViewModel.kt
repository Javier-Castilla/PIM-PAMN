package software.ulpgc.wherewhen.presentation.events

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

    override fun loadEvent(eventId: UUID) {
        currentEventId = eventId
        viewModelScope.launch {
            uiState = UiState.Loading
            getEventByIdUseCase(eventId).fold(
                onSuccess = { event ->
                    loadAttendees()
                },
                onFailure = { exception ->
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
            joinEventUseCase(eventId, userId).fold(
                onSuccess = {
                    loadEvent(eventId)
                },
                onFailure = { exception ->
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
            leaveEventUseCase(eventId, userId).fold(
                onSuccess = {
                    loadEvent(eventId)
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                }
            )
            isJoining = false
        }
    }

    override fun loadAttendees() {
        val eventId = currentEventId ?: return
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            val eventResult = getEventByIdUseCase(eventId)
            val attendeesResult = getEventAttendeesUseCase(eventId)
            
            if (eventResult.isSuccess && attendeesResult.isSuccess) {
                val event = eventResult.getOrNull()!!
                val attendees = attendeesResult.getOrNull()!!
                val isAttending = attendees.contains(userId)
                uiState = UiState.Success(event, isAttending, attendees.size)
            } else {
                uiState = UiState.Error("Error al cargar el evento")
            }
        }
    }

    private fun getCurrentUserId(): UUID? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return null
        return UUID.parse(firebaseUser.uid).getOrNull()
    }

    private fun handleException(exception: Throwable): String {
        return when (exception) {
            is EventNotFoundException -> "Evento no encontrado"
            is EventFullException -> "El evento está lleno"
            is AlreadyAttendingEventException -> "Ya estás asistiendo a este evento"
            is NotAttendingEventException -> "No estás asistiendo a este evento"
            is UnauthorizedEventAccessException -> "No tienes permiso para acceder"
            else -> "Error al procesar la solicitud"
        }
    }
}
