package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository;

class SearchNearbyEventsUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(location: Location, radiusKm: Int = 25): Result<List<Event>> {
        return try {
            eventRepository.searchNearbyEvents(location, radiusKm)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}