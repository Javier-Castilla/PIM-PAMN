package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.exceptions.events.AlreadyAttendingEventException
import software.ulpgc.wherewhen.domain.exceptions.events.EventFullException
import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class JoinEventUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventUUID: UUID, userId: UUID): Result<Unit> {
        return try {
            val event = eventRepository.getEventById(eventUUID)
                .getOrElse { throw EventNotFoundException(eventUUID.value) }

            val attendees = eventRepository.getEventAttendees(eventUUID)
                .getOrElse { emptyList() }

            if (attendees.contains(userId)) {
                throw AlreadyAttendingEventException()
            }

            if (event.isFull(attendees.size)) {
                throw EventFullException()
            }

            eventRepository.joinEvent(eventUUID, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
