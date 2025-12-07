package software.ulpgc.wherewhen.domain.model.events

import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

data class Event(
    val id: UUID,
    val title: String,
    val description: String?,
    val category: EventCategory,
    val location: Location,
    val dateTime: LocalDateTime,
    val endDateTime: LocalDateTime?,
    val imageUrl: String?,
    val source: EventSource,
    val organizerId: UUID?,
    val externalId: String?,
    val externalUrl: String?,
    val price: Price?,
    val distance: Double?,
    val status: EventStatus,
    val createdAt: LocalDateTime,
    val maxAttendees: Int? = null
) {
    fun isUserCreated(): Boolean = source == EventSource.USER_CREATED
    fun isOfficial(): Boolean = source == EventSource.EXTERNAL_API
    fun isFree(): Boolean = price?.isFree == true
    fun isNearby(userLocation: Location, radiusKm: Double): Boolean {
        return location.distanceTo(userLocation) <= radiusKm
    }
    fun hasCapacityLimit(): Boolean = maxAttendees != null
    fun isFull(currentAttendees: Int): Boolean =
        maxAttendees != null && currentAttendees >= maxAttendees
}
