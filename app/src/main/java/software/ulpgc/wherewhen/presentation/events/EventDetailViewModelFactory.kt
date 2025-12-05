package software.ulpgc.wherewhen.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.usecases.events.*

class EventDetailViewModelFactory(
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val joinEventUseCase: JoinEventUseCase,
    private val leaveEventUseCase: LeaveEventUseCase,
    private val getEventAttendeesUseCase: GetEventAttendeesUseCase,
    private val deleteUserEventUseCase: DeleteUserEventUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JetpackComposeEventDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JetpackComposeEventDetailViewModel(
                getEventByIdUseCase,
                joinEventUseCase,
                leaveEventUseCase,
                getEventAttendeesUseCase,
                deleteUserEventUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
