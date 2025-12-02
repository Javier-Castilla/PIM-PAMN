package software.ulpgc.wherewhen.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import software.ulpgc.wherewhen.domain.usecases.events.CreateUserEventUseCase

class CreateEventViewModelFactory(
    private val createUserEventUseCase: CreateUserEventUseCase,
    private val locationService: LocationService
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JetpackComposeCreateEventViewModel::class.java)) {
            return JetpackComposeCreateEventViewModel(
                createUserEventUseCase = createUserEventUseCase,
                locationService = locationService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
