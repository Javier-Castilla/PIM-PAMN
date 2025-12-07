package software.ulpgc.wherewhen.domain.usecases.events

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.events.InvalidEventException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.EventSource
import software.ulpgc.wherewhen.domain.model.events.EventStatus
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class CreateUserEventUseCaseTest {

    private lateinit var repository: ExternalEventRepository
    private lateinit var useCase: CreateUserEventUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = CreateUserEventUseCase(repository)
    }

    @Test
    fun `fails when title is blank`() = runTest {
        val location = mockk<Location>()
        val category = mockk<EventCategory>()
        val organizerId = UUID.random()

        val result = useCase.invoke(
            title = "",
            description = "desc",
            location = location,
            dateTime = LocalDateTime.now().plusDays(1),
            endDateTime = null,
            category = category,
            organizerId = organizerId,
            maxAttendees = 10,
            imageUrl = null,
            price = null
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidEventException)
        coVerify(exactly = 0) { repository.createUserEvent(any()) }
    }

    @Test
    fun `fails when date is in the past`() = runTest {
        val location = mockk<Location>()
        val category = mockk<EventCategory>()
        val organizerId = UUID.random()

        val result = useCase.invoke(
            title = "Title",
            description = "desc",
            location = location,
            dateTime = LocalDateTime.now().minusDays(1),
            endDateTime = null,
            category = category,
            organizerId = organizerId,
            maxAttendees = 10,
            imageUrl = null,
            price = null
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidEventException)
        coVerify(exactly = 0) { repository.createUserEvent(any()) }
    }

    @Test
    fun `fails when end date is before start date`() = runTest {
        val location = mockk<Location>()
        val category = mockk<EventCategory>()
        val organizerId = UUID.random()
        val start = LocalDateTime.now().plusDays(2)
        val end = start.minusHours(1)

        val result = useCase.invoke(
            title = "Title",
            description = "desc",
            location = location,
            dateTime = start,
            endDateTime = end,
            category = category,
            organizerId = organizerId,
            maxAttendees = 10,
            imageUrl = null,
            price = null
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidEventException)
        coVerify(exactly = 0) { repository.createUserEvent(any()) }
    }

    @Test
    fun `fails when maxAttendees is not positive`() = runTest {
        val location = mockk<Location>()
        val category = mockk<EventCategory>()
        val organizerId = UUID.random()

        val result = useCase.invoke(
            title = "Title",
            description = "desc",
            location = location,
            dateTime = LocalDateTime.now().plusDays(1),
            endDateTime = null,
            category = category,
            organizerId = organizerId,
            maxAttendees = 0,
            imageUrl = null,
            price = null
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidEventException)
        coVerify(exactly = 0) { repository.createUserEvent(any()) }
    }

    @Test
    fun `creates event and joins organizer`() = runTest {
        val location = mockk<Location>()
        val category = mockk<EventCategory>()
        val organizerId = UUID.random()
        val now = LocalDateTime.now()
        val dateTime = now.plusDays(1)

        val createdEvent = Event(
            id = UUID.random(),
            title = "Title",
            description = "desc",
            category = category,
            location = location,
            dateTime = dateTime,
            endDateTime = null,
            imageUrl = null,
            source = EventSource.USER_CREATED,
            organizerId = organizerId,
            externalId = null,
            externalUrl = null,
            price = null,
            distance = null,
            status = EventStatus.ACTIVE,
            createdAt = now,
            maxAttendees = 10
        )

        val captured = slot<Event>()
        coEvery { repository.createUserEvent(capture(captured)) } returns Result.success(createdEvent)
        coEvery { repository.joinEvent(createdEvent.id, organizerId) } returns Result.success(Unit)

        val result = useCase.invoke(
            title = "Title",
            description = "desc",
            location = location,
            dateTime = dateTime,
            endDateTime = null,
            category = category,
            organizerId = organizerId,
            maxAttendees = 10,
            imageUrl = null,
            price = null
        )

        assertTrue(result.isSuccess)
        assertEquals(createdEvent, result.getOrNull())
        coVerify { repository.createUserEvent(any()) }
        coVerify { repository.joinEvent(createdEvent.id, organizerId) }
    }
}

