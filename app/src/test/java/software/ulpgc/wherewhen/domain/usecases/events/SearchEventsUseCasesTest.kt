package software.ulpgc.wherewhen.domain.usecases.events

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.events.InvalidEventException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository

class SearchEventsUseCasesTest {

    private lateinit var repository: ExternalEventRepository
    private lateinit var nearbyUseCase: SearchNearbyEventsUseCase
    private lateinit var byCategoryUseCase: SearchEventsByCategoryUseCase
    private lateinit var byNameUseCase: SearchEventsByNameUseCase

    @Before
    fun setup() {
        repository = mockk()
        nearbyUseCase = SearchNearbyEventsUseCase(repository)
        byCategoryUseCase = SearchEventsByCategoryUseCase(repository)
        byNameUseCase = SearchEventsByNameUseCase(repository)
    }

    @Test
    fun `search nearby events fails on invalid radius`() = runTest {
        val location = mockk<Location>()

        val result = nearbyUseCase.invoke(location, radiusKm = 0)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is InvalidEventException)
        coVerify(exactly = 0) { repository.searchNearbyEvents(any(), any()) }
    }

    @Test
    fun `search nearby events delegates to repository`() = runTest {
        val location = mockk<Location>()
        val events = listOf(mockk<Event>())

        coEvery { repository.searchNearbyEvents(location, 25) } returns Result.success(events)

        val result = nearbyUseCase.invoke(location, 25)

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(events, result.getOrNull())
        coVerify { repository.searchNearbyEvents(location, 25) }
    }

    @Test
    fun `search by category fails on invalid radius`() = runTest {
        val location = mockk<Location>()
        val category = mockk<EventCategory>()

        val result = byCategoryUseCase.invoke(location, category, radiusKm = 600)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is InvalidEventException)
        coVerify(exactly = 0) { repository.searchEventsByCategory(any(), any(), any()) }
    }

    @Test
    fun `search by category delegates to repository`() = runTest {
        val location = mockk<Location>()
        val category = mockk<EventCategory>()
        val events = listOf(mockk<Event>())

        coEvery {
            repository.searchEventsByCategory(
                location,
                category,
                25
            )
        } returns Result.success(events)

        val result = byCategoryUseCase.invoke(location, category, 25)

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(events, result.getOrNull())
        coVerify { repository.searchEventsByCategory(location, category, 25) }
    }

    @Test
    fun `search by name fails on blank query`() = runTest {
        val location = mockk<Location>()

        val result = byNameUseCase.invoke(location, query = "", radiusKm = 25)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is InvalidEventException)
        coVerify(exactly = 0) { repository.searchEventsByName(any(), any(), any()) }
    }

    @Test
    fun `search by name fails on invalid radius`() = runTest {
        val location = mockk<Location>()

        val result = byNameUseCase.invoke(location, query = "rock", radiusKm = 0)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is InvalidEventException)
        coVerify(exactly = 0) { repository.searchEventsByName(any(), any(), any()) }
    }

    @Test
    fun `search by name delegates to repository`() = runTest {
        val location = mockk<Location>()
        val events = listOf(mockk<Event>())

        coEvery { repository.searchEventsByName(location, "rock", 25) } returns Result.success(
            events
        )

        val result = byNameUseCase.invoke(location, query = "rock", radiusKm = 25)

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(events, result.getOrNull())
        coVerify { repository.searchEventsByName(location, "rock", 25) }
    }
}