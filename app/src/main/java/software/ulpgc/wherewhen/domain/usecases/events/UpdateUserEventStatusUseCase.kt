package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.events.UnauthorizedEventAccessException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventStatus
import software.ulpgc.wherewhen.domain.ports.persistence.UserEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class UpdateUserEventStatusUseCase(
    private val userEventRepository: UserEventRepository
) {
    suspend operator fun invoke(
        eventId: UUID,
        newStatus: EventStatus,
        organizerId: UUID
    ): Result<Event> {
        return try {
            val existingEvent = userEventRepository.getEventById(eventId)
                .getOrElse { throw EventNotFoundException(eventId.value) }

            if (existingEvent.organizerId != organizerId) {
                throw UnauthorizedEventAccessException()
            }

            val updatedEvent = existingEvent.copy(status = newStatus)

            userEventRepository.updateUserEvent(updatedEvent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
