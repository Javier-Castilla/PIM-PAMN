package software.ulpgc.wherewhen.presentation.events.list

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.events.*
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.EventStatus
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import software.ulpgc.wherewhen.domain.usecases.events.*
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.presentation.events.JetpackComposeEventsViewModel
import software.ulpgc.wherewhen.presentation.events.JetpackComposeEventsViewModel.UiState
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackComposeEventsViewModelTest {

    private lateinit var searchNearbyEventsUseCase: SearchNearbyEventsUseCase
    private lateinit var searchEventsByNameUseCase: SearchEventsByNameUseCase
    private lateinit var searchEventsByCategoryUseCase: SearchEventsByCategoryUseCase
    private lateinit var getUserJoinedEventsUseCase: GetUserJoinedEventsUseCase
    private lateinit var getUserCreatedEventsUseCase: GetUserCreatedEventsUseCase
    private lateinit var locationService: LocationService
    private lateinit var viewModel: JetpackComposeEventsViewModel
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private val dispatcher = UnconfinedTestDispatcher()

    private val currentUserId: UUID = UUID.random()
    private val currentUserIdString: String = currentUserId.toString()

    private val defaultLocation = Location(
        latitude = 28.1,
        longitude = -15.4,
        address = "Las Palmas de Gran Canaria, Spain",
        placeName = null,
        city = null,
        country = null
    )

    private val now = LocalDateTime.now()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        mockkStatic(FirebaseAuth::class)
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns currentUserIdString

        searchNearbyEventsUseCase = mockk()
        searchEventsByNameUseCase = mockk()
        searchEventsByCategoryUseCase = mockk()
        getUserJoinedEventsUseCase = mockk()
        getUserCreatedEventsUseCase = mockk()
        locationService = mockk()

        coEvery { locationService.getCurrentLocation() } returns Result.success(defaultLocation)
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(emptyList())

        viewModel = JetpackComposeEventsViewModel(
            searchNearbyEventsUseCase,
            searchEventsByNameUseCase,
            searchEventsByCategoryUseCase,
            getUserJoinedEventsUseCase,
            getUserCreatedEventsUseCase,
            locationService
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads location and nearby events`() {
        assertEquals(0, viewModel.selectedTab)
        assertEquals("", viewModel.searchQuery)
        assertEquals(25, viewModel.radiusKm)
        assertNotNull(viewModel.currentLocation)

        val state = viewModel.uiState
        assertTrue(state is UiState.Success)
        state as UiState.Success
        assertTrue(state.events.isEmpty())

        coVerify { locationService.getCurrentLocation() }
        coVerify { searchNearbyEventsUseCase.invoke(defaultLocation, 25) }
    }

    @Test
    fun `loadNearbyEvents filters only active or rescheduled and sorts by date and distance`() = runBlocking {
        val event1 = createEvent(status = EventStatus.CANCELLED, distance = 5.0)
        val event2 = createEvent(status = EventStatus.ACTIVE, distance = 10.0, plusMinutes = 60)
        val event3 = createEvent(status = EventStatus.ACTIVE, distance = 2.0, plusMinutes = 30)
        val event4 = createEvent(status = EventStatus.RESCHEDULED, distance = null, plusMinutes = 10)
        val events = listOf(event1, event2, event3, event4)

        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(events)

        viewModel.loadNearbyEvents()

        val state = viewModel.uiState
        assertTrue(state is UiState.Success)
        state as UiState.Success
        val ids = state.events.map { it.id }
        assertEquals(listOf(event4.id, event3.id, event2.id), ids)
    }

    @Test
    fun `loadNearbyEvents failure maps error with handleException`() {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns
                Result.failure(LocationUnavailableException())

        viewModel.loadNearbyEvents()

        val state = viewModel.uiState
        assertTrue(state is UiState.Error)
        assertEquals("Location could not be obtained", (state as UiState.Error).message)
    }

    @Test
    fun `onTabSelected 1 loads joined events and clears search`() {
        coEvery { getUserJoinedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("rock")
        viewModel.onCategorySelected(EventCategory.MUSIC)

        viewModel.onTabSelected(1)

        assertEquals(1, viewModel.selectedTab)
        assertEquals("", viewModel.searchQuery)
        assertNull(viewModel.selectedCategory)
        assertTrue(viewModel.uiState is UiState.Success)
        coVerify { getUserJoinedEventsUseCase.invoke(currentUserId) }
    }

    @Test
    fun `onTabSelected 2 loads created events and clears search`() {
        coEvery { getUserCreatedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("music")
        viewModel.onCategorySelected(EventCategory.MUSIC)

        viewModel.onTabSelected(2)

        assertEquals(2, viewModel.selectedTab)
        assertEquals("", viewModel.searchQuery)
        assertNull(viewModel.selectedCategory)
        assertTrue(viewModel.uiState is UiState.Success)
        coVerify { getUserCreatedEventsUseCase.invoke(currentUserId) }
    }

    @Test
    fun `loadJoinedEvents without user sets authentication error`() {
        every { firebaseAuth.currentUser } returns null

        viewModel.onTabSelected(1)

        val state = viewModel.uiState
        assertTrue(state is UiState.Error)
        assertEquals("User not authenticated", (state as UiState.Error).message)
        coVerify(exactly = 0) { getUserJoinedEventsUseCase.invoke(any()) }
    }

    @Test
    fun `loadCreatedEvents without user sets authentication error`() {
        every { firebaseAuth.currentUser } returns null

        viewModel.onTabSelected(2)

        val state = viewModel.uiState
        assertTrue(state is UiState.Error)
        assertEquals("User not authenticated", (state as UiState.Error).message)
        coVerify(exactly = 0) { getUserCreatedEventsUseCase.invoke(any()) }
    }

    @Test
    fun `onSearchQueryChange in nearby tab with blank query reloads nearby events`() {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("test")
        coEvery { searchEventsByNameUseCase.invoke(defaultLocation, "test", any()) } returns Result.success(emptyList())
        viewModel.onSearchQueryChange("test")

        viewModel.onSearchQueryChange("")

        coVerify(atLeast = 1) { searchNearbyEventsUseCase.invoke(defaultLocation, any()) }
    }

    @Test
    fun `onSearchQueryChange in nearby tab with text calls searchByName`() {
        coEvery { searchEventsByNameUseCase.invoke(defaultLocation, "rock", any()) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("rock")

        coVerify { searchEventsByNameUseCase.invoke(defaultLocation, "rock", any()) }
    }

    @Test
    fun `onCategorySelected in nearby tab with null category reloads based on query`() {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(emptyList())
        coEvery { searchEventsByNameUseCase.invoke(defaultLocation, "rock", any()) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("rock")
        viewModel.onCategorySelected(null)

        coVerify { searchEventsByNameUseCase.invoke(defaultLocation, "rock", any()) }
    }

    @Test
    fun `onCategorySelected in nearby tab with category calls searchByCategory`() {
        coEvery { searchEventsByCategoryUseCase.invoke(defaultLocation, EventCategory.MUSIC, any()) } returns Result.success(emptyList())

        viewModel.onCategorySelected(EventCategory.MUSIC)

        coVerify { searchEventsByCategoryUseCase.invoke(defaultLocation, EventCategory.MUSIC, any()) }
    }

    @Test
    fun `onRadiusChange in nearby tab triggers reload according to current filters`() {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, 50) } returns Result.success(emptyList())
        coEvery { searchEventsByNameUseCase.invoke(defaultLocation, "rock", 50) } returns Result.success(emptyList())
        coEvery { searchEventsByCategoryUseCase.invoke(defaultLocation, EventCategory.MUSIC, 50) } returns Result.success(emptyList())

        viewModel.onRadiusChange(50)
        coVerify { searchNearbyEventsUseCase.invoke(defaultLocation, 50) }

        viewModel.onSearchQueryChange("rock")
        viewModel.onRadiusChange(50)
        coVerify { searchEventsByNameUseCase.invoke(defaultLocation, "rock", 50) }

        viewModel.onCategorySelected(EventCategory.MUSIC)
        viewModel.onSearchQueryChange("")
        viewModel.onRadiusChange(50)
        coVerify { searchEventsByCategoryUseCase.invoke(defaultLocation, EventCategory.MUSIC, 50) }
    }

    @Test
    fun `onRefresh reloads according to selected tab`() {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(emptyList())
        coEvery { getUserJoinedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())
        coEvery { getUserCreatedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())

        viewModel.onRefresh()
        coVerify { searchNearbyEventsUseCase.invoke(defaultLocation, any()) }

        viewModel.onTabSelected(1)
        viewModel.onRefresh()
        coVerify { getUserJoinedEventsUseCase.invoke(currentUserId) }

        viewModel.onTabSelected(2)
        viewModel.onRefresh()
        coVerify { getUserCreatedEventsUseCase.invoke(currentUserId) }
    }

    @Test
    fun `clearSearch resets query and category and reloads according to tab`() {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(emptyList())
        coEvery { getUserJoinedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())
        coEvery { getUserCreatedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("something")
        viewModel.onCategorySelected(EventCategory.MUSIC)

        viewModel.clearSearch()
        assertEquals("", viewModel.searchQuery)
        assertNull(viewModel.selectedCategory)
        coVerify { searchNearbyEventsUseCase.invoke(defaultLocation, any()) }

        viewModel.onTabSelected(1)
        viewModel.onSearchQueryChange("x")
        viewModel.clearSearch()
        coVerify { getUserJoinedEventsUseCase.invoke(currentUserId) }

        viewModel.onTabSelected(2)
        viewModel.onSearchQueryChange("y")
        viewModel.clearSearch()
        coVerify { getUserCreatedEventsUseCase.invoke(currentUserId) }
    }

    @Test
    fun `removeEventFromCurrentList removes event from success state`() {
        val event1 = createEvent()
        val event2 = createEvent()
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(listOf(event1, event2))

        viewModel.loadNearbyEvents()

        viewModel.removeEventFromCurrentList(event1.id)

        val state = viewModel.uiState
        assertTrue(state is UiState.Success)
        state as UiState.Success
        val ids = state.events.map { it.id }
        assertEquals(listOf(event2.id), ids)
    }

    private fun createEvent(
        status: EventStatus = EventStatus.ACTIVE,
        distance: Double? = 1.0,
        plusMinutes: Long = 0
    ): Event {
        return Event(
            id = UUID.random(),
            title = "Event",
            description = "Desc",
            category = EventCategory.OTHER,
            location = defaultLocation,
            dateTime = now.plusMinutes(plusMinutes),
            endDateTime = null,
            imageUrl = null,
            source = software.ulpgc.wherewhen.domain.model.events.EventSource.EXTERNAL_API,
            organizerId = null,
            externalId = null,
            externalUrl = null,
            price = null,
            distance = distance,
            status = status,
            createdAt = now.minusDays(1),
            maxAttendees = null
        )
    }
}
