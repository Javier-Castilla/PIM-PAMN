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
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import software.ulpgc.wherewhen.domain.usecases.events.*
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.viewModels.EventsViewModel

class JetpackComposeEventsViewModel(
    private val searchNearbyEventsUseCase: SearchNearbyEventsUseCase,
    private val searchEventsByNameUseCase: SearchEventsByNameUseCase,
    private val searchEventsByCategoryUseCase: SearchEventsByCategoryUseCase,
    private val getUserJoinedEventsUseCase: GetUserJoinedEventsUseCase,
    private val getUserCreatedEventsUseCase: GetUserCreatedEventsUseCase,
    private val locationService: LocationService
) : ViewModel(), EventsViewModel {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val events: List<Event>) : UiState()
        data class Error(val message: String) : UiState()
    }

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    var selectedTab by mutableStateOf(0)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var selectedCategory by mutableStateOf<EventCategory?>(null)
        private set

    var currentLocation by mutableStateOf<Location?>(null)
        private set

    init {
        loadLocation()
        loadNearbyEvents()
    }

    override fun onTabSelected(index: Int) {
        selectedTab = index
        when (index) {
            0 -> loadNearbyEvents()
            1 -> loadMyEvents()
        }
    }

    override fun onSearchQueryChange(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            loadNearbyEvents()
        } else {
            searchByName(query)
        }
    }

    override fun onCategorySelected(category: EventCategory?) {
        selectedCategory = category
        if (category == null) {
            if (searchQuery.isBlank()) {
                loadNearbyEvents()
            } else {
                searchByName(searchQuery)
            }
        } else {
            searchByCategory(category)
        }
    }

    override fun onRefresh() {
        loadLocation()
        when (selectedTab) {
            0 -> loadNearbyEvents()
            1 -> loadMyEvents()
        }
    }

    override fun loadNearbyEvents() {
        val location = currentLocation
        println("DEBUG: Current location: $location")
        if (location == null) {
            uiState = UiState.Error("No se pudo obtener la ubicación")
            return
        }
        viewModelScope.launch {
            uiState = UiState.Loading
            println("DEBUG: Buscando eventos en: lat=${location.latitude}, lon=${location.longitude}")
            searchNearbyEventsUseCase(location, 25).fold(
                onSuccess = { events ->
                    println("DEBUG: Eventos encontrados: ${events.size}")
                    uiState = UiState.Success(events)
                },
                onFailure = { exception ->
                    println("DEBUG: Error: ${exception.message}")
                    exception.printStackTrace()
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    override fun loadMyEvents() {
        viewModelScope.launch {
            val userId = getCurrentUserId() ?: run {
                uiState = UiState.Error("Usuario no autenticado")
                return@launch
            }
            uiState = UiState.Loading
            getUserJoinedEventsUseCase(userId).fold(
                onSuccess = { events ->
                    uiState = UiState.Success(events)
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    override fun clearSearch() {
        searchQuery = ""
        selectedCategory = null
        loadNearbyEvents()
    }

    private fun searchByName(query: String) {
        val location = currentLocation ?: return
        viewModelScope.launch {
            uiState = UiState.Loading
            searchEventsByNameUseCase(location, query, 25).fold(
                onSuccess = { events ->
                    uiState = UiState.Success(events)
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    private fun searchByCategory(category: EventCategory) {
        val location = currentLocation ?: return
        viewModelScope.launch {
            uiState = UiState.Loading
            searchEventsByCategoryUseCase(location, category, 25).fold(
                onSuccess = { events ->
                    uiState = UiState.Success(events)
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    private fun loadLocation() {
        viewModelScope.launch {
            locationService.getCurrentLocation().fold(
                onSuccess = { location ->
                    currentLocation = location
                },
                onFailure = {
                    currentLocation = Location(28.1235, -15.4363, "Las Palmas de Gran Canaria, Spain", null, null, null)
                }
            )
        }
    }

    private fun getCurrentUserId(): UUID? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return null
        return UUID.parse(firebaseUser.uid).getOrNull()
    }

    private fun handleException(exception: Throwable): String {
        return when (exception) {
            is LocationPermissionDeniedException -> "Permiso de ubicación denegado"
            is LocationUnavailableException -> "No se pudo obtener la ubicación"
            is EventNotFoundException -> "Evento no encontrado"
            else -> "Error al cargar eventos"
        }
    }
}
