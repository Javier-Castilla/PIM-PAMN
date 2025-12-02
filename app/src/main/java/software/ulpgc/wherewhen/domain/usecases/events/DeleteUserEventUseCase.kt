package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class DeleteUserEventUseCase(
    private val externalEventRepository: ExternalEventRepository
) {
    suspend operator fun invoke(eventId: UUID): Result<Unit> {
        return try {
            externalEventRepository.getEventById(eventId)
                .getOrElse { throw EventNotFoundException(eventId.value) }

            externalEventRepository.deleteUserEvent(eventId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
