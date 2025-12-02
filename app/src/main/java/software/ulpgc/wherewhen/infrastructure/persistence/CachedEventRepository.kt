package software.ulpgc.wherewhen.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class CachedEventRepository(
    private val decorated: ExternalEventRepository
) : ExternalEventRepository {
    private val eventCache = mutableMapOf<String, Event>()

    override suspend fun searchNearbyEvents(
        location: Location,
        radiusKm: Int
    ): Result<List<Event>> {
        return decorated.searchNearbyEvents(location, radiusKm).onSuccess { events ->
            events.forEach { event ->
                eventCache[event.id.value] = event
            }
        }
    }

    override suspend fun searchEventsByCategory(
        location: Location,
        category: EventCategory,
        radiusKm: Int
    ): Result<List<Event>> {
        return decorated.searchEventsByCategory(location, category, radiusKm).onSuccess { events ->
            events.forEach { event ->
                eventCache[event.id.value] = event
            }
        }
    }

    override suspend fun searchEventsByName(
        location: Location,
        query: String,
        radiusKm: Int
    ): Result<List<Event>> {
        return decorated.searchEventsByName(location, query, radiusKm).onSuccess { events ->
            events.forEach { event ->
                eventCache[event.id.value] = event
            }
        }
    }

    override suspend fun getAllNearbyEvents(
        location: Location,
        radiusKm: Int
    ): Result<List<Event>> {
        return decorated.getAllNearbyEvents(location, radiusKm).onSuccess { events ->
            events.forEach { event ->
                eventCache[event.id.value] = event
            }
        }
    }

    override suspend fun getAllEventsByCategory(
        location: Location,
        category: EventCategory,
        radiusKm: Int
    ): Result<List<Event>> {
        return decorated.getAllEventsByCategory(location, category, radiusKm).onSuccess { events ->
            events.forEach { event ->
                eventCache[event.id.value] = event
            }
        }
    }

    override suspend fun getEventById(eventId: UUID): Result<Event> {
        val cachedEvent = eventCache[eventId.value]
        return if (cachedEvent != null) {
            Result.success(cachedEvent)
        } else {
            decorated.getEventById(eventId).onSuccess { event ->
                eventCache[event.id.value] = event
            }
        }
    }

    override suspend fun createUserEvent(event: Event): Result<Event> {
        return decorated.createUserEvent(event).onSuccess { createdEvent ->
            eventCache[createdEvent.id.value] = createdEvent
        }
    }

    override suspend fun updateUserEvent(event: Event): Result<Event> {
        return decorated.updateUserEvent(event).onSuccess { updatedEvent ->
            eventCache[updatedEvent.id.value] = updatedEvent
        }
    }

    override suspend fun deleteUserEvent(eventId: UUID): Result<Unit> {
        return decorated.deleteUserEvent(eventId).onSuccess {
            eventCache.remove(eventId.value)
        }
    }

    override fun observeUserEvents(organizerId: UUID): Flow<List<Event>> {
        return decorated.observeUserEvents(organizerId)
    }

    override suspend fun joinEvent(eventId: UUID, userId: UUID): Result<Unit> {
        return decorated.joinEvent(eventId, userId)
    }

    override suspend fun leaveEvent(eventId: UUID, userId: UUID): Result<Unit> {
        return decorated.leaveEvent(eventId, userId)
    }

    override suspend fun getEventAttendees(eventId: UUID): Result<List<UUID>> {
        return decorated.getEventAttendees(eventId)
    }

    override suspend fun getUserJoinedEvents(userId: UUID): Result<List<Event>> {
        return decorated.getUserJoinedEvents(userId).onSuccess { events ->
            events.forEach { event ->
                eventCache[event.id.value] = event
            }
        }
    }

    override suspend fun getUserCreatedEvents(userId: UUID): Result<List<Event>> {
        return decorated.getUserCreatedEvents(userId).onSuccess { events ->
            events.forEach { event ->
                eventCache[event.id.value] = event
            }
        }
    }
}
