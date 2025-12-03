package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.exceptions.events.InvalidEventException
import software.ulpgc.wherewhen.domain.model.events.*
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class CreateUserEventUseCase(
    private val externalEventRepository: ExternalEventRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String?,
        location: Location,
        dateTime: LocalDateTime,
        endDateTime: LocalDateTime?,
        category: EventCategory,
        organizerId: UUID,
        maxAttendees: Int?,
        imageUrl: String? = null,
        price: Price? = null
    ): Result<Event> {
        return try {
            if (title.isBlank()) {
                throw InvalidEventException("Event title cannot be empty")
            }

            if (dateTime.isBefore(LocalDateTime.now())) {
                throw InvalidEventException("Event date must be in the future")
            }

            if (endDateTime != null && endDateTime.isBefore(dateTime)) {
                throw InvalidEventException("End date must be after start date")
            }

            if (maxAttendees != null && maxAttendees <= 0) {
                throw InvalidEventException("Max attendees must be greater than 0")
            }

            val event = Event(
                id = UUID.random(),
                title = title,
                description = description,
                category = category,
                location = location,
                dateTime = dateTime,
                endDateTime = endDateTime,
                imageUrl = imageUrl,
                source = EventSource.USER_CREATED,
                organizerId = organizerId,
                externalId = null,
                externalUrl = null,
                price = price,
                distance = null,
                status = EventStatus.ACTIVE,
                createdAt = LocalDateTime.now(),
                maxAttendees = maxAttendees
            )

            val result = externalEventRepository.createUserEvent(event)
            result.onSuccess { createdEvent ->
                externalEventRepository.joinEvent(createdEvent.id, organizerId)
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
