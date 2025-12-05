package software.ulpgc.wherewhen.domain.ports.persistence

import kotlinx.coroutines.flow.Flow
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.valueObjects.UUID

interface UserEventRepository {
    suspend fun getEventById(eventId: UUID): Result<Event>
    suspend fun createUserEvent(event: Event): Result<Event>
    suspend fun updateUserEvent(event: Event): Result<Event>
    suspend fun deleteUserEvent(eventId: UUID): Result<Unit>
    fun observeUserEvents(organizerId: UUID): Flow<List<Event>>
    suspend fun getUserEventsByLocation(
        organizerId: UUID,
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Result<List<Event>>
    suspend fun joinEvent(eventId: UUID, userId: UUID): Result<Unit>
    suspend fun leaveEvent(eventId: UUID, userId: UUID): Result<Unit>
    suspend fun getEventAttendees(eventId: UUID): Result<List<UUID>>
    suspend fun getUserJoinedEvents(userId: UUID): Result<List<Event>>
    suspend fun getUserCreatedEvents(userId: UUID): Result<List<Event>>
}
