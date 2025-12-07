package software.ulpgc.wherewhen.infrastructure.persistence.mappers

import com.google.firebase.firestore.DocumentSnapshot
import software.ulpgc.wherewhen.domain.model.events.*
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FirebaseEventMapper {

    fun fromFirestore(snapshot: DocumentSnapshot): Event? {
        return try {
            Event(
                id = UUID.parse(snapshot.getString("id") ?: return null).getOrNull() ?: return null,
                title = snapshot.getString("title") ?: return null,
                description = snapshot.getString("description"),
                category = EventCategory.valueOf(snapshot.getString("category") ?: "OTHER"),
                location = Location(
                    latitude = snapshot.getDouble("latitude") ?: 0.0,
                    longitude = snapshot.getDouble("longitude") ?: 0.0,
                    address = snapshot.getString("address"),
                    placeName = snapshot.getString("placeName"),
                    city = snapshot.getString("city"),
                    country = snapshot.getString("country")
                ),
                dateTime = LocalDateTime.parse(snapshot.getString("dateTime") ?: return null),
                endDateTime = snapshot.getString("endDateTime")?.let { LocalDateTime.parse(it) },
                imageUrl = snapshot.getString("imageUrl"),
                source = EventSource.valueOf(snapshot.getString("source") ?: "USER_CREATED"),
                organizerId = snapshot.getString("organizerId")?.let {
                    UUID.parse(it).getOrNull()
                },
                externalId = snapshot.getString("externalId"),
                externalUrl = snapshot.getString("externalUrl"),
                price = if (snapshot.getBoolean("isFree") == true) {
                    Price.free()
                } else {
                    val min = snapshot.getDouble("priceMin")
                    val max = snapshot.getDouble("priceMax")
                    val currency = snapshot.getString("priceCurrency") ?: "EUR"
                    if (min != null && max != null) {
                        Price.range(min, max, currency)
                    } else {
                        null
                    }
                },
                distance = snapshot.getDouble("distance"),
                status = EventStatus.valueOf(snapshot.getString("status") ?: "ACTIVE"),
                createdAt = LocalDateTime.parse(
                    snapshot.getString("createdAt") ?: LocalDateTime.now().toString()
                ),
                maxAttendees = snapshot.getLong("maxAttendees")?.toInt()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun toFirestore(event: Event): Map<String, Any?> {
        return mapOf(
            "id" to event.id.value,
            "title" to event.title,
            "description" to event.description,
            "category" to event.category.name,
            "latitude" to event.location.latitude,
            "longitude" to event.location.longitude,
            "address" to event.location.address,
            "placeName" to event.location.placeName,
            "city" to event.location.city,
            "country" to event.location.country,
            "dateTime" to event.dateTime.format(DateTimeFormatter.ISO_DATE_TIME),
            "endDateTime" to event.endDateTime?.format(DateTimeFormatter.ISO_DATE_TIME),
            "imageUrl" to event.imageUrl,
            "source" to event.source.name,
            "organizerId" to event.organizerId?.value,
            "externalId" to event.externalId,
            "externalUrl" to event.externalUrl,
            "isFree" to event.price?.isFree,
            "priceMin" to event.price?.min,
            "priceMax" to event.price?.max,
            "priceCurrency" to event.price?.currency,
            "distance" to event.distance,
            "status" to event.status.name,
            "createdAt" to event.createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
            "maxAttendees" to event.maxAttendees
        )
    }
}
