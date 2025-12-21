package software.ulpgc.wherewhen.presentation.events.list

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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

    private val dispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

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
        testScope = TestScope(dispatcher)

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
        coEvery { searchNearbyEventsUseCase.invoke(any(), any()) } returns Result.success(emptyList())
        coEvery { searchEventsByNameUseCase.invoke(any(), any(), any()) } returns Result.success(emptyList())
        coEvery { searchEventsByCategoryUseCase.invoke(any(), any(), any()) } returns Result.success(emptyList())
        coEvery { getUserJoinedEventsUseCase.invoke(any()) } returns Result.success(emptyList())
        coEvery { getUserCreatedEventsUseCase.invoke(any()) } returns Result.success(emptyList())

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
        testScope.cancel()
        Dispatchers.resetMain()
        unmockkStatic(FirebaseAuth::class)
    }

    @Test
    fun `initial state loads location and nearby events`() = runTest {
        advanceUntilIdle()

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
    fun `loadNearbyEvents filters only active or rescheduled and sorts by date and distance`() = runTest {
        val event1 = createEvent(status = EventStatus.CANCELLED, distance = 5.0)
        val event2 = createEvent(status = EventStatus.ACTIVE, distance = 10.0, plusMinutes = 60)
        val event3 = createEvent(status = EventStatus.ACTIVE, distance = 2.0, plusMinutes = 30)
        val event4 = createEvent(status = EventStatus.RESCHEDULED, distance = null, plusMinutes = 10)
        val events = listOf(event1, event2, event3, event4)

        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(events)

        viewModel.loadNearbyEvents()
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(state is UiState.Success)
        state as UiState.Success
        val ids = state.events.map { it.id }
        assertEquals(listOf(event4.id, event3.id, event2.id), ids)
    }

    @Test
    fun `loadNearbyEvents failure maps error with handleException`() = runTest {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns
                Result.failure(LocationUnavailableException())

        viewModel.loadNearbyEvents()
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(state is UiState.Error)
        assertEquals("Location could not be obtained", (state as UiState.Error).message)
    }

    @Test
    fun `onTabSelected 1 loads joined events and clears search`() = runTest {
        coEvery { getUserJoinedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("rock")
        advanceUntilIdle()

        viewModel.onCategorySelected(EventCategory.MUSIC)
        advanceUntilIdle()

        viewModel.onTabSelected(1)
        advanceUntilIdle()

        assertEquals(1, viewModel.selectedTab)
        assertEquals("", viewModel.searchQuery)
        assertNull(viewModel.selectedCategory)
        assertTrue(viewModel.uiState is UiState.Success)
        coVerify { getUserJoinedEventsUseCase.invoke(currentUserId) }
    }

    @Test
    fun `onTabSelected 2 loads created events and clears search`() = runTest {
        coEvery { getUserCreatedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("music")
        advanceUntilIdle()

        viewModel.onCategorySelected(EventCategory.MUSIC)
        advanceUntilIdle()

        viewModel.onTabSelected(2)
        advanceUntilIdle()

        assertEquals(2, viewModel.selectedTab)
        assertEquals("", viewModel.searchQuery)
        assertNull(viewModel.selectedCategory)
        assertTrue(viewModel.uiState is UiState.Success)
        coVerify { getUserCreatedEventsUseCase.invoke(currentUserId) }
    }

    @Test
    fun `loadJoinedEvents without user sets authentication error`() = runTest {
        every { firebaseAuth.currentUser } returns null

        viewModel.onTabSelected(1)
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(state is UiState.Error)
        assertEquals("User not authenticated", (state as UiState.Error).message)
        coVerify(exactly = 0) { getUserJoinedEventsUseCase.invoke(any()) }
    }

    @Test
    fun `loadCreatedEvents without user sets authentication error`() = runTest {
        every { firebaseAuth.currentUser } returns null

        viewModel.onTabSelected(2)
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(state is UiState.Error)
        assertEquals("User not authenticated", (state as UiState.Error).message)
        coVerify(exactly = 0) { getUserCreatedEventsUseCase.invoke(any()) }
    }

    @Test
    fun `onSearchQueryChange in nearby tab with blank query reloads nearby events`() = runTest {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("test")
        advanceUntilIdle()

        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        coVerify(atLeast = 2) { searchNearbyEventsUseCase.invoke(defaultLocation, any()) }
    }

    @Test
    fun `onSearchQueryChange in nearby tab with text calls searchByName`() = runTest {
        coEvery { searchEventsByNameUseCase.invoke(defaultLocation, "rock", any()) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("rock")
        advanceUntilIdle()

        coVerify { searchEventsByNameUseCase.invoke(defaultLocation, "rock", any()) }
    }

    @Test
    fun `onCategorySelected in nearby tab with null category reloads based on query`() = runTest {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(emptyList())
        coEvery { searchEventsByNameUseCase.invoke(defaultLocation, "rock", any()) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("rock")
        advanceUntilIdle()

        viewModel.onCategorySelected(null)
        advanceUntilIdle()

        coVerify { searchEventsByNameUseCase.invoke(defaultLocation, "rock", any()) }
    }

    @Test
    fun `onCategorySelected in nearby tab with category calls searchByCategory`() = runTest {
        coEvery { searchEventsByCategoryUseCase.invoke(defaultLocation, EventCategory.MUSIC, any()) } returns Result.success(emptyList())

        viewModel.onCategorySelected(EventCategory.MUSIC)
        advanceUntilIdle()

        coVerify { searchEventsByCategoryUseCase.invoke(defaultLocation, EventCategory.MUSIC, any()) }
    }

    @Test
    fun `onRadiusChange in nearby tab triggers reload according to current filters`() = runTest {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, 50) } returns Result.success(emptyList())
        coEvery { searchEventsByNameUseCase.invoke(defaultLocation, "rock", 50) } returns Result.success(emptyList())
        coEvery { searchEventsByCategoryUseCase.invoke(defaultLocation, EventCategory.MUSIC, 50) } returns Result.success(emptyList())

        viewModel.onRadiusChange(50)
        advanceUntilIdle()
        coVerify { searchNearbyEventsUseCase.invoke(defaultLocation, 50) }

        viewModel.onSearchQueryChange("rock")
        advanceUntilIdle()
        viewModel.onRadiusChange(50)
        advanceUntilIdle()
        coVerify { searchEventsByNameUseCase.invoke(defaultLocation, "rock", 50) }

        viewModel.onCategorySelected(EventCategory.MUSIC)
        advanceUntilIdle()
        viewModel.onSearchQueryChange("")
        advanceUntilIdle()
        viewModel.onRadiusChange(50)
        advanceUntilIdle()
        coVerify { searchEventsByCategoryUseCase.invoke(defaultLocation, EventCategory.MUSIC, 50) }
    }

    @Test
    fun `onRefresh reloads according to selected tab`() = runTest {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(emptyList())
        coEvery { getUserJoinedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())
        coEvery { getUserCreatedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())

        viewModel.onRefresh()
        advanceUntilIdle()
        coVerify { searchNearbyEventsUseCase.invoke(defaultLocation, any()) }

        viewModel.onTabSelected(1)
        advanceUntilIdle()
        viewModel.onRefresh()
        advanceUntilIdle()
        coVerify { getUserJoinedEventsUseCase.invoke(currentUserId) }

        viewModel.onTabSelected(2)
        advanceUntilIdle()
        viewModel.onRefresh()
        advanceUntilIdle()
        coVerify { getUserCreatedEventsUseCase.invoke(currentUserId) }
    }

    @Test
    fun `clearSearch resets query and category and reloads according to tab`() = runTest {
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(emptyList())
        coEvery { getUserJoinedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())
        coEvery { getUserCreatedEventsUseCase.invoke(currentUserId) } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("something")
        advanceUntilIdle()
        viewModel.onCategorySelected(EventCategory.MUSIC)
        advanceUntilIdle()

        viewModel.clearSearch()
        advanceUntilIdle()
        assertEquals("", viewModel.searchQuery)
        assertNull(viewModel.selectedCategory)
        coVerify(atLeast = 1) { searchNearbyEventsUseCase.invoke(defaultLocation, any()) }

        viewModel.onTabSelected(1)
        advanceUntilIdle()
        viewModel.onSearchQueryChange("x")
        advanceUntilIdle()
        viewModel.clearSearch()
        advanceUntilIdle()
        coVerify { getUserJoinedEventsUseCase.invoke(currentUserId) }

        viewModel.onTabSelected(2)
        advanceUntilIdle()
        viewModel.onSearchQueryChange("y")
        advanceUntilIdle()
        viewModel.clearSearch()
        advanceUntilIdle()
        coVerify { getUserCreatedEventsUseCase.invoke(currentUserId) }
    }

    @Test
    fun `removeEventFromCurrentList removes event from success state`() = runTest {
        val event1 = createEvent()
        val event2 = createEvent()
        coEvery { searchNearbyEventsUseCase.invoke(defaultLocation, any()) } returns Result.success(listOf(event1, event2))

        viewModel.loadNearbyEvents()
        advanceUntilIdle()

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
