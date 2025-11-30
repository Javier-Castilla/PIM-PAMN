package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class JoinEventUseCase (
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventUUID: UUID, userId: UUID): Result<Unit> {
        return try {
            eventRepository.joinEvent(eventUUID, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
