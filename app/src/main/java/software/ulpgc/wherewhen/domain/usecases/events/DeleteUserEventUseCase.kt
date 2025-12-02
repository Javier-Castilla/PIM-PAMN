package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class DeleteUserEventUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: UUID): Result<Unit> {
        return try {
            eventRepository.getEventById(eventId)
                .getOrElse { throw EventNotFoundException(eventId.value) }

            eventRepository.deleteUserEvent(eventId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
