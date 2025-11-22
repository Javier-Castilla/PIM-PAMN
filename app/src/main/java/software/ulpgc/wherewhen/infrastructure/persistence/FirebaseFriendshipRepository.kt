package software.ulpgc.wherewhen.infrastructure.persistence

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import software.ulpgc.wherewhen.domain.model.Friendship
import software.ulpgc.wherewhen.domain.ports.repositories.FriendshipRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FirebaseFriendshipRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FriendshipRepository {

    private companion object {
        const val COLLECTION = "friendships"
        const val FIELD_ID = "id"
        const val FIELD_USER1_ID = "user1Id"
        const val FIELD_USER2_ID = "user2Id"
        const val FIELD_CREATED_AT = "createdAt"
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    override suspend fun create(friendship: Friendship): Result<Friendship> = runCatching {
        firestore.collection(COLLECTION)
            .document(friendship.id.value)
            .set(friendship.toMap())
            .await()
        friendship
    }

    override suspend fun getById(id: UUID): Result<Friendship> = runCatching {
        val document = firestore.collection(COLLECTION)
            .document(id.value)
            .get()
            .await()

        document.takeIf { it.exists() }?.toFriendship()
            ?: throw IllegalStateException("Friendship not found")
    }

    override suspend fun getFriendshipsForUser(userId: UUID): Result<List<Friendship>> = runCatching {
        val asUser1 = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_USER1_ID, userId.value)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFriendship() }

        val asUser2 = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_USER2_ID, userId.value)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFriendship() }

        (asUser1 + asUser2).distinctBy { it.id }
    }

    override suspend fun existsBetweenUsers(user1Id: UUID, user2Id: UUID): Result<Boolean> = runCatching {
        val query1 = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_USER1_ID, user1Id.value)
            .whereEqualTo(FIELD_USER2_ID, user2Id.value)
            .limit(1)
            .get()
            .await()

        val query2 = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_USER1_ID, user2Id.value)
            .whereEqualTo(FIELD_USER2_ID, user1Id.value)
            .limit(1)
            .get()
            .await()

        !query1.isEmpty || !query2.isEmpty
    }

    override suspend fun deleteBetweenUsers(user1Id: UUID, user2Id: UUID): Result<Unit> = runCatching {
        val query1 = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_USER1_ID, user1Id.value)
            .whereEqualTo(FIELD_USER2_ID, user2Id.value)
            .get()
            .await()

        val query2 = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_USER1_ID, user2Id.value)
            .whereEqualTo(FIELD_USER2_ID, user1Id.value)
            .get()
            .await()

        (query1.documents + query2.documents).forEach {
            firestore.collection(COLLECTION).document(it.id).delete().await()
        }
    }

    private fun Friendship.toMap() = mapOf(
        FIELD_ID to id.value,
        FIELD_USER1_ID to user1Id.value,
        FIELD_USER2_ID to user2Id.value,
        FIELD_CREATED_AT to createdAt.format(DATE_FORMATTER)
    )

    private fun DocumentSnapshot.toFriendship(): Friendship {
        val id = getString(FIELD_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing id field")

        val user1Id = getString(FIELD_USER1_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing user1Id field")

        val user2Id = getString(FIELD_USER2_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing user2Id field")

        val createdAt = getString(FIELD_CREATED_AT)
            ?.let { LocalDateTime.parse(it, DATE_FORMATTER) }
            ?: throw IllegalStateException("Missing createdAt field")

        return Friendship(id, user1Id, user2Id, createdAt)
    }
}
