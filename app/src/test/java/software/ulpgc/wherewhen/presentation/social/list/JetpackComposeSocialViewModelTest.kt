package software.ulpgc.wherewhen.presentation.social.list

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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.friendship.*
import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.friendship.SentFriendRequestWithUser
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.usecases.friendship.*
import software.ulpgc.wherewhen.domain.usecases.friendship.GetUserFriendsUseCase
import software.ulpgc.wherewhen.domain.usecases.user.SearchUsersUseCase
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackComposeSocialViewModelTest {

    private lateinit var searchUsersUseCase: SearchUsersUseCase
    private lateinit var sendFriendRequestUseCase: SendFriendRequestUseCase
    private lateinit var checkFriendshipStatusUseCase: CheckFriendshipStatusUseCase
    private lateinit var getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase
    private lateinit var getSentFriendRequestsUseCase: GetSentFriendRequestsUseCase
    private lateinit var acceptFriendRequestUseCase: AcceptFriendRequestUseCase
    private lateinit var rejectFriendRequestUseCase: RejectFriendRequestUseCase
    private lateinit var cancelFriendRequestUseCase: CancelFriendRequestUseCase
    private lateinit var getUserFriendsUseCase: GetUserFriendsUseCase
    private lateinit var removeFriendUseCase: RemoveFriendUseCase

    private lateinit var viewModel: JetpackComposeSocialViewModel

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private val dispatcher = UnconfinedTestDispatcher()

    private val currentUserId: UUID = UUID.random()
    private val currentUserIdString: String = currentUserId.toString()
    private val otherUserId: UUID = UUID.random()
    private val otherEmail: Email = Email.create("other@example.com").getOrThrow()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        mockkStatic(FirebaseAuth::class)
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns currentUserIdString

        searchUsersUseCase = mockk()
        sendFriendRequestUseCase = mockk()
        checkFriendshipStatusUseCase = mockk()
        getPendingFriendRequestsUseCase = mockk()
        getSentFriendRequestsUseCase = mockk()
        acceptFriendRequestUseCase = mockk()
        rejectFriendRequestUseCase = mockk()
        cancelFriendRequestUseCase = mockk()
        getUserFriendsUseCase = mockk()
        removeFriendUseCase = mockk()

        viewModel = JetpackComposeSocialViewModel(
            searchUsersUseCase,
            sendFriendRequestUseCase,
            checkFriendshipStatusUseCase,
            getPendingFriendRequestsUseCase,
            getSentFriendRequestsUseCase,
            acceptFriendRequestUseCase,
            rejectFriendRequestUseCase,
            cancelFriendRequestUseCase,
            getUserFriendsUseCase,
            removeFriendUseCase
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

        assertEquals("", state.searchQuery)
        assertTrue(state.users.isEmpty())
        assertTrue(state.receivedRequests.isEmpty())
        assertTrue(state.sentRequests.isEmpty())
        assertTrue(state.friends.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.friendToRemove)
    }

    @Test
    fun `onSearchQueryChange with blank query clears users`() {
        val currentUser: User = mockk()
        val otherUser: User = mockk()
        every { currentUser.uuid } returns currentUserId
        every { otherUser.uuid } returns otherUserId

        coEvery { searchUsersUseCase.invoke("old") } returns Result.success(listOf(currentUser, otherUser))
        coEvery { checkFriendshipStatusUseCase.invoke(currentUserId, otherUserId) } returns Result.success(
            FriendshipStatus.FRIENDS
        )

        viewModel.onSearchQueryChange("old")
        assertFalse(viewModel.uiState.users.isEmpty())

        viewModel.onSearchQueryChange("")

        val state = viewModel.uiState
        assertEquals("", state.searchQuery)
        assertTrue(state.users.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onSearchQueryChange with non-empty query and empty results shows empty results`() {
        coEvery { searchUsersUseCase.invoke("john") } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("john")

        val state = viewModel.uiState
        assertEquals("john", state.searchQuery)
        assertTrue(state.users.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)

        coVerify { searchUsersUseCase.invoke("john") }
    }

    @Test
    fun `onSearchQueryChange with results maps to UserWithStatus and filters current user`() {
        val currentUser: User = mockk()
        val otherUser: User = mockk()

        every { currentUser.uuid } returns currentUserId
        every { otherUser.uuid } returns otherUserId

        coEvery { searchUsersUseCase.invoke("john") } returns Result.success(
            listOf(currentUser, otherUser)
        )
        coEvery { checkFriendshipStatusUseCase.invoke(currentUserId, otherUserId) } returns Result.success(
            FriendshipStatus.FRIENDS
        )

        viewModel.onSearchQueryChange("john")

        val state = viewModel.uiState
        assertEquals(1, state.users.size)
        assertSame(otherUser, state.users.first().user)
        assertEquals(FriendshipStatus.FRIENDS, state.users.first().status)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `sendFriendRequest success triggers loadPendingRequests`() {
        coEvery { sendFriendRequestUseCase.invoke(currentUserId, otherUserId) } returns Result.success(Unit)
        coEvery { getPendingFriendRequestsUseCase.invoke(currentUserId) } returns Result.success(emptyList())
        coEvery { getSentFriendRequestsUseCase.invoke(currentUserId) } returns Result.success(emptyList())

        viewModel.sendFriendRequest(otherUserId)

        assertNull(viewModel.uiState.errorMessage)
        coVerify { sendFriendRequestUseCase.invoke(currentUserId, otherUserId) }
        coVerify { getPendingFriendRequestsUseCase.invoke(currentUserId) }
        coVerify { getSentFriendRequestsUseCase.invoke(currentUserId) }
    }

    @Test
    fun `sendFriendRequest with SelfFriendRequestException shows specific error`() {
        coEvery { sendFriendRequestUseCase.invoke(currentUserId, otherUserId) } returns Result.failure(
            SelfFriendRequestException()
        )

        viewModel.sendFriendRequest(otherUserId)

        assertEquals("Cannot send request to yourself", viewModel.uiState.errorMessage)
    }

    @Test
    fun `sendFriendRequest with AlreadyFriendsException shows specific error`() {
        coEvery { sendFriendRequestUseCase.invoke(currentUserId, otherUserId) } returns Result.failure(
            AlreadyFriendsException(currentUserId, otherUserId)
        )

        viewModel.sendFriendRequest(otherUserId)

        assertEquals("Already friends", viewModel.uiState.errorMessage)
    }

    @Test
    fun `sendFriendRequest with UserNotFoundException shows specific error`() {
        coEvery { sendFriendRequestUseCase.invoke(currentUserId, otherUserId) } returns Result.failure(
            UserNotFoundException(otherUserId)
        )

        viewModel.sendFriendRequest(otherUserId)

        assertEquals("User not found", viewModel.uiState.errorMessage)
    }

    @Test
    fun `loadPendingRequests success sets received and sent lists`() {
        val pending: FriendRequestWithUser = mockk()
        val sent: SentFriendRequestWithUser = mockk()

        coEvery { getPendingFriendRequestsUseCase.invoke(currentUserId) } returns Result.success(
            listOf(pending)
        )
        coEvery { getSentFriendRequestsUseCase.invoke(currentUserId) } returns Result.success(
            listOf(sent)
        )

        viewModel.loadPendingRequests()

        val state = viewModel.uiState
        assertEquals(1, state.receivedRequests.size)
        assertEquals(1, state.sentRequests.size)
        assertSame(pending, state.receivedRequests.first())
        assertSame(sent, state.sentRequests.first())
        assertNull(state.errorMessage)
    }

    @Test
    fun `cancelFriendRequest with FriendRequestNotFoundException shows specific error`() {
        val requestId = UUID.random()

        coEvery {
            cancelFriendRequestUseCase.invoke(requestId, currentUserId)
        } returns Result.failure(FriendRequestNotFoundException(requestId))

        viewModel.cancelFriendRequest(requestId)

        assertEquals("Request not found", viewModel.uiState.errorMessage)
    }

    @Test
    fun `acceptFriendRequest success reloads pending requests and friends`() {
        val requestId = UUID.random()

        coEvery { acceptFriendRequestUseCase.invoke(requestId, currentUserId) } returns Result.success(
            Unit
        )
        coEvery { getPendingFriendRequestsUseCase.invoke(currentUserId) } returns Result.success(
            emptyList()
        )
        coEvery { getSentFriendRequestsUseCase.invoke(currentUserId) } returns Result.success(
            emptyList()
        )
        coEvery { getUserFriendsUseCase.invoke(currentUserId) } returns Result.success(emptyList())

        viewModel.acceptFriendRequest(requestId)

        coVerify { acceptFriendRequestUseCase.invoke(requestId, currentUserId) }
        coVerify { getPendingFriendRequestsUseCase.invoke(currentUserId) }
        coVerify { getSentFriendRequestsUseCase.invoke(currentUserId) }
        coVerify { getUserFriendsUseCase.invoke(currentUserId) }
    }

    @Test
    fun `rejectFriendRequest with UnauthorizedFriendshipActionException shows specific error`() {
        val requestId = UUID.random()

        coEvery {
            rejectFriendRequestUseCase.invoke(requestId, currentUserId)
        } returns Result.failure(UnauthorizedFriendshipActionException("Not authorized to reject"))

        viewModel.rejectFriendRequest(requestId)

        assertEquals("Not authorized to reject", viewModel.uiState.errorMessage)
    }

    @Test
    fun `loadFriends success sets friends list`() {
        val friend: User = mockk()
        every { friend.uuid } returns otherUserId

        coEvery { getUserFriendsUseCase.invoke(currentUserId) } returns Result.success(
            listOf(friend)
        )

        viewModel.loadFriends()

        val state = viewModel.uiState
        assertEquals(1, state.friends.size)
        assertSame(friend, state.friends.first())
        assertNull(state.errorMessage)
    }

    @Test
    fun `show and hide remove friend dialog toggle friendToRemove`() {
        val friend: User = mockk()

        viewModel.showRemoveFriendDialog(friend)
        assertSame(friend, viewModel.uiState.friendToRemove)

        viewModel.hideRemoveFriendDialog()
        assertNull(viewModel.uiState.friendToRemove)
    }

    @Test
    fun `confirmRemoveFriend success hides dialog and reloads friends`() {
        val friend: User = mockk()
        every { friend.uuid } returns otherUserId

        coEvery { removeFriendUseCase.invoke(currentUserId, otherUserId) } returns Result.success(
            Unit
        )
        coEvery { getUserFriendsUseCase.invoke(currentUserId) } returns Result.success(
            emptyList()
        )

        viewModel.showRemoveFriendDialog(friend)
        viewModel.confirmRemoveFriend()

        val state = viewModel.uiState
        assertNull(state.friendToRemove)
        coVerify { removeFriendUseCase.invoke(currentUserId, otherUserId) }
        coVerify { getUserFriendsUseCase.invoke(currentUserId) }
    }

    @Test
    fun `clearSearch resets query and users but keeps other data`() {
        val pending: FriendRequestWithUser = mockk()
        val sent: SentFriendRequestWithUser = mockk()
        val friend: User = mockk()
        every { friend.uuid } returns otherUserId

        coEvery { getPendingFriendRequestsUseCase.invoke(currentUserId) } returns Result.success(
            listOf(pending)
        )
        coEvery { getSentFriendRequestsUseCase.invoke(currentUserId) } returns Result.success(
            listOf(sent)
        )
        coEvery { getUserFriendsUseCase.invoke(currentUserId) } returns Result.success(
            listOf(friend)
        )

        viewModel.loadPendingRequests()
        viewModel.loadFriends()
        viewModel.onSearchQueryChange("john")
        viewModel.showError("error")

        viewModel.clearSearch()

        val state = viewModel.uiState
        assertEquals("", state.searchQuery)
        assertTrue(state.users.isEmpty())
        assertNull(state.errorMessage)
        assertEquals(1, state.friends.size)
        assertEquals(1, state.receivedRequests.size)
        assertEquals(1, state.sentRequests.size)
    }

    @Test
    fun `showError sets message and stops loading`() {
        viewModel.showLoading()

        viewModel.showError("error msg")

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals("error msg", state.errorMessage)
    }
}
