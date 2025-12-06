package software.ulpgc.wherewhen.infrastructure.persistence

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.exceptions.friendship.FriendRequestNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FirebaseFriendRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FriendRequestRepository {

    private companion object {
        const val COLLECTION = "friendRequests"
        const val FIELD_ID = "id"
        const val FIELD_SENDER_ID = "senderId"
        const val FIELD_RECEIVER_ID = "receiverId"
        const val FIELD_STATUS = "status"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_RESPONDED_AT = "respondedAt"
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    override suspend fun create(request: FriendRequest): Result<FriendRequest> = runCatching {
        firestore.collection(COLLECTION)
            .document(request.id.value)
            .set(request.toMap())
            .await()
        request
    }

    override suspend fun getById(id: UUID): Result<FriendRequest> = runCatching {
        val document = firestore.collection(COLLECTION)
            .document(id.value)
            .get()
            .await()

        document.takeIf { it.exists() }?.toFriendRequest()
            ?: throw FriendRequestNotFoundException(id)
    }

    override suspend fun getPendingRequestsForUser(userId: UUID): Result<List<FriendRequest>> = runCatching {
        firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_RECEIVER_ID, userId.value)
            .whereEqualTo(FIELD_STATUS, FriendRequestStatus.PENDING.name)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFriendRequest() }
    }

    override suspend fun getSentRequestsFromUser(userId: UUID): Result<List<FriendRequest>> = runCatching {
        firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_SENDER_ID, userId.value)
            .whereEqualTo(FIELD_STATUS, FriendRequestStatus.PENDING.name)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFriendRequest() }
    }

    override suspend fun getPendingBetweenUsers(senderId: UUID, receiverId: UUID): Result<FriendRequest?> = runCatching {
        firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_SENDER_ID, senderId.value)
            .whereEqualTo(FIELD_RECEIVER_ID, receiverId.value)
            .whereEqualTo(FIELD_STATUS, FriendRequestStatus.PENDING.name)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toFriendRequest()
    }

    override suspend fun updateStatus(id: UUID, status: FriendRequestStatus, respondedAt: LocalDateTime): Result<FriendRequest> = runCatching {
        firestore.collection(COLLECTION)
            .document(id.value)
            .update(
                mapOf(
                    FIELD_STATUS to status.name,
                    FIELD_RESPONDED_AT to respondedAt.format(DATE_FORMATTER)
                )
            )
            .await()
        getById(id).getOrThrow()
    }

    override suspend fun delete(id: UUID): Result<Unit> = runCatching {
        firestore.collection(COLLECTION)
            .document(id.value)
            .delete()
            .await()
    }

    private fun FriendRequest.toMap() = mapOf(
        FIELD_ID to id.value,
        FIELD_SENDER_ID to senderId.value,
        FIELD_RECEIVER_ID to receiverId.value,
        FIELD_STATUS to status.name,
        FIELD_CREATED_AT to createdAt.format(DATE_FORMATTER),
        FIELD_RESPONDED_AT to respondedAt?.format(DATE_FORMATTER)
    )

    private fun DocumentSnapshot.toFriendRequest(): FriendRequest {
        val id = getString(FIELD_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing id field")

        val senderId = getString(FIELD_SENDER_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing senderId field")

        val receiverId = getString(FIELD_RECEIVER_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing receiverId field")

        val status = getString(FIELD_STATUS)
            ?.let { FriendRequestStatus.valueOf(it) }
            ?: throw IllegalStateException("Missing status field")

        val createdAt = getString(FIELD_CREATED_AT)
            ?.let { LocalDateTime.parse(it, DATE_FORMATTER) }
            ?: throw IllegalStateException("Missing createdAt field")

        val respondedAt = getString(FIELD_RESPONDED_AT)
            ?.let { LocalDateTime.parse(it, DATE_FORMATTER) }

        return FriendRequest(id, senderId, receiverId, status, createdAt, respondedAt)
    }
}
