package software.ulpgc.wherewhen.infrastructure.persistence

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
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
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_CREATED_AT = "createdAt"
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    override suspend fun register(profile: Profile): Result<Profile> = runCatching {
        firestore.collection(COLLECTION)
            .document(profile.uuid.value)
            .set(profile.toMap())
            .await()
        profile
    }

    override suspend fun getWith(uuid: UUID): Result<Profile> = runCatching {
        val document = firestore.collection(COLLECTION)
            .document(uuid.value)
            .get()
            .await()

        document.takeIf { it.exists() }?.toProfile()
            ?: throw UserNotFoundException(uuid)
    }

    override suspend fun getWith(email: Email): Result<Profile> = runCatching {
        val querySnapshot = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_EMAIL, email.value)
            .limit(1)
            .get()
            .await()

        querySnapshot.documents.firstOrNull()?.toProfile()
            ?: throw UserNotFoundException(email)
    }

    override suspend fun getPublicUser(uuid: UUID): Result<User> = runCatching {
        val document = firestore.collection(COLLECTION)
            .document(uuid.value)
            .get()
            .await()

        document.takeIf { it.exists() }?.toPublicUser()
            ?: throw UserNotFoundException(uuid)
    }

    override suspend fun update(profile: Profile): Result<Profile> = runCatching {
        val exists = firestore.collection(COLLECTION)
            .document(profile.uuid.value)
            .get()
            .await()
            .exists()

        if (!exists) throw UserNotFoundException(profile.uuid)

        firestore.collection(COLLECTION)
            .document(profile.uuid.value)
            .update(profile.toUpdateMap())
            .await()
        profile
    }

    override suspend fun delete(uuid: UUID): Result<Profile> = runCatching {
        val document = firestore.collection(COLLECTION)
            .document(uuid.value)
            .get()
            .await()

        val profile = document.takeIf { it.exists() }?.toProfile()
            ?: throw UserNotFoundException(uuid)

        firestore.collection(COLLECTION)
            .document(uuid.value)
            .delete()
            .await()

        profile
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

    override suspend fun searchByName(query: String): Result<List<User>> = runCatching {
        val normalizedQuery = query.trim().lowercase()

        firestore.collection(COLLECTION)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                runCatching { document.toPublicUser() }.getOrNull()
            }
            .filter { user ->
                user.name.lowercase().contains(normalizedQuery)
            }
    }

    private fun Profile.toMap() = mapOf(
        FIELD_UUID to uuid.value,
        FIELD_EMAIL to email.value,
        FIELD_NAME to name,
        FIELD_DESCRIPTION to description,
        FIELD_CREATED_AT to createdAt.format(DATE_FORMATTER)
    )

    private fun Profile.toUpdateMap() = mapOf(
        FIELD_EMAIL to email.value,
        FIELD_NAME to name,
        FIELD_DESCRIPTION to description,
        FIELD_CREATED_AT to createdAt.format(DATE_FORMATTER)
    )

    private fun DocumentSnapshot.toProfile(): Profile {
        val uuid = getString(FIELD_UUID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing UUID field")

        val email = getString(FIELD_EMAIL)
            ?.let { Email.create(it).getOrThrow() }
            ?: throw IllegalStateException("Missing email field")

        val name = getString(FIELD_NAME)
            ?: throw IllegalStateException("Missing name field")

        val description = getString(FIELD_DESCRIPTION) ?: ""

        val createdAt = getString(FIELD_CREATED_AT)
            ?.let { LocalDateTime.parse(it, DATE_FORMATTER) }
            ?: throw IllegalStateException("Missing createdAt field")

        return Profile(uuid, email, name, description, createdAt)
    }

    private fun DocumentSnapshot.toPublicUser(): User {
        val uuid = getString(FIELD_UUID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing UUID field")

        val name = getString(FIELD_NAME)
            ?: throw IllegalStateException("Missing name field")

        val description = getString(FIELD_DESCRIPTION) ?: ""

        val createdAt = getString(FIELD_CREATED_AT)
            ?.let { LocalDateTime.parse(it, DATE_FORMATTER) }
            ?: throw IllegalStateException("Missing createdAt field")

        return User(uuid, name, description, createdAt)
    }
}
