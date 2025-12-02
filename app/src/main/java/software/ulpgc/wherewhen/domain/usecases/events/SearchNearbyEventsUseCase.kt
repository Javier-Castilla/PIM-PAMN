package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.exceptions.events.InvalidEventException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository

class SearchNearbyEventsUseCase(
    private val externalEventRepository: ExternalEventRepository
) {
    suspend operator fun invoke(location: Location, radiusKm: Int = 25): Result<List<Event>> {
        return try {
            if (radiusKm <= 0 || radiusKm > 500) {
                throw InvalidEventException("Radius must be between 1 and 500 km")
            }

            externalEventRepository.searchNearbyEvents(location, radiusKm)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
