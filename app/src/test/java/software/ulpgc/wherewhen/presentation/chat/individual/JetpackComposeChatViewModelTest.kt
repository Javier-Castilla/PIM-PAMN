package software.ulpgc.wherewhen.presentation.chat.individual

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.chat.ChatNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.chat.MessageNotFoundException
import software.ulpgc.wherewhen.domain.model.chat.Chat
import software.ulpgc.wherewhen.domain.model.chat.Message
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.usecases.chat.CreateOrGetChatUseCase
import software.ulpgc.wherewhen.domain.usecases.chat.GetChatMessagesUseCase
import software.ulpgc.wherewhen.domain.usecases.chat.MarkMessagesAsReadUseCase
import software.ulpgc.wherewhen.domain.usecases.chat.SendMessageUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackComposeChatViewModelTest {

    private lateinit var createOrGetChatUseCase: CreateOrGetChatUseCase
    private lateinit var getChatMessagesUseCase: GetChatMessagesUseCase
    private lateinit var sendMessageUseCase: SendMessageUseCase
    private lateinit var markMessagesAsReadUseCase: MarkMessagesAsReadUseCase

    private lateinit var viewModel: JetpackComposeChatViewModel

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private val dispatcher = UnconfinedTestDispatcher()

    private val currentUserId: UUID = UUID.random()
    private val currentUserIdString: String = currentUserId.toString()
    private val otherUserId: UUID = UUID.random()
    private val chatId: UUID = UUID.random()

    private lateinit var otherUser: User

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        mockkStatic(FirebaseAuth::class)
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns currentUserIdString

        createOrGetChatUseCase = mockk()
        getChatMessagesUseCase = mockk()
        sendMessageUseCase = mockk()
        markMessagesAsReadUseCase = mockk()

        otherUser = mockk()
        every { otherUser.uuid } returns otherUserId

        viewModel = JetpackComposeChatViewModel(
            createOrGetChatUseCase,
            getChatMessagesUseCase,
            sendMessageUseCase,
            markMessagesAsReadUseCase
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState

        assertNull(state.otherUser)
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.messageText)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(viewModel.getChatId())
    }

    @Test
    fun `initChat success sets chatId, otherUser and observes messages`() {
        val chat = Chat(
            id = chatId,
            participant1Id = currentUserId,
            participant2Id = otherUserId
        )
        val message: Message = mockk()

        coEvery { createOrGetChatUseCase.invoke(currentUserId, otherUserId) } returns Result.success(chat)
        every { getChatMessagesUseCase.invoke(chatId) } returns flowOf(listOf(message))

        viewModel.initChat(otherUser)

        val state = viewModel.uiState
        assertEquals(chatId, viewModel.getChatId())
        assertSame(otherUser, state.otherUser)
        assertEquals(1, state.messages.size)
        assertSame(message, state.messages.first())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `initChat failure with ChatNotFoundException shows specific error`() {
        coEvery {
            createOrGetChatUseCase.invoke(currentUserId, otherUserId)
        } returns Result.failure(ChatNotFoundException(chatId))

        viewModel.initChat(otherUser)

        val state = viewModel.uiState
        assertNull(viewModel.getChatId())
        assertEquals("Chat not found", state.errorMessage)
        assertFalse(state.isLoading)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun `onMessageTextChange updates messageText`() {
        viewModel.onMessageTextChange("Hola")

        val state = viewModel.uiState
        assertEquals("Hola", state.messageText)
    }

    @Test
    fun `sendMessage with blank content does nothing`() {
        val chat = Chat(
            id = chatId,
            participant1Id = currentUserId,
            participant2Id = otherUserId
        )
        coEvery { createOrGetChatUseCase.invoke(currentUserId, otherUserId) } returns Result.success(chat)
        every { getChatMessagesUseCase.invoke(chatId) } returns flowOf(emptyList())

        viewModel.initChat(otherUser)
        viewModel.onMessageTextChange("   ")

        viewModel.sendMessage()

        coVerify(exactly = 0) {
            sendMessageUseCase.invoke(chatId, currentUserId, any<String>())
        }
        assertEquals("   ", viewModel.uiState.messageText)
    }

    @Test
    fun `sendMessage success clears messageText and does not set error`() {
        val chat = Chat(
            id = chatId,
            participant1Id = currentUserId,
            participant2Id = otherUserId
        )
        val message: Message = mockk()

        coEvery { createOrGetChatUseCase.invoke(currentUserId, otherUserId) } returns Result.success(chat)
        every { getChatMessagesUseCase.invoke(chatId) } returns flowOf(emptyList())
        coEvery { sendMessageUseCase.invoke(chatId, currentUserId, "Hola") } returns Result.success(message)

        viewModel.initChat(otherUser)
        viewModel.onMessageTextChange("Hola")

        viewModel.sendMessage()

        val state = viewModel.uiState
        assertEquals("", state.messageText)
        assertNull(state.errorMessage)
        coVerify { sendMessageUseCase.invoke(chatId, currentUserId, "Hola") }
    }

    @Test
    fun `sendMessage failure with MessageNotFoundException shows specific error`() {
        val chat = Chat(
            id = chatId,
            participant1Id = currentUserId,
            participant2Id = otherUserId
        )

        coEvery { createOrGetChatUseCase.invoke(currentUserId, otherUserId) } returns Result.success(chat)
        every { getChatMessagesUseCase.invoke(chatId) } returns flowOf(emptyList())
        coEvery {
            sendMessageUseCase.invoke(chatId, currentUserId, "Hola")
        } returns Result.failure(MessageNotFoundException(UUID.random()))

        viewModel.initChat(otherUser)
        viewModel.onMessageTextChange("Hola")

        viewModel.sendMessage()

        val state = viewModel.uiState
        assertEquals("", state.messageText)
        assertEquals("Message not found", state.errorMessage)
    }

    @Test
    fun `isOwnMessage returns true when senderId is current user`() {
        val ownMessage: Message = mockk()
        every { ownMessage.senderId } returns currentUserId

        val result = viewModel.isOwnMessage(ownMessage)

        assertTrue(result)
    }

    @Test
    fun `isOwnMessage returns false when senderId is other user`() {
        val otherMessage: Message = mockk()
        every { otherMessage.senderId } returns otherUserId

        val result = viewModel.isOwnMessage(otherMessage)

        assertFalse(result)
    }

    @Test
    fun `markMessagesAsRead delegates to use case`() {
        coEvery { markMessagesAsReadUseCase.invoke(chatId, currentUserId) } returns Result.success(Unit)

        viewModel.markMessagesAsRead(chatId, currentUserId)

        coVerify { markMessagesAsReadUseCase.invoke(chatId, currentUserId) }
    }

    @Test
    fun `cleanupChat cancels chat and resets state`() {
        val chat = Chat(
            id = chatId,
            participant1Id = currentUserId,
            participant2Id = otherUserId
        )
        val message: Message = mockk()

        coEvery { createOrGetChatUseCase.invoke(currentUserId, otherUserId) } returns Result.success(chat)
        every { getChatMessagesUseCase.invoke(chatId) } returns flowOf(listOf(message))

        viewModel.initChat(otherUser)
        viewModel.onMessageTextChange("Texto")

        viewModel.cleanupChat()

        val state = viewModel.uiState
        assertNull(viewModel.getChatId())
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.messageText)
        assertNull(state.errorMessage)
    }
}
