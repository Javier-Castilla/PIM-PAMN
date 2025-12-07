package software.ulpgc.wherewhen.presentation.events.form

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.EventSource
import software.ulpgc.wherewhen.domain.model.events.EventStatus
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.model.events.Price
import software.ulpgc.wherewhen.domain.usecases.events.CreateUserEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetEventByIdUseCase
import software.ulpgc.wherewhen.domain.usecases.events.UpdateUserEventUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.presentation.events.form.JetpackComposeCreateEventViewModel.UiState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackComposeCreateEventViewModelTest {

    private lateinit var application: Application
    private lateinit var createUserEventUseCase: CreateUserEventUseCase
    private lateinit var updateUserEventUseCase: UpdateUserEventUseCase
    private lateinit var getEventByIdUseCase: GetEventByIdUseCase
    private lateinit var locationService: software.ulpgc.wherewhen.domain.ports.location.LocationService
    private lateinit var imageUploadService: software.ulpgc.wherewhen.domain.ports.storage.ImageUploadService

    private lateinit var viewModel: JetpackComposeCreateEventViewModel

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private val dispatcher = UnconfinedTestDispatcher()

    private val currentUserId: UUID = UUID.random()
    private val currentUserIdString: String = currentUserId.toString()

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

        application = mockk(relaxed = true)
        createUserEventUseCase = mockk()
        updateUserEventUseCase = mockk()
        getEventByIdUseCase = mockk()
        locationService = mockk()
        imageUploadService = mockk()

        coEvery { locationService.getCurrentLocation() } returns Result.success(defaultLocation)

        viewModel = JetpackComposeCreateEventViewModel(
            application,
            createUserEventUseCase,
            updateUserEventUseCase,
            getEventByIdUseCase,
            locationService,
            imageUploadService
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is idle with defaults and current location loaded`() {
        val state = viewModel.uiState

        assertTrue(state is UiState.Idle)
        assertEquals("", viewModel.title)
        assertEquals("", viewModel.description)
        assertEquals(EventCategory.OTHER, viewModel.selectedCategory)
        assertTrue(viewModel.isFreeEvent)
        assertFalse(viewModel.hasUnsavedChanges)
        assertEquals(defaultLocation, viewModel.currentLocation)
        assertEquals("Calle Falsa 123, Las Palmas", viewModel.locationAddress)
    }

    @Test
    fun `onTitleChange updates title and marks unsaved changes`() {
        viewModel.onTitleChange("Fiesta")

        assertEquals("Fiesta", viewModel.title)
        assertTrue(viewModel.hasUnsavedChanges)
    }

    @Test
    fun `onMaxAttendeesChange accepts only digits`() {
        viewModel.onMaxAttendeesChange("123")
        assertEquals("123", viewModel.maxAttendees)

        viewModel.onMaxAttendeesChange("12a")
        assertEquals("123", viewModel.maxAttendees)
    }

    @Test
    fun `onBackPressed with unsaved changes shows exit dialog`() {
        viewModel.onTitleChange("Cambio")

        viewModel.onBackPressed()

        assertTrue(viewModel.showExitDialog)
    }

    @Test
    fun `onBackPressed without unsaved changes does not show exit dialog`() {
        viewModel.resetState()

        viewModel.onBackPressed()

        assertFalse(viewModel.showExitDialog)
    }

    @Test
    fun `confirmExit closes dialog and calls callback`() {
        viewModel.onTitleChange("Cambio")
        viewModel.onBackPressed()
        var exited = false

        viewModel.confirmExit { exited = true }

        assertFalse(viewModel.showExitDialog)
        assertTrue(exited)
    }

    @Test
    fun `createEvent with unauthenticated user sets error`() {
        every { firebaseAuth.currentUser } returns null

        var callbackCalled = false
        viewModel.createEvent { callbackCalled = true }

        val state = viewModel.uiState
        assertTrue(state is UiState.Error)
        assertEquals("User not authenticated", (state as UiState.Error).message)
        assertFalse(callbackCalled)
    }

    @Test
    fun `createEvent success with current location and free event calls use case and sets success`() {
        val dummyEvent = createDummyEvent()

        coEvery {
            createUserEventUseCase.invoke(
                title = any(),
                description = any(),
                location = any(),
                dateTime = any(),
                endDateTime = any(),
                category = any(),
                organizerId = any(),
                maxAttendees = any(),
                imageUrl = any(),
                price = any()
            )
        } returns Result.success(dummyEvent)

        viewModel.onTitleChange("Fiesta")
        viewModel.onCategoryChange(EventCategory.MUSIC)

        var callbackCalled = false
        viewModel.createEvent { callbackCalled = true }

        assertTrue(callbackCalled)
        assertTrue(viewModel.uiState is UiState.Success)
        assertFalse(viewModel.hasUnsavedChanges)

        coVerify {
            createUserEventUseCase.invoke(
                title = "Fiesta",
                description = null,
                location = defaultLocation,
                dateTime = any(),
                endDateTime = null,
                category = EventCategory.MUSIC,
                organizerId = currentUserId,
                maxAttendees = null,
                imageUrl = null,
                price = Price.free()
            )
        }
    }

    @Test
    fun `createEvent with invalid price amount returns error and does not call use case`() {
        coEvery {
            createUserEventUseCase.invoke(
                title = any(),
                description = any(),
                location = any(),
                dateTime = any(),
                endDateTime = any(),
                category = any(),
                organizerId = any(),
                maxAttendees = any(),
                imageUrl = any(),
                price = any()
            )
        } returns Result.success(createDummyEvent())

        viewModel.onTitleChange("Fiesta")
        viewModel.onIsFreeEventChange(false)
        viewModel.onPriceAmountChange("0")

        var callbackCalled = false
        viewModel.createEvent { callbackCalled = true }

        val state = viewModel.uiState
        assertTrue(state is UiState.Error)
        assertEquals("Please provide a valid price amount", (state as UiState.Error).message)
        assertFalse(callbackCalled)

        coVerify(exactly = 0) {
            createUserEventUseCase.invoke(
                title = any(),
                description = any(),
                location = any(),
                dateTime = any(),
                endDateTime = any(),
                category = any(),
                organizerId = any(),
                maxAttendees = any(),
                imageUrl = any(),
                price = any()
            )
        }
    }

    @Test
    fun `loadEventToEdit fills fields and sets edit mode data`() {
        val eventId = UUID.random()
        val eventLocation = Location(
            latitude = 10.0,
            longitude = 20.0,
            address = "Evento 123",
            placeName = "Sala X",
            city = "Ciudad",
            country = "ES"
        )
        val eventDateTime = LocalDateTime.of(LocalDate.now().plusDays(2), LocalTime.of(20, 0))
        val eventPrice = Price.single(15.0, "EUR")
        val event = Event(
            id = eventId,
            title = "Título original",
            description = "Descripción original",
            category = EventCategory.SPORTS,
            location = eventLocation,
            dateTime = eventDateTime,
            endDateTime = null,
            imageUrl = "http://image",
            source = EventSource.USER_CREATED,
            organizerId = currentUserId,
            externalId = null,
            externalUrl = null,
            price = eventPrice,
            distance = null,
            status = EventStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            maxAttendees = 50
        )

        coEvery { getEventByIdUseCase.invoke(eventId) } returns Result.success(event)

        viewModel.loadEventToEdit(eventId)

        assertEquals("Título original", viewModel.title)
        assertEquals("Descripción original", viewModel.description)
        assertEquals(EventCategory.SPORTS, viewModel.selectedCategory)
        assertEquals(eventDateTime.toLocalDate(), viewModel.selectedDate)
        assertEquals(eventDateTime.toLocalTime(), viewModel.selectedTime)
        assertEquals("Evento 123", viewModel.locationAddress)
        assertEquals(eventLocation, viewModel.currentLocation)
        assertFalse(viewModel.useCurrentLocation)
        assertEquals("http://image", viewModel.imageUrl)
        assertFalse(viewModel.isFreeEvent)
        assertEquals("15.0", viewModel.priceAmount)
        assertEquals("EUR", viewModel.priceCurrency)
        assertFalse(viewModel.hasUnsavedChanges)
        assertTrue(viewModel.uiState is UiState.Idle)
    }

    @Test
    fun `loadEventToEdit failure sets error state`() {
        val eventId = UUID.random()
        coEvery {
            getEventByIdUseCase.invoke(eventId)
        } returns Result.failure(RuntimeException("not found"))

        viewModel.loadEventToEdit(eventId)

        val state = viewModel.uiState
        assertTrue(state is UiState.Error)
        assertTrue((state as UiState.Error).message.contains("Failed to load event"))
    }

    @Test
    fun `clearError sets uiState to Idle`() {
        every { firebaseAuth.currentUser } returns null

        viewModel.createEvent {}

        assertTrue(viewModel.uiState is UiState.Error)

        viewModel.clearError()

        assertTrue(viewModel.uiState is UiState.Idle)
    }

    private fun createDummyEvent(): Event {
        val now = LocalDateTime.now().plusDays(2)
        return Event(
            id = UUID.random(),
            title = "dummy",
            description = "dummy",
            category = EventCategory.OTHER,
            location = defaultLocation,
            dateTime = now,
            endDateTime = null,
            imageUrl = null,
            source = EventSource.USER_CREATED,
            organizerId = currentUserId,
            externalId = null,
            externalUrl = null,
            price = null,
            distance = null,
            status = EventStatus.ACTIVE,
            createdAt = now,
            maxAttendees = 10
        )
    }
}
