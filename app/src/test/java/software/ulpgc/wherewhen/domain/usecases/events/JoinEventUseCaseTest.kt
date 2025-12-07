package software.ulpgc.wherewhen.domain.usecases.events

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.events.AlreadyAttendingEventException
import software.ulpgc.wherewhen.domain.exceptions.events.EventFullException
import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class JoinEventUseCaseTest {

    private lateinit var repository: ExternalEventRepository
    private lateinit var useCase: JoinEventUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = JoinEventUseCase(repository)
    }

    @Test
    fun `fails when event not found`() = runTest {
        val eventId = UUID.Companion.random()
        val userId = UUID.Companion.random()

        coEvery { repository.getEventById(eventId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(eventId, userId)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is EventNotFoundException)
        coVerify { repository.getEventById(eventId) }
        coVerify(exactly = 0) { repository.getEventAttendees(any()) }
        coVerify(exactly = 0) { repository.joinEvent(any(), any()) }
    }

    @Test
    fun `fails when user already attending`() = runTest {
        val eventId = UUID.Companion.random()
        val userId = UUID.Companion.random()
        val event = mockk<Event>()

        coEvery { repository.getEventById(eventId) } returns Result.success(event)
        coEvery { repository.getEventAttendees(eventId) } returns Result.success(listOf(userId))

        val result = useCase.invoke(eventId, userId)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is AlreadyAttendingEventException)
        coVerify { repository.getEventById(eventId) }
        coVerify { repository.getEventAttendees(eventId) }
        coVerify(exactly = 0) { repository.joinEvent(any(), any()) }
    }

    @Test
    fun `fails when event is full`() = runTest {
        val eventId = UUID.Companion.random()
        val userId = UUID.Companion.random()
        val event = mockk<Event>()

        coEvery { repository.getEventById(eventId) } returns Result.success(event)
        coEvery { repository.getEventAttendees(eventId) } returns Result.success(
            listOf(
                UUID.Companion.random(),
                UUID.Companion.random()
            )
        )
        every { event.isFull(any()) } returns true

        val result = useCase.invoke(eventId, userId)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is EventFullException)
        coVerify { repository.getEventById(eventId) }
        coVerify { repository.getEventAttendees(eventId) }
        coVerify(exactly = 0) { repository.joinEvent(any(), any()) }
    }

    @Test
    fun `joins event successfully`() = runTest {
        val eventId = UUID.Companion.random()
        val userId = UUID.Companion.random()
        val event = mockk<Event>()

        coEvery { repository.getEventById(eventId) } returns Result.success(event)
        coEvery { repository.getEventAttendees(eventId) } returns Result.success(emptyList())
        every { event.isFull(any()) } returns false
        coEvery { repository.joinEvent(eventId, userId) } returns Result.success(Unit)

        val result = useCase.invoke(eventId, userId)

        Assert.assertTrue(result.isSuccess)
        coVerify { repository.getEventById(eventId) }
        coVerify { repository.getEventAttendees(eventId) }
        coVerify { repository.joinEvent(eventId, userId) }
    }
}