package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.events.NotAttendingEventException
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class LeaveEventUseCase(
    private val externalEventRepository: ExternalEventRepository
) {
    suspend operator fun invoke(eventUUID: UUID, userId: UUID): Result<Unit> {
        return try {
            val attendees = externalEventRepository.getEventAttendees(eventUUID)
                .getOrElse { throw EventNotFoundException(eventUUID.value) }

            if (!attendees.contains(userId)) {
                throw NotAttendingEventException()
            }

            externalEventRepository.leaveEvent(eventUUID, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
