package software.ulpgc.wherewhen.presentation.events.individual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import software.ulpgc.wherewhen.domain.usecases.events.DeleteUserEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetEventAttendeesUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetEventByIdUseCase
import software.ulpgc.wherewhen.domain.usecases.events.JoinEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.LeaveEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.UpdateUserEventStatusUseCase
import software.ulpgc.wherewhen.domain.usecases.user.GetUserUseCase

class EventDetailViewModelFactory(
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val joinEventUseCase: JoinEventUseCase,
    private val leaveEventUseCase: LeaveEventUseCase,
    private val getEventAttendeesUseCase: GetEventAttendeesUseCase,
    private val deleteUserEventUseCase: DeleteUserEventUseCase,
    private val updateEventStatusUseCase: UpdateUserEventStatusUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val locationService: LocationService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JetpackComposeEventDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JetpackComposeEventDetailViewModel(
                getEventByIdUseCase = getEventByIdUseCase,
                joinEventUseCase = joinEventUseCase,
                leaveEventUseCase = leaveEventUseCase,
                getEventAttendeesUseCase = getEventAttendeesUseCase,
                deleteUserEventUseCase = deleteUserEventUseCase,
                updateEventStatusUseCase = updateEventStatusUseCase,
                getUserUseCase = getUserUseCase,
                locationService = locationService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
