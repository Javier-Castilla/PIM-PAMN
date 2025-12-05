package software.ulpgc.wherewhen.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.api.ExternalEventApiService
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class CompositeEventRepository(
    private val externalEventApiService: ExternalEventApiService,
    private val userEventRepository: UserEventRepository
) : ExternalEventRepository {

    override suspend fun searchNearbyEvents(
        location: Location,
        radiusKm: Int
    ): Result<List<Event>> {
        return try {
            println("🔍 CompositeEventRepository: Iniciando búsqueda nearby")
            println("🔍 Location: ${location.latitude}, ${location.longitude}, radius: $radiusKm km")

            val externalEvents = externalEventApiService.searchNearbyEvents(
                location.latitude,
                location.longitude,
                radiusKm
            ).getOrElse { error ->
                println("❌ Error en externalEventApiService: ${error.message}")
                emptyList()
            }
            println("🎫 Ticketmaster devolvió: ${externalEvents.size} eventos")
            externalEvents.forEach { println("   - ${it.title} (source: ${it.source})") }

            val userEvents = userEventRepository.getUserEventsByLocation(
                UUID.random(),
                location.latitude,
                location.longitude,
                radiusKm.toDouble()
            ).getOrElse { error ->
                println("❌ Error en userEventRepository: ${error.message}")
                emptyList()
            }
            println("👤 Firebase devolvió: ${userEvents.size} eventos")
            userEvents.forEach { println("   - ${it.title} (source: ${it.source})") }

            val combined = (externalEvents + userEvents)
                .distinctBy { it.id }
                .sortedBy { it.dateTime }

            println("✅ Total combinado después de distinctBy: ${combined.size} eventos")
            combined.forEach { println("   - ${it.title} (source: ${it.source})") }

            Result.success(combined)
        } catch (e: Exception) {
            println("💥 Exception en CompositeEventRepository: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun searchEventsByCategory(
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

            val userEvents = userEventRepository.getUserEventsByLocation(
                UUID.random(),
                location.latitude,
                location.longitude,
                radiusKm.toDouble()
            ).getOrElse { emptyList() }
                .filter { it.category == category }

            val combined = (externalEvents + userEvents)
                .distinctBy { it.id }
                .sortedBy { it.dateTime }

            Result.success(combined)
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
            val externalEvents = externalEventApiService.searchNearbyEvents(
                location.latitude,
                location.longitude,
                radiusKm
            ).getOrElse { emptyList() }
                .filter { event -> event.title.contains(query, ignoreCase = true) }

            val userEvents = userEventRepository.getUserEventsByLocation(
                UUID.random(),
                location.latitude,
                location.longitude,
                radiusKm.toDouble()
            ).getOrElse { emptyList() }
                .filter { event -> event.title.contains(query, ignoreCase = true) }

            val combined = (externalEvents + userEvents)
                .distinctBy { it.id }
                .sortedBy { it.dateTime }

            Result.success(combined)
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

            val userEvents = userEventRepository.getUserEventsByLocation(
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

            val userEvents = userEventRepository.getUserEventsByLocation(
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
        return userEventRepository.getEventById(eventId)
    }

    override suspend fun createUserEvent(event: Event): Result<Event> {
        return userEventRepository.createUserEvent(event)
    }

    override suspend fun updateUserEvent(event: Event): Result<Event> {
        return userEventRepository.updateUserEvent(event)
    }

    override suspend fun deleteUserEvent(eventId: UUID): Result<Unit> {
        return userEventRepository.deleteUserEvent(eventId)
    }

    override fun observeUserEvents(organizerId: UUID): Flow<List<Event>> {
        return userEventRepository.observeUserEvents(organizerId)
    }

    override suspend fun joinEvent(eventId: UUID, userId: UUID): Result<Unit> {
        return userEventRepository.joinEvent(eventId, userId)
    }

    override suspend fun leaveEvent(eventId: UUID, userId: UUID): Result<Unit> {
        return userEventRepository.leaveEvent(eventId, userId)
    }

    override suspend fun getEventAttendees(eventId: UUID): Result<List<UUID>> {
        return userEventRepository.getEventAttendees(eventId)
    }

    override suspend fun getUserJoinedEvents(userId: UUID): Result<List<Event>> {
        return userEventRepository.getUserJoinedEvents(userId)
    }

    override suspend fun getUserCreatedEvents(userId: UUID): Result<List<Event>> {
        return userEventRepository.getUserCreatedEvents(userId)
    }
}
