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
import software.ulpgc.wherewhen.domain.exceptions.events.UnauthorizedEventAccessException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.EventSource
import software.ulpgc.wherewhen.domain.model.events.EventStatus
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.persistence.UserEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class UpdateUserEventStatusUseCaseTest {

    private lateinit var repository: UserEventRepository
    private lateinit var useCase: UpdateUserEventStatusUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = UpdateUserEventStatusUseCase(repository)
    }

    @Test
    fun `fails when event not found`() = runTest {
        val eventId = UUID.Companion.random()
        val organizerId = UUID.Companion.random()

        coEvery { repository.getEventById(eventId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(eventId, EventStatus.CANCELLED, organizerId)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is EventNotFoundException)
        coVerify { repository.getEventById(eventId) }
        coVerify(exactly = 0) { repository.updateUserEvent(any()) }
    }

    @Test
    fun `fails when organizerId does not match`() = runTest {
        val eventId = UUID.Companion.random()
        val realOrganizerId = UUID.Companion.random()
        val otherUserId = UUID.Companion.random()
        val category = mockk<EventCategory>()
        val location = mockk<Location>()
        val now = LocalDateTime.now()
        val event = Event(
            id = eventId,
            title = "Title",
            description = "desc",
            category = category,
            location = location,
            dateTime = now.plusDays(1),
            endDateTime = null,
            imageUrl = null,
            source = EventSource.USER_CREATED,
            organizerId = realOrganizerId,
            externalId = null,
            externalUrl = null,
            price = null,
            distance = null,
            status = EventStatus.ACTIVE,
            createdAt = now,
            maxAttendees = 10
        )

        coEvery { repository.getEventById(eventId) } returns Result.success(event)

        val result = useCase.invoke(eventId, EventStatus.CANCELLED, otherUserId)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is UnauthorizedEventAccessException)
        coVerify { repository.getEventById(eventId) }
        coVerify(exactly = 0) { repository.updateUserEvent(any()) }
    }

    @Test
    fun `updates status successfully`() = runTest {
        val eventId = UUID.Companion.random()
        val organizerId = UUID.Companion.random()
        val category = mockk<EventCategory>()
        val location = mockk<Location>()
        val now = LocalDateTime.now()
        val event = Event(
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

        val captured = slot<Event>()
        val updatedFromRepo = event.copy(status = EventStatus.CANCELLED)

        coEvery { repository.getEventById(eventId) } returns Result.success(event)
        coEvery { repository.updateUserEvent(capture(captured)) } returns Result.success(
            updatedFromRepo
        )

        val result = useCase.invoke(eventId, EventStatus.CANCELLED, organizerId)

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(updatedFromRepo, result.getOrNull())
        Assert.assertEquals(EventStatus.CANCELLED, captured.captured.status)

        coVerify { repository.getEventById(eventId) }
        coVerify { repository.updateUserEvent(any()) }
    }
}