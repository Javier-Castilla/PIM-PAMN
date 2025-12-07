package software.ulpgc.wherewhen.presentation.events.individual

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.events.*
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.EventSource
import software.ulpgc.wherewhen.domain.model.events.EventStatus
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import software.ulpgc.wherewhen.domain.usecases.events.DeleteUserEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetEventAttendeesUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetEventByIdUseCase
import software.ulpgc.wherewhen.domain.usecases.events.JoinEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.LeaveEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.UpdateUserEventStatusUseCase
import software.ulpgc.wherewhen.domain.usecases.user.GetUserUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.presentation.events.individual.JetpackComposeEventDetailViewModel.UiState
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackComposeEventDetailViewModelTest {

    private lateinit var getEventByIdUseCase: GetEventByIdUseCase
    private lateinit var joinEventUseCase: JoinEventUseCase
    private lateinit var leaveEventUseCase: LeaveEventUseCase
    private lateinit var getEventAttendeesUseCase: GetEventAttendeesUseCase
    private lateinit var deleteUserEventUseCase: DeleteUserEventUseCase
    private lateinit var updateEventStatusUseCase: UpdateUserEventStatusUseCase
    private lateinit var getUserUseCase: GetUserUseCase
    private lateinit var locationService: LocationService

    private lateinit var viewModel: JetpackComposeEventDetailViewModel

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private val dispatcher = UnconfinedTestDispatcher()

    private val currentUserId: UUID = UUID.random()
    private val currentUserIdString: String = currentUserId.toString()
    private val otherUserId: UUID = UUID.random()
    private val eventId: UUID = UUID.random()

    private val defaultLocation = Location(
        latitude = 28.1,
        longitude = -15.4,
        address = "Calle Falsa 123",
        placeName = null,
        city = "Las Palmas",
        country = "ES"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        mockkStatic(FirebaseAuth::class)
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns currentUserIdString

        getEventByIdUseCase = mockk()
        joinEventUseCase = mockk()
        leaveEventUseCase = mockk()
        getEventAttendeesUseCase = mockk()
        deleteUserEventUseCase = mockk()
        updateEventStatusUseCase = mockk()
        getUserUseCase = mockk()
        locationService = mockk()

        coEvery { locationService.getCurrentLocation() } returns Result.success(defaultLocation)

        viewModel = JetpackComposeEventDetailViewModel(
            getEventByIdUseCase,
            joinEventUseCase,
            leaveEventUseCase,
            getEventAttendeesUseCase,
            deleteUserEventUseCase,
            updateEventStatusUseCase,
            getUserUseCase,
            locationService
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `loadEvent external event sets success state without attendees`() {
        val event = createEvent(
            id = eventId,
            source = EventSource.EXTERNAL_API,
            organizerId = null,
            maxAttendees = null,
            status = EventStatus.ACTIVE
        )

        coEvery { getEventByIdUseCase.invoke(eventId) } returns Result.success(event)

        viewModel.loadEvent(eventId)

        val state = viewModel.uiState
        assertTrue(state is UiState.Success)
        state as UiState.Success
        assertEquals(event.id, state.event.id)
        assertFalse(state.isAttending)
        assertTrue(state.attendees.isEmpty())
        assertFalse(state.isOrganizer)
        assertFalse(state.isFull)
    }

    @Test
    fun `loadEvent failure sets error state`() {
        coEvery { getEventByIdUseCase.invoke(eventId) } returns Result.failure(RuntimeException("x"))

        viewModel.loadEvent(eventId)

        val state = viewModel.uiState
        assertTrue(state is UiState.Error)
        assertEquals("Event not found", (state as UiState.Error).message)
    }

    @Test
    fun `loadEvent user created with attendees populates attendees and flags`() {
        val event = createEvent(
            id = eventId,
            source = EventSource.USER_CREATED,
            organizerId = currentUserId,
            maxAttendees = 3,
            status = EventStatus.ACTIVE
        )
        coEvery { getEventByIdUseCase.invoke(eventId) } returns Result.success(event)
        every { getEventByIdUseCase.observe(eventId) } returns flowOf(event)
        coEvery { getEventAttendeesUseCase.invoke(eventId) } returns Result.success(listOf(currentUserId))
        coEvery { getUserUseCase.invoke(currentUserId) } returns Result.success(mockk(relaxed = true))

        viewModel.loadEvent(eventId)

        val state = viewModel.uiState
        assertTrue(state is UiState.Success)
        state as UiState.Success
        assertEquals(1, state.attendees.size)
        assertTrue(state.isAttending)
        assertTrue(state.isOrganizer)
        assertFalse(state.isFull)
    }

    @Test
    fun `onJoinEvent when state is full sets inline error and does not call use case`() {
        val event = createEvent(
            id = eventId,
            source = EventSource.USER_CREATED,
            organizerId = currentUserId,
            maxAttendees = 1,
            status = EventStatus.ACTIVE
        )
        coEvery { getEventByIdUseCase.invoke(eventId) } returns Result.success(event)
        every { getEventByIdUseCase.observe(eventId) } returns flowOf(event)
        coEvery { getEventAttendeesUseCase.invoke(eventId) } returns Result.success(listOf(currentUserId))
        coEvery { getUserUseCase.invoke(currentUserId) } returns Result.success(mockk(relaxed = true))

        viewModel.loadEvent(eventId)

        val before = viewModel.uiState as UiState.Success
        assertTrue(before.isFull)

        viewModel.onJoinEvent()

        assertEquals("Event is full", viewModel.inlineErrorMessage)
        coVerify(exactly = 0) { joinEventUseCase.invoke(any(), any()) }
    }

    @Test
    fun `onJoinEvent success clears joining and keeps success state`() {
        val event = createEvent(
            id = eventId,
            source = EventSource.USER_CREATED,
            organizerId = currentUserId,
            maxAttendees = 5,
            status = EventStatus.ACTIVE
        )
        coEvery { getEventByIdUseCase.invoke(eventId) } returns Result.success(event)
        every { getEventByIdUseCase.observe(eventId) } returns flowOf(event)
        coEvery { getEventAttendeesUseCase.invoke(eventId) } returns Result.success(emptyList())
        coEvery { getUserUseCase.invoke(any<UUID>()) } returns Result.success(mockk(relaxed = true))
        coEvery { joinEventUseCase.invoke(eventId, currentUserId) } returns Result.success(Unit)

        viewModel.loadEvent(eventId)

        viewModel.onJoinEvent()

        assertFalse(viewModel.isJoining)
        assertNull(viewModel.inlineErrorMessage)
        assertTrue(viewModel.uiState is UiState.Success)
        coVerify { joinEventUseCase.invoke(eventId, currentUserId) }
    }

    @Test
    fun `onJoinEvent failure with EventFullException sets inline error`() {
        val event = createEvent(
            id = eventId,
            source = EventSource.USER_CREATED,
            organizerId = currentUserId,
            maxAttendees = 5,
            status = EventStatus.ACTIVE
        )
        coEvery { getEventByIdUseCase.invoke(eventId) } returns Result.success(event)
        every { getEventByIdUseCase.observe(eventId) } returns flowOf(event)
        coEvery { getEventAttendeesUseCase.invoke(eventId) } returns Result.success(emptyList())
        coEvery { getUserUseCase.invoke(any<UUID>()) } returns Result.success(mockk(relaxed = true))
        coEvery { joinEventUseCase.invoke(eventId, currentUserId) } returns Result.failure(EventFullException())

        viewModel.loadEvent(eventId)

        viewModel.onJoinEvent()

        assertEquals("Event is full", viewModel.inlineErrorMessage)
        assertFalse(viewModel.isJoining)
    }

    @Test
    fun `onLeaveEvent failure with NotAttendingEventException sets error state`() {
        val event = createEvent(
            id = eventId,
            source = EventSource.USER_CREATED,
            organizerId = currentUserId,
            maxAttendees = 5,
            status = EventStatus.ACTIVE
        )
        coEvery { getEventByIdUseCase.invoke(eventId) } returns Result.success(event)
        every { getEventByIdUseCase.observe(eventId) } returns flowOf(event)
        coEvery { getEventAttendeesUseCase.invoke(eventId) } returns Result.failure(RuntimeException("x"))
        coEvery { leaveEventUseCase.invoke(eventId, currentUserId) } returns Result.failure(NotAttendingEventException())

        viewModel.loadEvent(eventId)

        viewModel.onLeaveEvent()

        val state = viewModel.uiState
        assertTrue(state is UiState.Error)
        assertEquals("You are not attending this event", (state as UiState.Error).message)
        assertFalse(viewModel.isJoining)
    }

    @Test
    fun `loadAttendees external event sets success with empty attendees`() {
        val event = createEvent(
            id = eventId,
            source = EventSource.EXTERNAL_API,
            organizerId = null,
            maxAttendees = null,
            status = EventStatus.ACTIVE
        )
        coEvery { getEventByIdUseCase.invoke(eventId) } returns Result.success(event)

        viewModel.loadEvent(eventId)
        viewModel.loadAttendees()

        val state = viewModel.uiState
        assertTrue(state is UiState.Success)
        state as UiState.Success
        assertFalse(state.isAttending)
        assertTrue(state.attendees.isEmpty())
        assertFalse(state.isOrganizer)
        assertFalse(state.isFull)
    }

    @Test
    fun `onDeleteEvent success calls callback`() {
        val event = createEvent(
            id = eventId,
            source = EventSource.USER_CREATED,
            organizerId = currentUserId,
            maxAttendees = null,
            status = EventStatus.ACTIVE
        )
        coEvery { getEventByIdUseCase.invoke(eventId) } returns Result.success(event)
        every { getEventByIdUseCase.observe(eventId) } returns flowOf(event)
        coEvery { getEventAttendeesUseCase.invoke(eventId) } returns Result.failure(RuntimeException("x"))
        coEvery { deleteUserEventUseCase.invoke(eventId) } returns Result.success(Unit)

        viewModel.loadEvent(eventId)

        var deletedCalled = false
        viewModel.onDeleteEvent { deletedCalled = true }

        assertTrue(deletedCalled)
    }

    @Test
    fun `onUpdateStatus failure UnauthorizedEventAccessException sets inline error and hides dialog`() {
        val event = createEvent(
            id = eventId,
            source = EventSource.USER_CREATED,
            organizerId = otherUserId,
            maxAttendees = null,
            status = EventStatus.ACTIVE
        )
        coEvery { getEventByIdUseCase.invoke(eventId) } returns Result.success(event)
        every { getEventByIdUseCase.observe(eventId) } returns flowOf(event)
        coEvery { getEventAttendeesUseCase.invoke(eventId) } returns Result.failure(RuntimeException("x"))
        coEvery {
            updateEventStatusUseCase.invoke(eventId, EventStatus.CANCELLED, currentUserId)
        } returns Result.failure(UnauthorizedEventAccessException())

        viewModel.loadEvent(eventId)
        viewModel.showStatusDialog()

        viewModel.onUpdateStatus(EventStatus.CANCELLED)

        assertEquals("You have no permission to access", viewModel.inlineErrorMessage)
        assertFalse(viewModel.showStatusDialog)
    }

    @Test
    fun `show and dismiss delete dialog toggle flag`() {
        assertFalse(viewModel.showDeleteDialog)

        viewModel.showDeleteConfirmation()
        assertTrue(viewModel.showDeleteDialog)

        viewModel.dismissDeleteDialog()
        assertFalse(viewModel.showDeleteDialog)
    }

    @Test
    fun `show and dismiss attendees dialog toggle flag`() {
        assertFalse(viewModel.showAttendeesDialog)

        viewModel.openAttendeesDialog()
        assertTrue(viewModel.showAttendeesDialog)

        viewModel.dismissAttendeesDialog()
        assertFalse(viewModel.showAttendeesDialog)
    }

    @Test
    fun `show and dismiss status dialog toggle flag`() {
        assertFalse(viewModel.showStatusDialog)

        viewModel.showStatusDialog()
        assertTrue(viewModel.showStatusDialog)

        viewModel.dismissStatusDialog()
        assertFalse(viewModel.showStatusDialog)
    }

    private fun createEvent(
        id: UUID,
        source: EventSource,
        organizerId: UUID?,
        maxAttendees: Int?,
        status: EventStatus
    ): Event {
        val now = LocalDateTime.now().plusDays(1)
        return Event(
            id = id,
            title = "Event",
            description = "Desc",
            category = EventCategory.OTHER,
            location = defaultLocation,
            dateTime = now,
            endDateTime = null,
            imageUrl = null,
            source = source,
            organizerId = organizerId,
            externalId = null,
            externalUrl = null,
            price = null,
            distance = null,
            status = status,
            createdAt = now.minusDays(1),
            maxAttendees = maxAttendees
        )
    }
}
