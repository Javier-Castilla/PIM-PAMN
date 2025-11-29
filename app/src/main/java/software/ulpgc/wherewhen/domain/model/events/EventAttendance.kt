package software.ulpgc.wherewhen.domain.model.events

import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

data class EventAttendance(
    val id: UUID,
    val eventId: UUID,
    val userId: UUID,
    val status: AttendanceStatus,
    val joinedAt: LocalDateTime
)
