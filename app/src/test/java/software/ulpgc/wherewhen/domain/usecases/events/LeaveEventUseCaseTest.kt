package software.ulpgc.wherewhen.domain.usecases.events

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.events.NotAttendingEventException
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class LeaveEventUseCaseTest {

    private lateinit var repository: ExternalEventRepository
    private lateinit var useCase: LeaveEventUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = LeaveEventUseCase(repository)
    }

    @Test
    fun `fails when event not found`() = runTest {
        val eventId = UUID.Companion.random()
        val userId = UUID.Companion.random()

        coEvery { repository.getEventAttendees(eventId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(eventId, userId)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is EventNotFoundException)
        coVerify { repository.getEventAttendees(eventId) }
        coVerify(exactly = 0) { repository.leaveEvent(any(), any()) }
    }

    @Test
    fun `fails when user is not attending`() = runTest {
        val eventId = UUID.Companion.random()
        val userId = UUID.Companion.random()

        coEvery { repository.getEventAttendees(eventId) } returns Result.success(emptyList())

        val result = useCase.invoke(eventId, userId)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is NotAttendingEventException)
        coVerify { repository.getEventAttendees(eventId) }
        coVerify(exactly = 0) { repository.leaveEvent(any(), any()) }
    }

    @Test
    fun `leaves event successfully`() = runTest {
        val eventId = UUID.Companion.random()
        val userId = UUID.Companion.random()

        coEvery { repository.getEventAttendees(eventId) } returns Result.success(listOf(userId))
        coEvery { repository.leaveEvent(eventId, userId) } returns Result.success(Unit)

        val result = useCase.invoke(eventId, userId)

        Assert.assertTrue(result.isSuccess)
        coVerify { repository.getEventAttendees(eventId) }
        coVerify { repository.leaveEvent(eventId, userId) }
    }
}