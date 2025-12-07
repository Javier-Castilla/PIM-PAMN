package software.ulpgc.wherewhen.domain.usecases.events

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class DeleteUserEventUseCaseTest {

    private lateinit var repository: ExternalEventRepository
    private lateinit var useCase: DeleteUserEventUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = DeleteUserEventUseCase(repository)
    }

    @Test
    fun `deletes event successfully`() = runTest {
        val eventId = UUID.Companion.random()
        val event = mockk<Event>()

        coEvery { repository.getEventById(eventId) } returns Result.success(event)
        coEvery { repository.deleteUserEvent(eventId) } returns Result.success(Unit)

        val result = useCase.invoke(eventId)

        Assert.assertTrue(result.isSuccess)
        coVerify { repository.getEventById(eventId) }
        coVerify { repository.deleteUserEvent(eventId) }
    }

    @Test
    fun `fails when event not found`() = runTest {
        val eventId = UUID.Companion.random()

        coEvery { repository.getEventById(eventId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(eventId)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is EventNotFoundException)
        coVerify { repository.getEventById(eventId) }
        coVerify(exactly = 0) { repository.deleteUserEvent(eventId) }
    }
}