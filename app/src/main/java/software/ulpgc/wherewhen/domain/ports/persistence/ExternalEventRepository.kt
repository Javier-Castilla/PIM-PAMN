package software.ulpgc.wherewhen.domain.ports.persistence

import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import kotlinx.coroutines.flow.Flow

interface ExternalEventRepository {
    suspend fun searchNearbyEvents(
        location: Location,
        radiusKm: Int = 25
    ): Result<List<Event>>

    suspend fun searchEventsByCategory(
        location: Location,
        category: EventCategory,
        radiusKm: Int = 25
    ): Result<List<Event>>

    suspend fun searchEventsByName(
        location: Location,
        query: String,
        radiusKm: Int = 25
    ): Result<List<Event>>

    //TODO: borrar esto o que?
    suspend fun getAllNearbyEvents(
        location: Location,
        radiusKm: Int = 25
    ): Result<List<Event>>

    //TODO: borrar esto o que?
    suspend fun getAllEventsByCategory(
        location: Location,
        category: EventCategory,
        radiusKm: Int = 25
    ): Result<List<Event>>

    suspend fun getEventById(eventId: UUID): Result<Event>

    suspend fun createUserEvent(event: Event): Result<Event>

    suspend fun updateUserEvent(event: Event): Result<Event>

    suspend fun deleteUserEvent(eventId: UUID): Result<Unit>

    fun observeUserEvents(organizerId: UUID): Flow<List<Event>>

    suspend fun joinEvent(eventId: UUID, userId: UUID): Result<Unit>

    suspend fun leaveEvent(eventId: UUID, userId: UUID): Result<Unit>

    suspend fun getEventAttendees(eventId: UUID): Result<List<UUID>>

    suspend fun getUserJoinedEvents(userId: UUID): Result<List<Event>>

    suspend fun getUserCreatedEvents(userId: UUID): Result<List<Event>>
}
