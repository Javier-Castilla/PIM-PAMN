package software.ulpgc.wherewhen.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.api.ExternalEventApiService
import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class CompositeEventRepository(
    private val externalEventApiService: ExternalEventApiService,
    private val firebaseEventRepository: FirebaseEventRepository
) : EventRepository {

    override suspend fun searchNearbyEvents(
        location: Location,
        radiusKm: Int
    ): Result<List<Event>> {
        return try {
            val events = externalEventApiService.searchNearbyEvents(
                location.latitude,
                location.longitude,
                radiusKm
            ).getOrElse { emptyList() }

            val uniqueEvents = events.distinctBy { it.externalId }
            Result.success(uniqueEvents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchEventsByCategory(
        location: Location,
        category: EventCategory,
        radiusKm: Int
    ): Result<List<Event>> {
        return try {
            val events = externalEventApiService.searchEventsByCategory(
                location.latitude,
                location.longitude,
                category,
                radiusKm
            ).getOrElse { emptyList() }

            val uniqueEvents = events.distinctBy { it.externalId }
            Result.success(uniqueEvents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchEventsByName(
        location: Location,
        query: String,
        radiusKm: Int
    ): Result<List<Event>> {
        return try {
            val allEvents = externalEventApiService.searchNearbyEvents(
                location.latitude,
                location.longitude,
                radiusKm
            ).getOrElse { emptyList() }

            val filtered = allEvents
                .filter { event -> event.title.contains(query, ignoreCase = true) }
                .distinctBy { it.externalId }

            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllNearbyEvents(
        location: Location,
        radiusKm: Int
    ): Result<List<Event>> {
        return try {
            val externalEvents = externalEventApiService.searchNearbyEvents(
                location.latitude,
                location.longitude,
                radiusKm
            ).getOrElse { emptyList() }

            val userEvents = firebaseEventRepository.getUserEventsByLocation(
                UUID.random(),
                location.latitude,
                location.longitude,
                radiusKm.toDouble()
            ).getOrElse { emptyList() }

            val combined = (externalEvents + userEvents).sortedBy { it.dateTime }
            Result.success(combined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllEventsByCategory(
        location: Location,
        category: EventCategory,
        radiusKm: Int
    ): Result<List<Event>> {
        return try {
            val externalEvents = externalEventApiService.searchEventsByCategory(
                location.latitude,
                location.longitude,
                category,
                radiusKm
            ).getOrElse { emptyList() }

            val userEvents = firebaseEventRepository.getUserEventsByLocation(
                UUID.random(),
                location.latitude,
                location.longitude,
                radiusKm.toDouble()
            ).getOrElse { emptyList() }
                .filter { it.category == category }

            val combined = (externalEvents + userEvents).sortedBy { it.dateTime }
            Result.success(combined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEventById(eventId: UUID): Result<Event> {
        return firebaseEventRepository.getEventById(eventId)
    }

    override suspend fun createUserEvent(event: Event): Result<Event> {
        return firebaseEventRepository.createUserEvent(event)
    }

    override suspend fun updateUserEvent(event: Event): Result<Event> {
        return firebaseEventRepository.updateUserEvent(event)
    }

    override suspend fun deleteUserEvent(eventId: UUID): Result<Unit> {
        return firebaseEventRepository.deleteUserEvent(eventId)
    }

    override fun observeUserEvents(organizerId: UUID): Flow<List<Event>> {
        return firebaseEventRepository.observeUserEvents(organizerId)
    }

    override suspend fun joinEvent(eventId: UUID, userId: UUID): Result<Unit> {
        return firebaseEventRepository.joinEvent(eventId, userId)
    }

    override suspend fun leaveEvent(eventId: UUID, userId: UUID): Result<Unit> {
        return firebaseEventRepository.leaveEvent(eventId, userId)
    }

    override suspend fun getEventAttendees(eventId: UUID): Result<List<UUID>> {
        return firebaseEventRepository.getEventAttendees(eventId)
    }

    override suspend fun getUserJoinedEvents(userId: UUID): Result<List<Event>> {
        return firebaseEventRepository.getUserJoinedEvents(userId)
    }

    override suspend fun getUserCreatedEvents(userId: UUID): Result<List<Event>> {
        return try {
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
