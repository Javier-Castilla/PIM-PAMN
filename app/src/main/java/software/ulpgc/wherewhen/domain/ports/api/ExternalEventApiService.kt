package software.ulpgc.wherewhen.domain.ports.api

import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.Location

interface ExternalEventApiService {
    suspend fun searchNearbyEvents(
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): Result<List<Event>>
    
    suspend fun searchEventsByCategory(
        latitude: Double,
        longitude: Double,
        category: EventCategory,
        radiusKm: Int
    ): Result<List<Event>>
}
