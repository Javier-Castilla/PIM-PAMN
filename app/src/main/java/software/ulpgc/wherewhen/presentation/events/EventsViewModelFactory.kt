package software.ulpgc.wherewhen.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import software.ulpgc.wherewhen.domain.usecases.events.*

class EventsViewModelFactory(
    private val searchNearbyEventsUseCase: SearchNearbyEventsUseCase,
    private val searchEventsByNameUseCase: SearchEventsByNameUseCase,
    private val searchEventsByCategoryUseCase: SearchEventsByCategoryUseCase,
    private val getUserJoinedEventsUseCase: GetUserJoinedEventsUseCase,
    private val getUserCreatedEventsUseCase: GetUserCreatedEventsUseCase,
    private val locationService: LocationService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JetpackComposeEventsViewModel::class.java)) {
            return JetpackComposeEventsViewModel(
                searchNearbyEventsUseCase,
                searchEventsByNameUseCase,
                searchEventsByCategoryUseCase,
                getUserJoinedEventsUseCase,
                getUserCreatedEventsUseCase,
                locationService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
