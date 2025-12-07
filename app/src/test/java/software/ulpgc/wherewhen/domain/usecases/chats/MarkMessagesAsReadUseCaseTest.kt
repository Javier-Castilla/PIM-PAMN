package software.ulpgc.wherewhen.domain.usecases.chats

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.chat.ChatNotFoundException
import software.ulpgc.wherewhen.domain.model.chat.Chat
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.ports.persistence.MessageRepository
import software.ulpgc.wherewhen.domain.usecases.chat.MarkMessagesAsReadUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class MarkMessagesAsReadUseCaseTest {

    private lateinit var chatRepository: ChatRepository
    private lateinit var messageRepository: MessageRepository
    private lateinit var useCase: MarkMessagesAsReadUseCase

    @Before
    fun setup() {
        chatRepository = mockk()
        messageRepository = mockk()
        useCase = MarkMessagesAsReadUseCase(chatRepository, messageRepository)
    }

    @Test
    fun `fails when chat not found`() = runTest {
        val chatId = UUID.Companion.random()
        val userId = UUID.Companion.random()

        coEvery { chatRepository.getChat(chatId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(chatId, userId)

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is ChatNotFoundException)
        coVerify { chatRepository.getChat(chatId) }
        coVerify(exactly = 0) { messageRepository.markAllAsRead(any(), any()) }
        coVerify(exactly = 0) { chatRepository.resetUnreadCount(any(), any()) }
    }

    @Test
    fun `fails when markAllAsRead fails`() = runTest {
        val chatId = UUID.Companion.random()
        val userId = UUID.Companion.random()
        val chat = Chat(
            id = chatId,
            participant1Id = userId,
            participant2Id = UUID.Companion.random()
        )
        val error = RuntimeException("db error")

        coEvery { chatRepository.getChat(chatId) } returns Result.success(chat)
        coEvery { messageRepository.markAllAsRead(chatId, userId) } returns Result.failure(error)

        val result = useCase.invoke(chatId, userId)

        Assert.assertTrue(result.isFailure)
        Assert.assertEquals(error, result.exceptionOrNull())
        coVerify { chatRepository.getChat(chatId) }
        coVerify { messageRepository.markAllAsRead(chatId, userId) }
        coVerify(exactly = 0) { chatRepository.resetUnreadCount(any(), any()) }
    }

    @Test
    fun `marks messages as read and resets unread count`() = runTest {
        val chatId = UUID.Companion.random()
        val userId = UUID.Companion.random()
        val chat = Chat(
            id = chatId,
            participant1Id = userId,
            participant2Id = UUID.Companion.random()
        )

        coEvery { chatRepository.getChat(chatId) } returns Result.success(chat)
        coEvery { messageRepository.markAllAsRead(chatId, userId) } returns Result.success(Unit)
        coEvery { chatRepository.resetUnreadCount(chatId, userId) } returns Result.success(Unit)

        val result = useCase.invoke(chatId, userId)

        Assert.assertTrue(result.isSuccess)
        coVerify { chatRepository.getChat(chatId) }
        coVerify { messageRepository.markAllAsRead(chatId, userId) }
        coVerify { chatRepository.resetUnreadCount(chatId, userId) }
    }
}