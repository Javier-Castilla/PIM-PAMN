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
            Log.d("EventDetailViewModel", "Cargando evento: $eventId")
            getEventByIdUseCase(eventId).fold(
                onSuccess = { event ->
                    Log.d("EventDetailViewModel", "Evento cargado: ${event.title}")
                    currentEvent = event
                    loadAttendees()
                },
                onFailure = { exception ->
                    Log.e("EventDetailViewModel", "Error cargando evento", exception)
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
            Log.d("EventDetailViewModel", "Uniéndose al evento: $eventId")
            joinEventUseCase(eventId, userId).fold(
                onSuccess = {
                    Log.d("EventDetailViewModel", "Unido correctamente")
                    loadEvent(eventId)
                },
                onFailure = { exception ->
                    Log.e("EventDetailViewModel", "Error al unirse", exception)
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
            Log.d("EventDetailViewModel", "Abandonando evento: $eventId")
            leaveEventUseCase(eventId, userId).fold(
                onSuccess = {
                    Log.d("EventDetailViewModel", "Abandonado correctamente")
                    loadEvent(eventId)
                },
                onFailure = { exception ->
                    Log.e("EventDetailViewModel", "Error al abandonar", exception)
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
            Log.d("EventDetailViewModel", "Cargando asistentes para: $eventId")
            val attendeesResult = getEventAttendeesUseCase(eventId)

            if (attendeesResult.isSuccess) {
                val attendees = attendeesResult.getOrNull()!!
                val isAttending = attendees.contains(userId)
                Log.d("EventDetailViewModel", "Asistentes: ${attendees.size}, isAttending: $isAttending")
                uiState = UiState.Success(event, isAttending, attendees.size)
            } else {
                Log.e("EventDetailViewModel", "Error cargando asistentes", attendeesResult.exceptionOrNull())
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
            is EventNotFoundException -> "Evento no encontrado"
            is EventFullException -> "El evento está lleno"
            is AlreadyAttendingEventException -> "Ya estás asistiendo a este evento"
            is NotAttendingEventException -> "No estás asistiendo a este evento"
            is UnauthorizedEventAccessException -> "No tienes permiso para acceder"
            else -> "Error al procesar la solicitud: ${exception.message}"
        }
    }
}
