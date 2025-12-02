package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetEventByIdUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventUUID: UUID): Result<Event> {
        return try {
            eventRepository.getEventById(eventUUID)
                .onFailure { throw EventNotFoundException(eventUUID.value) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
