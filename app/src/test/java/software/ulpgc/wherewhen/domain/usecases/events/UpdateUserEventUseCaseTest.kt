package software.ulpgc.wherewhen.domain.usecases.events

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.events.InvalidEventException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.EventSource
import software.ulpgc.wherewhen.domain.model.events.EventStatus
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.persistence.UserEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class UpdateUserEventUseCaseTest {

    private lateinit var repository: UserEventRepository
    private lateinit var useCase: UpdateUserEventUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = UpdateUserEventUseCase(repository)
    }

    @Test
    fun `fails when event not found`() = runTest {
        val eventId = UUID.Companion.random()

        coEvery { repository.getEventById(eventId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(eventId)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is EventNotFoundException)
        coVerify { repository.getEventById(eventId) }
        coVerify(exactly = 0) { repository.updateUserEvent(any()) }
    }

    @Test
    fun `fails when new title is blank`() = runTest {
        val eventId = UUID.Companion.random()
        val category = mockk<EventCategory>()
        val location = mockk<Location>()
        val organizerId = UUID.Companion.random()
        val now = LocalDateTime.now()
        val existing = Event(
            id = eventId,
            title = "Title",
            description = "desc",
            category = category,
            location = location,
            dateTime = now.plusDays(1),
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

        coEvery { repository.getEventById(eventId) } returns Result.success(existing)

        val result = useCase.invoke(eventId, newTitle = "")

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is InvalidEventException)
        coVerify { repository.getEventById(eventId) }
        coVerify(exactly = 0) { repository.updateUserEvent(any()) }
    }

    @Test
    fun `fails when new end date is before start`() = runTest {
        val eventId = UUID.Companion.random()
        val category = mockk<EventCategory>()
        val location = mockk<Location>()
        val organizerId = UUID.Companion.random()
        val now = LocalDateTime.now()
        val start = now.plusDays(2)
        val existing = Event(
            id = eventId,
            title = "Title",
            description = "desc",
            category = category,
            location = location,
            dateTime = start,
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

        coEvery { repository.getEventById(eventId) } returns Result.success(existing)

        val newEnd = start.minusHours(1)
        val result = useCase.invoke(eventId, newEndDateTime = newEnd)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is InvalidEventException)
        coVerify { repository.getEventById(eventId) }
        coVerify(exactly = 0) { repository.updateUserEvent(any()) }
    }

    @Test
    fun `updates event and sets status to rescheduled when dates change`() = runTest {
        val eventId = UUID.Companion.random()
        val category = mockk<EventCategory>()
        val location = mockk<Location>()
        val organizerId = UUID.Companion.random()
        val now = LocalDateTime.now()
        val start = now.plusDays(2)
        val existing = Event(
            id = eventId,
            title = "Title",
            description = "desc",
            category = category,
            location = location,
            dateTime = start,
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

        val newDate = start.plusDays(1)
        val captured = slot<Event>()
        val updatedFromRepo = existing.copy(dateTime = newDate, status = EventStatus.RESCHEDULED)

        coEvery { repository.getEventById(eventId) } returns Result.success(existing)
        coEvery { repository.updateUserEvent(capture(captured)) } returns Result.success(
            updatedFromRepo
        )

        val result = useCase.invoke(eventId, newDateTime = newDate)

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(updatedFromRepo, result.getOrNull())

        val updated = captured.captured
        Assert.assertEquals(newDate, updated.dateTime)
        Assert.assertEquals(EventStatus.RESCHEDULED, updated.status)

        coVerify { repository.getEventById(eventId) }
        coVerify { repository.updateUserEvent(any()) }
    }
}