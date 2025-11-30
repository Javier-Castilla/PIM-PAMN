package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class UpdateUserEventUseCase(
    private val eventRepository: EventRepository
) {

    suspend operator fun invoke(
        eventId: UUID,
        newTitle: String? = null,
        newLocation: Location? = null,
        newDateTime: LocalDateTime? = null,
        newEndDateTime: LocalDateTime? = null
    ): Result<Event> {
        return try {
            // 1. Load the original event
            val existingEventResult = eventRepository.getEventById(eventId)
            val existingEvent = existingEventResult.getOrElse {
                return Result.failure(it)
            }

            // 2. Create the updated event
            val updatedEvent = existingEvent.copy(
                title = newTitle ?: existingEvent.title,
                location = newLocation ?: existingEvent.location,
                dateTime = newDateTime ?: existingEvent.dateTime,
                endDateTime = newEndDateTime ?: existingEvent.endDateTime
            )

            // 3. Save update
            eventRepository.updateUserEvent(updatedEvent)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
