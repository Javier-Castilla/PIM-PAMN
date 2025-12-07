package software.ulpgc.wherewhen.domain.usecases.events

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetEventAttendeesUseCaseTest {

    private lateinit var repository: ExternalEventRepository
    private lateinit var useCase: GetEventAttendeesUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetEventAttendeesUseCase(repository)
    }

    @Test
    fun `returns attendees when event exists`() = runTest {
        val eventId = UUID.Companion.random()
        val userId = UUID.Companion.random()
        val attendees = listOf(userId)

        coEvery { repository.getEventAttendees(eventId) } returns Result.success(attendees)

        val result = useCase.invoke(eventId)

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(attendees, result.getOrNull())
        coVerify { repository.getEventAttendees(eventId) }
    }

    @Test
    fun `fails with EventNotFoundException when repository fails`() = runTest {
        val eventId = UUID.Companion.random()

        coEvery { repository.getEventAttendees(eventId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(eventId)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is EventNotFoundException)
        coVerify { repository.getEventAttendees(eventId) }
    }
}