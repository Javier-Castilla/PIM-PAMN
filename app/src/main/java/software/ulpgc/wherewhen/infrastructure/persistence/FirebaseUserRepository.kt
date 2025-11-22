package software.ulpgc.wherewhen.infrastructure.persistence

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.persistence.repositories.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FirebaseUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private companion object {
        const val COLLECTION = "users"
        const val FIELD_UUID = "uuid"
        const val FIELD_EMAIL = "email"
        const val FIELD_NAME = "name"
        const val FIELD_CREATED_AT = "createdAt"
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    override suspend fun register(user: User): Result<User> = runCatching {
        firestore.collection(COLLECTION)
            .document(user.uuid.value)
            .set(user.toMap())
            .await()
        user
    }

    override suspend fun getWith(uuid: UUID): Result<User> = runCatching {
        val document = firestore.collection(COLLECTION)
            .document(uuid.value)
            .get()
            .await()

        document.takeIf { it.exists() }?.toUser()
            ?: throw UserNotFoundException(uuid)
    }

    override suspend fun getWith(email: Email): Result<User> = runCatching {
        val querySnapshot = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_EMAIL, email.value)
            .limit(1)
            .get()
            .await()

        querySnapshot.documents.firstOrNull()?.toUser()
            ?: throw UserNotFoundException(email)
    }

    override suspend fun update(user: User): Result<User> = runCatching {
        val exists = firestore.collection(COLLECTION)
            .document(user.uuid.value)
            .get()
            .await()
            .exists()

        if (!exists) throw UserNotFoundException(user.uuid)

        firestore.collection(COLLECTION)
            .document(user.uuid.value)
            .update(user.toUpdateMap())
            .await()
        user
    }

    override suspend fun delete(uuid: UUID): Result<User> = runCatching {
        val document = firestore.collection(COLLECTION)
            .document(uuid.value)
            .get()
            .await()

        val user = document.takeIf { it.exists() }?.toUser()
            ?: throw UserNotFoundException(uuid)

        firestore.collection(COLLECTION)
            .document(uuid.value)
            .delete()
            .await()

        user
    }

    override suspend fun existsWith(email: Email): Boolean = runCatching {
        firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_EMAIL, email.value)
            .limit(1)
            .get()
            .await()
            .isEmpty.not()
    }.getOrDefault(false)

    override suspend fun existsWith(uuid: UUID): Boolean = runCatching {
        firestore.collection(COLLECTION)
            .document(uuid.value)
            .get()
            .await()
            .exists()
    }.getOrDefault(false)

    private fun User.toMap() = mapOf(
        FIELD_UUID to uuid.value,
        FIELD_EMAIL to email.value,
        FIELD_NAME to name,
        FIELD_CREATED_AT to createdAt.format(DATE_FORMATTER)
    )

    private fun User.toUpdateMap() = mapOf(
        FIELD_EMAIL to email.value,
        FIELD_NAME to name,
        FIELD_CREATED_AT to createdAt.format(DATE_FORMATTER)
    )

    private fun DocumentSnapshot.toUser(): User {
        val uuid = getString(FIELD_UUID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing UUID field")

        val email = getString(FIELD_EMAIL)
            ?.let { Email.create(it).getOrThrow() }
            ?: throw IllegalStateException("Missing email field")

        val name = getString(FIELD_NAME)
            ?: throw IllegalStateException("Missing name field")

        val createdAt = getString(FIELD_CREATED_AT)
            ?.let { LocalDateTime.parse(it, DATE_FORMATTER) }
            ?: throw IllegalStateException("Missing createdAt field")

        return User(uuid, email, name, createdAt)
    }
}
