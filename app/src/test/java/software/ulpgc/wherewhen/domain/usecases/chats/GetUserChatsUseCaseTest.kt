package software.ulpgc.wherewhen.domain.usecases.chats

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.model.chat.Chat
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.usecases.chat.GetUserChatsUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class GetUserChatsUseCaseTest {

    private lateinit var chatRepository: ChatRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: GetUserChatsUseCase

    @Before
    fun setup() {
        chatRepository = mockk()
        userRepository = mockk()
        useCase = GetUserChatsUseCase(chatRepository, userRepository)
    }

    @Test
    fun `maps chats to ChatWithUser including unread count`() = runTest {
        val userId = UUID.Companion.random()
        val otherUserId = UUID.Companion.random()
        val now = LocalDateTime.now()
        val chat = Chat(
            id = UUID.Companion.random(),
            participant1Id = userId,
            participant2Id = otherUserId,
            lastMessage = "hi",
            lastMessageAt = now,
            unreadCount1 = 3,
            unreadCount2 = 0
        )
        val otherUser = User(
            uuid = otherUserId,
            name = "Other"
        )

        every { chatRepository.observeUserChats(userId) } returns flowOf(listOf(chat))
        coEvery { userRepository.getPublicUser(otherUserId) } returns Result.success(otherUser)

        val resultList = useCase.invoke(userId).first()

        Assert.assertEquals(1, resultList.size)
        val chatWithUser = resultList.first()
        Assert.assertEquals(chat, chatWithUser.chat)
        Assert.assertEquals(otherUser, chatWithUser.otherUser)
        Assert.assertEquals("hi", chatWithUser.lastMessage)
        Assert.assertEquals(now, chatWithUser.lastMessageAt)
        Assert.assertEquals(3, chatWithUser.unreadCount)
    }

    @Test
    fun `filters out chats when other user cannot be resolved`() = runTest {
        val userId = UUID.Companion.random()
        val otherUserId = UUID.Companion.random()
        val chat = Chat(
            id = UUID.Companion.random(),
            participant1Id = userId,
            participant2Id = otherUserId
        )

        every { chatRepository.observeUserChats(userId) } returns flowOf(listOf(chat))
        coEvery { userRepository.getPublicUser(otherUserId) } returns Result.failure(
            RuntimeException("not found")
        )

        val resultList = useCase.invoke(userId).first()

        Assert.assertTrue(resultList.isEmpty())
    }
}