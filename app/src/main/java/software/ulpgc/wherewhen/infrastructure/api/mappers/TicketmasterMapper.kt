package software.ulpgc.wherewhen.infrastructure.api.mappers

import software.ulpgc.wherewhen.domain.model.events.*
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.infrastructure.api.models.TicketmasterEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TicketmasterMapper {
    
    fun toDomain(apiEvent: TicketmasterEvent): Event {
        val venue = apiEvent.embedded?.venues?.firstOrNull()
        val classification = apiEvent.classifications?.firstOrNull()
        
        return Event(
            id = UUID.random(),
            title = apiEvent.name,
            description = null,
            category = EventCategory.fromTicketmaster(classification?.segment?.name),
            location = Location(
                latitude = venue?.location?.latitude?.toDoubleOrNull() ?: 0.0,
                longitude = venue?.location?.longitude?.toDoubleOrNull() ?: 0.0,
                address = venue?.address?.line1,
                placeName = venue?.name,
                city = venue?.city?.name,
                country = venue?.country?.name
            ),
            dateTime = parseDateTime(apiEvent.dates.start),
            endDateTime = null,
            imageUrl = apiEvent.images?.firstOrNull()?.url,
            source = EventSource.EXTERNAL_API,
            organizerId = null,
            externalId = apiEvent.id,
            externalUrl = apiEvent.url,
            price = apiEvent.priceRanges?.firstOrNull()?.let {
                Price.range(it.min, it.max, it.currency)
            },
            distance = apiEvent.distance,
            status = EventStatus.ACTIVE,
            createdAt = LocalDateTime.now()
        )
    }
    
    private fun parseDateTime(start: software.ulpgc.wherewhen.infrastructure.api.models.TicketmasterStartDate): LocalDateTime {
        return try {
            when {
                start.dateTime != null -> 
                    LocalDateTime.parse(start.dateTime, DateTimeFormatter.ISO_DATE_TIME)
                start.localDate != null && start.localTime != null -> 
                    LocalDateTime.parse("${start.localDate}T${start.localTime}")
                start.localDate != null -> 
                    LocalDateTime.parse("${start.localDate}T00:00:00")
                else -> LocalDateTime.now()
            }
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}
