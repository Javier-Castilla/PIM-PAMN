package software.ulpgc.wherewhen.infrastructure.persistence

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import software.ulpgc.wherewhen.domain.model.events.*
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.infrastructure.persistence.mappers.FirebaseEventMapper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FirebaseEventRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val eventsCollection = firestore.collection("events")
    private val attendanceCollection = firestore.collection("event_attendance")

    suspend fun getEventById(eventId: UUID): Result<Event> {
        return try {
            val snapshot = eventsCollection.document(eventId.value).get().await()
            if (snapshot.exists()) {
                val event = FirebaseEventMapper.fromFirestore(snapshot)
                    ?: return Result.failure(Exception("Failed to parse event"))
                Result.success(event)
            } else {
                Result.failure(Exception("Event not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserEvent(event: Event): Result<Event> {
        return try {
            val eventMap = FirebaseEventMapper.toFirestore(event)
            eventsCollection.document(event.id.value).set(eventMap).await()
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserEvent(event: Event): Result<Event> {
        return try {
            val eventMap = FirebaseEventMapper.toFirestore(event)
            eventsCollection.document(event.id.value).set(eventMap).await()
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserEvent(eventId: UUID): Result<Unit> {
        return try {
            eventsCollection.document(eventId.value).delete().await()
            attendanceCollection.whereEqualTo("eventId", eventId.value).get().await()
                .documents.forEach { it.reference.delete() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUserEvents(organizerId: UUID): Flow<List<Event>> = callbackFlow {
        val listener = eventsCollection
            .whereEqualTo("organizerId", organizerId.value)
            .whereEqualTo("source", EventSource.USER_CREATED.name)
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull {
                    FirebaseEventMapper.fromFirestore(it)
                } ?: emptyList()
                trySend(events)
            }

        awaitClose { listener.remove() }
    }

    suspend fun getUserEventsByLocation(
        organizerId: UUID,
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Result<List<Event>> {
        return try {
            val events = eventsCollection
                .whereEqualTo("organizerId", organizerId.value)
                .whereEqualTo("source", EventSource.USER_CREATED.name)
                .get()
                .await()
                .documents
                .mapNotNull { FirebaseEventMapper.fromFirestore(it) }
                .filter { event ->
                    val distance = calculateDistance(
                        latitude, longitude,
                        event.location.latitude, event.location.longitude
                    )
                    distance <= radiusKm
                }

            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinEvent(eventId: UUID, userId: UUID): Result<Unit> {
        return try {
            val attendanceId = "${eventId.value}_${userId.value}"
            val attendanceMap = mapOf(
                "id" to attendanceId,
                "eventId" to eventId.value,
                "userId" to userId.value,
                "status" to AttendanceStatus.GOING.name,
                "joinedAt" to LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            )
            attendanceCollection.document(attendanceId).set(attendanceMap).await()

            val eventDoc = eventsCollection.document(eventId.value).get().await()
            val currentAttendees = eventDoc.getLong("currentAttendees")?.toInt() ?: 0
            eventsCollection.document(eventId.value)
                .update("currentAttendees", currentAttendees + 1)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveEvent(eventId: UUID, userId: UUID): Result<Unit> {
        return try {
            val attendanceId = "${eventId.value}_${userId.value}"
            attendanceCollection.document(attendanceId).delete().await()

            val eventDoc = eventsCollection.document(eventId.value).get().await()
            val currentAttendees = eventDoc.getLong("currentAttendees")?.toInt() ?: 0
            if (currentAttendees > 0) {
                eventsCollection.document(eventId.value)
                    .update("currentAttendees", currentAttendees - 1)
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEventAttendees(eventId: UUID): Result<List<UUID>> {
        return try {
            val attendees = attendanceCollection
                .whereEqualTo("eventId", eventId.value)
                .whereEqualTo("status", AttendanceStatus.GOING.name)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.getString("userId")?.let { UUID.parse(it).getOrNull() }
                }

            Result.success(attendees)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserJoinedEvents(userId: UUID): Result<List<Event>> {
        return try {
            val eventIds = attendanceCollection
                .whereEqualTo("userId", userId.value)
                .whereEqualTo("status", AttendanceStatus.GOING.name)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("eventId") }

            val events = eventIds.mapNotNull { eventId ->
                val snapshot = eventsCollection.document(eventId).get().await()
                FirebaseEventMapper.fromFirestore(snapshot)
            }

            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}
