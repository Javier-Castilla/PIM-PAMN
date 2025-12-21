package software.ulpgc.wherewhen.presentation.chat.list

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.model.chat.Chat
import software.ulpgc.wherewhen.domain.model.chat.ChatWithUser
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.usecases.chat.GetUserChatsUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackComposeChatsViewModelTest {

    private lateinit var getUserChatsUseCase: GetUserChatsUseCase
    private lateinit var viewModel: JetpackComposeChatsViewModel

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private val dispatcher = UnconfinedTestDispatcher()

    private val currentUserId: UUID = UUID.random()
    private val currentUserIdString: String = currentUserId.toString()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        mockkStatic(FirebaseAuth::class)
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns currentUserIdString

        getUserChatsUseCase = mockk()

        every { getUserChatsUseCase.invoke(currentUserId) } returns flowOf(emptyList())

        viewModel = JetpackComposeChatsViewModel(getUserChatsUseCase)
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state after init has empty chats and not loading`() {
        val state = viewModel.uiState

        assertTrue(state.chats.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(0, viewModel.totalUnreadCount)
    }

    @Test
    fun `loadChats with no authenticated user sets error`() {
        unmockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseAuth::class)

        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns null

        viewModel.loadChats()

        val state = viewModel.uiState
        assertEquals("User not authenticated", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `showChats sorts by lastMessageAt desc and clears error`() {
        val user1: User = mockk()
        val user2: User = mockk()
        val chat1 = Chat(
            id = UUID.random(),
            participant1Id = currentUserId,
            participant2Id = UUID.random()
        )
        val chat2 = Chat(
            id = UUID.random(),
            participant1Id = currentUserId,
            participant2Id = UUID.random()
        )
        val t1 = Instant.now().minusSeconds(600)
        val t2 = Instant.now()

        val c1 = ChatWithUser(chat1, user1, "old", t1, 1)
        val c2 = ChatWithUser(chat2, user2, "new", t2, 2)

        viewModel.showChats(listOf(c1, c2))

        val state = viewModel.uiState
        assertEquals(listOf(c2, c1), state.chats)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `totalUnreadCount sums unread of all chats`() {
        val user1: User = mockk()
        val user2: User = mockk()
        val chat1 = Chat(
            id = UUID.random(),
            participant1Id = currentUserId,
            participant2Id = UUID.random()
        )
        val chat2 = Chat(
            id = UUID.random(),
            participant1Id = currentUserId,
            participant2Id = UUID.random()
        )

        val c1 = ChatWithUser(chat1, user1, null, null, 3)
        val c2 = ChatWithUser(chat2, user2, null, null, 5)

        viewModel.showChats(listOf(c1, c2))

        assertEquals(8, viewModel.totalUnreadCount)
    }

    @Test
    fun `showLoading sets isLoading true`() {
        viewModel.showLoading()

        val state = viewModel.uiState
        assertTrue(state.isLoading)
    }

    @Test
    fun `hideLoading sets isLoading false`() {
        viewModel.showLoading()
        viewModel.hideLoading()

        val state = viewModel.uiState
        assertFalse(state.isLoading)
    }

    @Test
    fun `showError sets message and stops loading`() {
        viewModel.showLoading()

        viewModel.showError("error msg")

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals("error msg", state.errorMessage)
    }

    @Test
    fun `loadChats when use case emits chats calls showChats`() {
        val user: User = mockk()
        val chat = Chat(
            id = UUID.random(),
            participant1Id = currentUserId,
            participant2Id = UUID.random()
        )
        val chatWithUser = ChatWithUser(chat, user, "hi", Instant.now(), 1)

        every { getUserChatsUseCase.invoke(currentUserId) } returns flowOf(listOf(chatWithUser))

        val vm = JetpackComposeChatsViewModel(getUserChatsUseCase)

        val state = vm.uiState
        assertEquals(1, state.chats.size)
        assertEquals(chatWithUser, state.chats.first())
    }

    @Test
    fun `loadChats handles exceptions and sets error`() {
        every { getUserChatsUseCase.invoke(currentUserId) } returns flow {
            throw RuntimeException("boom")
        }

        viewModel.loadChats()

        val state = viewModel.uiState
        assertEquals("boom", state.errorMessage)
        assertFalse(state.isLoading)
    }
}
