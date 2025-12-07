package software.ulpgc.wherewhen.domain.usecases.chats

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.chat.Chat
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.usecases.chat.CreateOrGetChatUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class CreateOrGetChatUseCaseTest {

    private lateinit var chatRepository: ChatRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: CreateOrGetChatUseCase

    @Before
    fun setup() {
        chatRepository = mockk()
        userRepository = mockk()
        useCase = CreateOrGetChatUseCase(chatRepository, userRepository)
    }

    @Test
    fun `fails when first user does not exist`() = runTest {
        val userId = UUID.random()
        val otherUserId = UUID.random()

        coEvery { userRepository.existsWith(userId) } returns false

        val result = useCase.invoke(userId, otherUserId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UserNotFoundException)
        coVerify { userRepository.existsWith(userId) }
        coVerify(exactly = 0) { userRepository.existsWith(otherUserId) }
        coVerify(exactly = 0) { chatRepository.createOrGetChat(any(), any()) }
    }

    @Test
    fun `fails when second user does not exist`() = runTest {
        val userId = UUID.random()
        val otherUserId = UUID.random()

        coEvery { userRepository.existsWith(userId) } returns true
        coEvery { userRepository.existsWith(otherUserId) } returns false

        val result = useCase.invoke(userId, otherUserId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UserNotFoundException)
        coVerify { userRepository.existsWith(userId) }
        coVerify { userRepository.existsWith(otherUserId) }
        coVerify(exactly = 0) { chatRepository.createOrGetChat(any(), any()) }
    }

    @Test
    fun `creates or gets chat successfully`() = runTest {
        val userId = UUID.random()
        val otherUserId = UUID.random()
        val chat = Chat(
            id = UUID.random(),
            participant1Id = userId,
            participant2Id = otherUserId
        )

        coEvery { userRepository.existsWith(userId) } returns true
        coEvery { userRepository.existsWith(otherUserId) } returns true
        coEvery { chatRepository.createOrGetChat(userId, otherUserId) } returns Result.success(chat)

        val result = useCase.invoke(userId, otherUserId)

        assertTrue(result.isSuccess)
        assertEquals(chat, result.getOrNull())
        coVerify { userRepository.existsWith(userId) }
        coVerify { userRepository.existsWith(otherUserId) }
        coVerify { chatRepository.createOrGetChat(userId, otherUserId) }
    }
}
