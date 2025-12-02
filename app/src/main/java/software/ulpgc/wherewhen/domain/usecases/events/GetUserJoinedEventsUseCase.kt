package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class GetUserJoinedEventsUseCase (
    private val externalEventRepository: ExternalEventRepository
) {
    suspend operator fun invoke(userId: UUID): Result<List<Event>> {
        return try {
            externalEventRepository.getUserJoinedEvents(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
