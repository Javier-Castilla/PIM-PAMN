package software.ulpgc.wherewhen.domain.usecases.events

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetEventByIdUseCaseTest {

    private lateinit var repository: ExternalEventRepository
    private lateinit var useCase: GetEventByIdUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetEventByIdUseCase(repository)
    }

    @Test
    fun `returns event when found`() = runTest {
        val eventId = UUID.random()
        val event = mockk<Event>()

        coEvery { repository.getEventById(eventId) } returns Result.success(event)

        val result = useCase.invoke(eventId)

        assertTrue(result.isSuccess)
        assertEquals(event, result.getOrNull())
        coVerify { repository.getEventById(eventId) }
    }

    @Test
    fun `fails with EventNotFoundException when repository fails`() = runTest {
        val eventId = UUID.random()

        coEvery { repository.getEventById(eventId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(eventId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is EventNotFoundException)
        coVerify { repository.getEventById(eventId) }
    }
}
