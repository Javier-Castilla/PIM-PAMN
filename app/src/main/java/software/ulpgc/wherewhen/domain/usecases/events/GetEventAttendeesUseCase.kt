package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetEventAttendeesUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventUUID: UUID): Result<List<UUID>> {
        return try {
            eventRepository.getEventAttendees(eventUUID)
                .onFailure { throw EventNotFoundException(eventUUID.value) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
