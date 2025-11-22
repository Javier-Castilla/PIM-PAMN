package software.ulpgc.wherewhen.infrastructure.persistence

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import software.ulpgc.wherewhen.domain.model.Chat
import software.ulpgc.wherewhen.domain.ports.repositories.ChatRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FirebaseChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ChatRepository {

    private companion object {
        const val COLLECTION = "chats"
        const val FIELD_ID = "id"
        const val FIELD_PARTICIPANT1_ID = "participant1Id"
        const val FIELD_PARTICIPANT2_ID = "participant2Id"
        const val FIELD_LAST_MESSAGE = "lastMessage"
        const val FIELD_LAST_MESSAGE_AT = "lastMessageAt"
        const val FIELD_UNREAD_COUNT_1 = "unreadCount1"
        const val FIELD_UNREAD_COUNT_2 = "unreadCount2"
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    override suspend fun createOrGetChat(userId1: UUID, userId2: UUID): Result<Chat> = runCatching {
        val sortedIds = listOf(userId1.value, userId2.value).sorted()
        val chatDocId = "${sortedIds[0]}_${sortedIds[1]}"

        val document = firestore.collection(COLLECTION)
            .document(chatDocId)
            .get()
            .await()

        if (document.exists()) {
            document.toChat()
        } else {
            val chat = Chat(
                id = UUID.random(),
                participant1Id = userId1,
                participant2Id = userId2
            )
            firestore.collection(COLLECTION)
                .document(chatDocId)
                .set(chat.toMap())
                .await()
            chat
        }
    }

    override suspend fun getChat(chatId: UUID): Result<Chat> = runCatching {
        val querySnapshot = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_ID, chatId.value)
            .limit(1)
            .get()
            .await()

        querySnapshot.documents.firstOrNull()?.toChat()
            ?: throw IllegalStateException("Chat not found")
    }

    override fun observeUserChats(userId: UUID): Flow<List<Chat>> = callbackFlow {
        val allChats = mutableMapOf<String, Chat>()

        val listener1 = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_PARTICIPANT1_ID, userId.value)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.documents?.forEach { doc ->
                    runCatching {
                        allChats[doc.id] = doc.toChat()
                    }
                }
                trySend(allChats.values.toList())
            }

        val listener2 = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_PARTICIPANT2_ID, userId.value)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.documents?.forEach { doc ->
                    runCatching {
                        allChats[doc.id] = doc.toChat()
                    }
                }
                trySend(allChats.values.toList())
            }

        awaitClose {
            listener1.remove()
            listener2.remove()
        }
    }

    override suspend fun updateLastMessage(
        chatId: UUID,
        message: String,
        timestamp: LocalDateTime
    ): Result<Unit> = runCatching {
        val querySnapshot = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_ID, chatId.value)
            .limit(1)
            .get()
            .await()

        val docId = querySnapshot.documents.firstOrNull()?.id
            ?: throw IllegalStateException("Chat not found")

        firestore.collection(COLLECTION)
            .document(docId)
            .update(
                mapOf(
                    FIELD_LAST_MESSAGE to message,
                    FIELD_LAST_MESSAGE_AT to timestamp.format(DATE_FORMATTER)
                )
            )
            .await()
    }

    override suspend fun incrementUnreadCount(chatId: UUID, userId: UUID): Result<Unit> = runCatching {
        val chat = getChat(chatId).getOrThrow()
        val field = if (chat.participant1Id == userId) FIELD_UNREAD_COUNT_1 else FIELD_UNREAD_COUNT_2

        val querySnapshot = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_ID, chatId.value)
            .limit(1)
            .get()
            .await()

        val docId = querySnapshot.documents.firstOrNull()?.id
            ?: throw IllegalStateException("Chat not found")

        firestore.collection(COLLECTION)
            .document(docId)
            .update(field, com.google.firebase.firestore.FieldValue.increment(1))
            .await()
    }

    override suspend fun resetUnreadCount(chatId: UUID, userId: UUID): Result<Unit> = runCatching {
        val chat = getChat(chatId).getOrThrow()
        val field = if (chat.participant1Id == userId) FIELD_UNREAD_COUNT_1 else FIELD_UNREAD_COUNT_2
        val querySnapshot = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_ID, chatId.value)
            .limit(1)
            .get()
            .await()
        val docId = querySnapshot.documents.firstOrNull()?.id
            ?: throw IllegalStateException("Chat not found")
        firestore.collection(COLLECTION)
            .document(docId)
            .update(field, 0)
            .await()
    }

    private fun Chat.toMap() = mapOf(
        FIELD_ID to id.value,
        FIELD_PARTICIPANT1_ID to participant1Id.value,
        FIELD_PARTICIPANT2_ID to participant2Id.value,
        FIELD_LAST_MESSAGE to lastMessage,
        FIELD_LAST_MESSAGE_AT to lastMessageAt?.format(DATE_FORMATTER),
        FIELD_UNREAD_COUNT_1 to unreadCount1,
        FIELD_UNREAD_COUNT_2 to unreadCount2
    )

    private fun DocumentSnapshot.toChat(): Chat {
        val id = getString(FIELD_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing id field")

        val participant1Id = getString(FIELD_PARTICIPANT1_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing participant1Id field")

        val participant2Id = getString(FIELD_PARTICIPANT2_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing participant2Id field")

        val lastMessage = getString(FIELD_LAST_MESSAGE)
        val lastMessageAt = getString(FIELD_LAST_MESSAGE_AT)
            ?.let { LocalDateTime.parse(it, DATE_FORMATTER) }

        val unreadCount1 = getLong(FIELD_UNREAD_COUNT_1)?.toInt() ?: 0
        val unreadCount2 = getLong(FIELD_UNREAD_COUNT_2)?.toInt() ?: 0

        return Chat(
            id = id,
            participant1Id = participant1Id,
            participant2Id = participant2Id,
            lastMessage = lastMessage,
            lastMessageAt = lastMessageAt,
            unreadCount1 = unreadCount1,
            unreadCount2 = unreadCount2
        )
    }
}
