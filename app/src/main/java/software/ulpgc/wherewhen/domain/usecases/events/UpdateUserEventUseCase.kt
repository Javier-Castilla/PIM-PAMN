package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.exceptions.events.EventNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.events.InvalidEventException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.model.events.Price
import software.ulpgc.wherewhen.domain.ports.persistence.UserEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class UpdateUserEventUseCase(
    private val userEventRepository: UserEventRepository
) {
    suspend operator fun invoke(
        eventId: UUID,
        newTitle: String? = null,
        newLocation: Location? = null,
        newDateTime: LocalDateTime? = null,
        newEndDateTime: LocalDateTime? = null,
        newImageUrl: String? = null,
        newPrice: Price? = null
    ): Result<Event> {
        return try {
            val existingEvent = userEventRepository.getEventById(eventId)
                .getOrElse { throw EventNotFoundException(eventId.value) }

            val title = newTitle ?: existingEvent.title
            if (title.isBlank()) {
                throw InvalidEventException("Event title cannot be empty")
            }

            val dateTime = newDateTime ?: existingEvent.dateTime
            val endDateTime = newEndDateTime ?: existingEvent.endDateTime
            if (endDateTime != null && endDateTime.isBefore(dateTime)) {
                throw InvalidEventException("End date must be after start date")
            }

            val updatedEvent = existingEvent.copy(
                title = title,
                location = newLocation ?: existingEvent.location,
                dateTime = dateTime,
                endDateTime = endDateTime,
                imageUrl = newImageUrl ?: existingEvent.imageUrl,
                price = newPrice ?: existingEvent.price
            )

            userEventRepository.updateUserEvent(updatedEvent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
