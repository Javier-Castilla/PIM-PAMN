package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetEventByIdUseCase (
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventUUID: UUID): Result<Event> {
        return try {
            eventRepository.getEventById(eventUUID)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
