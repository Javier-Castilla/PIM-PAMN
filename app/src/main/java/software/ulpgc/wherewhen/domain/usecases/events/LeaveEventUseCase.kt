package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.events.NotAttendingEventException
import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class LeaveEventUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventUUID: UUID, userId: UUID): Result<Unit> {
        return try {
            val attendees = eventRepository.getEventAttendees(eventUUID)
                .getOrElse { throw EventNotFoundException(eventUUID.value) }

            if (!attendees.contains(userId)) {
                throw NotAttendingEventException()
            }

            eventRepository.leaveEvent(eventUUID, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
