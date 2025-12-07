package software.ulpgc.wherewhen.presentation.events.form

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import software.ulpgc.wherewhen.domain.ports.storage.ImageUploadService
import software.ulpgc.wherewhen.domain.usecases.events.CreateUserEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetEventByIdUseCase
import software.ulpgc.wherewhen.domain.usecases.events.UpdateUserEventUseCase

class CreateEventViewModelFactory(
    private val application: Application,
    private val createUserEventUseCase: CreateUserEventUseCase,
    private val updateUserEventUseCase: UpdateUserEventUseCase,
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val locationService: LocationService,
    private val imageUploadService: ImageUploadService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JetpackComposeCreateEventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JetpackComposeCreateEventViewModel(
                application,
                createUserEventUseCase,
                updateUserEventUseCase,
                getEventByIdUseCase,
                locationService,
                imageUploadService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
