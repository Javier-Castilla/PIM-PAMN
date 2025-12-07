package software.ulpgc.wherewhen.domain.usecases.chats

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.model.chat.Message
import software.ulpgc.wherewhen.domain.ports.persistence.MessageRepository
import software.ulpgc.wherewhen.domain.usecases.chat.GetChatMessagesUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class GetChatMessagesUseCaseTest {

    private lateinit var messageRepository: MessageRepository
    private lateinit var useCase: GetChatMessagesUseCase

    @Before
    fun setup() {
        messageRepository = mockk()
        useCase = GetChatMessagesUseCase(messageRepository)
    }

    @Test
    fun `observes chat messages from repository`() = runTest {
        val chatId = UUID.Companion.random()
        val message1 = Message(
            id = UUID.Companion.random(),
            chatId = chatId,
            senderId = UUID.Companion.random(),
            content = "hi",
            timestamp = LocalDateTime.now()
        )
        val message2 = Message(
            id = UUID.Companion.random(),
            chatId = chatId,
            senderId = UUID.Companion.random(),
            content = "hello",
            timestamp = LocalDateTime.now()
        )

        every { messageRepository.observeMessages(chatId) } returns flowOf(
            listOf(
                message1,
                message2
            )
        )

        val resultList = useCase.invoke(chatId).first()

        Assert.assertEquals(listOf(message1, message2), resultList)
    }
}