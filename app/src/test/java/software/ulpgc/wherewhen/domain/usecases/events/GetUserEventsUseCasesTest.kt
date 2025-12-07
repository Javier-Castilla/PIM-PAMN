package software.ulpgc.wherewhen.domain.usecases.events

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetUserEventsUseCasesTest {

    private lateinit var repository: ExternalEventRepository
    private lateinit var createdUseCase: GetUserCreatedEventsUseCase
    private lateinit var joinedUseCase: GetUserJoinedEventsUseCase

    @Before
    fun setup() {
        repository = mockk()
        createdUseCase = GetUserCreatedEventsUseCase(repository)
        joinedUseCase = GetUserJoinedEventsUseCase(repository)
    }

    @Test
    fun `get user created events delegates to repository`() = runTest {
        val userId = UUID.Companion.random()
        val events = listOf(mockk<Event>())

        coEvery { repository.getUserCreatedEvents(userId) } returns Result.success(events)

        val result = createdUseCase.invoke(userId)

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(events, result.getOrNull())
        coVerify { repository.getUserCreatedEvents(userId) }
    }

    @Test
    fun `get user joined events delegates to repository`() = runTest {
        val userId = UUID.Companion.random()
        val events = listOf(mockk<Event>())

        coEvery { repository.getUserJoinedEvents(userId) } returns Result.success(events)

        val result = joinedUseCase.invoke(userId)

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(events, result.getOrNull())
        coVerify { repository.getUserJoinedEvents(userId) }
    }
}