package software.ulpgc.wherewhen.domain.usecases.chats

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.chat.ChatNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.chat.EmptyMessageException
import software.ulpgc.wherewhen.domain.model.chat.Chat
import software.ulpgc.wherewhen.domain.model.chat.Message
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.ports.persistence.MessageRepository
import software.ulpgc.wherewhen.domain.usecases.chat.SendMessageUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class SendMessageUseCaseTest {

    private lateinit var chatRepository: ChatRepository
    private lateinit var messageRepository: MessageRepository
    private lateinit var useCase: SendMessageUseCase

    @Before
    fun setup() {
        chatRepository = mockk()
        messageRepository = mockk()
        useCase = SendMessageUseCase(chatRepository, messageRepository)
    }

    @Test
    fun `fails when content is blank`() = runTest {
        val chatId = UUID.Companion.random()
        val senderId = UUID.Companion.random()

        val result = useCase.invoke(chatId, senderId, "   ")

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is EmptyMessageException)
        coVerify(exactly = 0) { chatRepository.getChat(any()) }
        coVerify(exactly = 0) { messageRepository.sendMessage(any()) }
    }

    @Test
    fun `fails when chat not found`() = runTest {
        val chatId = UUID.Companion.random()
        val senderId = UUID.Companion.random()

        coEvery { chatRepository.getChat(chatId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(chatId, senderId, "hello")

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is ChatNotFoundException)
        coVerify { chatRepository.getChat(chatId) }
        coVerify(exactly = 0) { messageRepository.sendMessage(any()) }
    }

    @Test
    fun `fails when receiver cannot be resolved`() = runTest {
        val chatId = UUID.Companion.random()
        val senderId = UUID.Companion.random()
        val chat = Chat(
            id = chatId,
            participant1Id = UUID.Companion.random(),
            participant2Id = UUID.Companion.random()
        )

        coEvery { chatRepository.getChat(chatId) } returns Result.success(chat)

        val result = useCase.invoke(chatId, senderId, "hello")

        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is ChatNotFoundException)
        coVerify { chatRepository.getChat(chatId) }
        coVerify(exactly = 0) { messageRepository.sendMessage(any()) }
    }

    @Test
    fun `fails when sendMessage returns failure`() = runTest {
        val chatId = UUID.Companion.random()
        val senderId = UUID.Companion.random()
        val receiverId = UUID.Companion.random()
        val chat = Chat(
            id = chatId,
            participant1Id = senderId,
            participant2Id = receiverId
        )
        val error = RuntimeException("send error")

        coEvery { chatRepository.getChat(chatId) } returns Result.success(chat)
        coEvery { messageRepository.sendMessage(any()) } returns Result.failure(error)

        val result = useCase.invoke(chatId, senderId, "hello")

        Assert.assertTrue(result.isFailure)
        Assert.assertEquals(error, result.exceptionOrNull())
        coVerify { chatRepository.getChat(chatId) }
        coVerify { messageRepository.sendMessage(any()) }
        coVerify(exactly = 0) { chatRepository.updateLastMessage(any(), any(), any()) }
        coVerify(exactly = 0) { chatRepository.incrementUnreadCount(any(), any()) }
    }

    @Test
    fun `sends message and updates chat metadata`() = runTest {
        val chatId = UUID.Companion.random()
        val senderId = UUID.Companion.random()
        val receiverId = UUID.Companion.random()
        val chat = Chat(
            id = chatId,
            participant1Id = senderId,
            participant2Id = receiverId
        )
        val content = "hello"

        coEvery { chatRepository.getChat(chatId) } returns Result.success(chat)
        coEvery { messageRepository.sendMessage(any()) } answers {
            val msg = arg<Message>(0)
            Result.success(msg)
        }
        coEvery { chatRepository.updateLastMessage(chatId, content, any()) } returns Result.success(
            Unit
        )
        coEvery { chatRepository.incrementUnreadCount(chatId, receiverId) } returns Result.success(
            Unit
        )

        val result = useCase.invoke(chatId, senderId, content)

        Assert.assertTrue(result.isSuccess)
        val message = result.getOrNull()
        Assert.assertNotNull(message)
        Assert.assertEquals(chatId, message?.chatId)
        Assert.assertEquals(senderId, message?.senderId)
        Assert.assertEquals(content, message?.content)

        coVerify { chatRepository.getChat(chatId) }
        coVerify { messageRepository.sendMessage(any()) }
        coVerify { chatRepository.updateLastMessage(chatId, content, any()) }
        coVerify { chatRepository.incrementUnreadCount(chatId, receiverId) }
    }
}