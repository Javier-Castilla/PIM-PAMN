package software.ulpgc.wherewhen.domain.usecases.events

import software.ulpgc.wherewhen.domain.model.events.*
import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class CreateUserEventUseCase(
    private val eventRepository: EventRepository
) {

    suspend operator fun invoke(
        title: String,
        description: String?,
        category: EventCategory,
        location: Location,
        dateTime: LocalDateTime,
        endDateTime: LocalDateTime?,
        imageUrl: String?,
        organizerId: UUID,
        price: Price?
    ): Result<Event> {
        return try {
            val event = Event(
                id = UUID.random(),                 // Javi is this ok for random generating or no?
                title = title,
                description = description,
                category = category,
                location = location,
                dateTime = dateTime,
                endDateTime = endDateTime,
                imageUrl = imageUrl,
                source = EventSource.USER_CREATED, // important
                organizerId = organizerId,
                externalId = null,
                externalUrl = null,
                price = price,
                distance = null,
                status = EventStatus.ACTIVE,
                createdAt = LocalDateTime.now()
            )

            eventRepository.createUserEvent(event)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
