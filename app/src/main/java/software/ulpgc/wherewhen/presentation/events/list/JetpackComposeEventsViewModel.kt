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
import software.ulpgc.wherewhen.domain.model.events.EventStatus
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

    var radiusKm by mutableStateOf(25)
        private set

    init {
        loadLocation()
    }

    override fun onTabSelected(index: Int) {
        selectedTab = index
        clearSearch()
        when (index) {
            0 -> loadNearbyEvents()
            1 -> loadJoinedEvents()
            2 -> loadCreatedEvents()
        }
    }

    override fun onSearchQueryChange(query: String) {
        searchQuery = query
        when (selectedTab) {
            0 -> {
                if (query.isBlank()) {
                    loadNearbyEvents()
                } else {
                    searchByName(query)
                }
            }
            1 -> searchJoinedEvents(query)
            2 -> searchCreatedEvents(query)
        }
    }

    override fun onCategorySelected(category: EventCategory?) {
        selectedCategory = category
        when (selectedTab) {
            0 -> {
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
            1 -> loadJoinedEvents()
            2 -> loadCreatedEvents()
        }
    }

    fun onRadiusChange(newRadius: Int) {
        radiusKm = newRadius
        if (selectedTab == 0) {
            when {
                searchQuery.isNotBlank() -> searchByName(searchQuery)
                selectedCategory != null -> searchByCategory(selectedCategory!!)
                else -> loadNearbyEvents()
            }
        }
    }

    override fun onRefresh() {
        when (selectedTab) {
            0 -> loadLocation()
            1 -> loadJoinedEvents()
            2 -> loadCreatedEvents()
        }
    }

    override fun loadNearbyEvents() {
        val location = currentLocation
        if (location == null) {
            uiState = UiState.Error("Location could not be obtained")
            return
        }
        viewModelScope.launch {
            uiState = UiState.Loading
            searchNearbyEventsUseCase(location, radiusKm).fold(
                onSuccess = { events ->
                    val filteredEvents = events.filter {
                        it.status == EventStatus.ACTIVE || it.status == EventStatus.RESCHEDULED
                    }
                    val sorted = filteredEvents.sortedWith(
                        compareBy<Event> { it.dateTime }
                            .thenBy { it.distance ?: Double.MAX_VALUE }
                    )
                    uiState = UiState.Success(sorted)
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    override fun loadMyEvents() {
        loadJoinedEvents()
    }

    private fun loadJoinedEvents() {
        viewModelScope.launch {
            val userId = getCurrentUserId() ?: run {
                uiState = UiState.Error("User not authenticated")
                return@launch
            }
            uiState = UiState.Loading
            getUserJoinedEventsUseCase(userId).fold(
                onSuccess = { events ->
                    val filteredEvents = filterEventsBySearchAndCategory(events)
                    val sorted = filteredEvents.sortedWith(
                        compareBy<Event> { it.dateTime }
                            .thenBy { it.distance ?: Double.MAX_VALUE }
                    )
                    uiState = UiState.Success(sorted)
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    private fun loadCreatedEvents() {
        viewModelScope.launch {
            val userId = getCurrentUserId() ?: run {
                uiState = UiState.Error("User not authenticated")
                return@launch
            }
            uiState = UiState.Loading
            getUserCreatedEventsUseCase(userId).fold(
                onSuccess = { events ->
                    val filteredEvents = filterEventsBySearchAndCategory(events)
                    val sorted = filteredEvents.sortedWith(
                        compareBy<Event> { it.dateTime }
                            .thenBy { it.distance ?: Double.MAX_VALUE }
                    )
                    uiState = UiState.Success(sorted)
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    private fun searchJoinedEvents(query: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId() ?: run {
                uiState = UiState.Error("User not authenticated")
                return@launch
            }
            uiState = UiState.Loading
            getUserJoinedEventsUseCase(userId).fold(
                onSuccess = { events ->
                    val filteredEvents = filterEventsBySearchAndCategory(events)
                    val sorted = filteredEvents.sortedWith(
                        compareBy<Event> { it.dateTime }
                            .thenBy { it.distance ?: Double.MAX_VALUE }
                    )
                    uiState = UiState.Success(sorted)
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    private fun searchCreatedEvents(query: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId() ?: run {
                uiState = UiState.Error("User not authenticated")
                return@launch
            }
            uiState = UiState.Loading
            getUserCreatedEventsUseCase(userId).fold(
                onSuccess = { events ->
                    val filteredEvents = filterEventsBySearchAndCategory(events)
                    val sorted = filteredEvents.sortedWith(
                        compareBy<Event> { it.dateTime }
                            .thenBy { it.distance ?: Double.MAX_VALUE }
                    )
                    uiState = UiState.Success(sorted)
                },
                onFailure = { exception ->
                    uiState = UiState.Error(handleException(exception))
                }
            )
        }
    }

    private fun filterEventsBySearchAndCategory(events: List<Event>): List<Event> {
        var filtered = events
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description?.contains(searchQuery, ignoreCase = true) == true
            }
        }
        if (selectedCategory != null) {
            filtered = filtered.filter { it.category == selectedCategory }
        }
        return filtered
    }

    override fun clearSearch() {
        searchQuery = ""
        selectedCategory = null
        when (selectedTab) {
            0 -> loadNearbyEvents()
            1 -> loadJoinedEvents()
            2 -> loadCreatedEvents()
        }
    }

    private fun searchByName(query: String) {
        val location = currentLocation ?: return
        viewModelScope.launch {
            uiState = UiState.Loading
            searchEventsByNameUseCase(location, query, radiusKm).fold(
                onSuccess = { events ->
                    val filteredEvents = events.filter {
                        it.status == EventStatus.ACTIVE || it.status == EventStatus.RESCHEDULED
                    }
                    val sorted = filteredEvents.sortedWith(
                        compareBy<Event> { it.dateTime }
                            .thenBy { it.distance ?: Double.MAX_VALUE }
                    )
                    uiState = UiState.Success(sorted)
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
            searchEventsByCategoryUseCase(location, category, radiusKm).fold(
                onSuccess = { events ->
                    val filteredEvents = events.filter {
                        it.status == EventStatus.ACTIVE || it.status == EventStatus.RESCHEDULED
                    }
                    val sorted = filteredEvents.sortedWith(
                        compareBy<Event> { it.dateTime }
                            .thenBy { it.distance ?: Double.MAX_VALUE }
                    )
                    uiState = UiState.Success(sorted)
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
                    if (selectedTab == 0) {
                        loadNearbyEvents()
                    }
                },
                onFailure = {
                    currentLocation = Location(
                        28.1235,
                        -15.4363,
                        "Las Palmas de Gran Canaria, Spain",
                        null,
                        null,
                        null
                    )
                    if (selectedTab == 0) {
                        loadNearbyEvents()
                    }
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
            is LocationPermissionDeniedException -> "Location permissions denied"
            is LocationUnavailableException -> "Location could not be obtained"
            is EventNotFoundException -> "Event not found"
            else -> "Error loading events: ${exception.message}"
        }
    }

    fun removeEventFromCurrentList(eventId: UUID) {
        val currentState = uiState
        if (currentState is UiState.Success) {
            val updated = currentState.events.filter { it.id != eventId }
            uiState = UiState.Success(updated)
        }
    }
}
