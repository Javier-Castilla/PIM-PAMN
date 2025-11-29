package software.ulpgc.wherewhen.infrastructure.persistence

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import software.ulpgc.wherewhen.domain.model.chat.Message
import software.ulpgc.wherewhen.domain.ports.persistence.MessageRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.exceptions.chat.MessageNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FirebaseMessageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MessageRepository {

    private companion object {
        const val COLLECTION_CHATS = "chats"
        const val COLLECTION_MESSAGES = "messages"
        const val FIELD_ID = "id"
        const val FIELD_CHAT_ID = "chatId"
        const val FIELD_SENDER_ID = "senderId"
        const val FIELD_CONTENT = "content"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_IS_READ = "isRead"
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    override suspend fun sendMessage(message: Message): Result<Message> = runCatching {
        firestore.collection(COLLECTION_CHATS)
            .document(message.chatId.value)
            .collection(COLLECTION_MESSAGES)
            .document(message.id.value)
            .set(message.toMap())
            .await()
        message
    }

    override fun observeMessages(chatId: UUID): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_CHATS)
            .document(chatId.value)
            .collection(COLLECTION_MESSAGES)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull {
                    runCatching { it.toMessage() }.getOrNull()
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun markAsRead(messageId: UUID): Result<Unit> = runCatching {
        val messageDoc = firestore.collectionGroup(COLLECTION_MESSAGES)
            .whereEqualTo(FIELD_ID, messageId.value)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?: throw MessageNotFoundException(messageId)

        messageDoc.reference
            .update(FIELD_IS_READ, true)
            .await()
    }

    override suspend fun markAllAsRead(chatId: UUID, userId: UUID): Result<Unit> = runCatching {
        val messages = firestore.collection(COLLECTION_CHATS)
            .document(chatId.value)
            .collection(COLLECTION_MESSAGES)
            .whereEqualTo(FIELD_IS_READ, false)
            .get()
            .await()

        val batch = firestore.batch()
        messages.documents.forEach { doc ->
            val senderId = doc.getString(FIELD_SENDER_ID)
            if (senderId != userId.value) {
                batch.update(doc.reference, FIELD_IS_READ, true)
            }
        }
        batch.commit().await()
    }

    private fun Message.toMap() = mapOf(
        FIELD_ID to id.value,
        FIELD_CHAT_ID to chatId.value,
        FIELD_SENDER_ID to senderId.value,
        FIELD_CONTENT to content,
        FIELD_TIMESTAMP to timestamp.format(DATE_FORMATTER),
        FIELD_IS_READ to isRead
    )

    private fun DocumentSnapshot.toMessage(): Message {
        val id = getString(FIELD_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing id field")

        val chatId = getString(FIELD_CHAT_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing chatId field")

        val senderId = getString(FIELD_SENDER_ID)
            ?.let { UUID.parse(it).getOrThrow() }
            ?: throw IllegalStateException("Missing senderId field")

        val content = getString(FIELD_CONTENT)
            ?: throw IllegalStateException("Missing content field")

        val timestamp = getString(FIELD_TIMESTAMP)
            ?.let { LocalDateTime.parse(it, DATE_FORMATTER) }
            ?: throw IllegalStateException("Missing timestamp field")

        val isRead = getBoolean(FIELD_IS_READ) ?: false

        return Message(id, chatId, senderId, content, timestamp, isRead)
    }
}
