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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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

    private val dispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    private val currentUserId: UUID = UUID.random()
    private val currentUserIdString: String = currentUserId.toString()
    private val otherUserId: UUID = UUID.random()
    private val otherEmail: Email = Email.create("other@example.com").getOrThrow()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        testScope = TestScope(dispatcher)

        mockkStatic(FirebaseAuth::class)
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns currentUserIdString

        searchUsersUseCase = mockk()
        sendFriendRequestUseCase = mockk()
        checkFriendshipStatusUseCase = mockk()
        getPendingFriendRequestsUseCase = mockk(relaxed = true)
        getSentFriendRequestsUseCase = mockk(relaxed = true)
        acceptFriendRequestUseCase = mockk()
        rejectFriendRequestUseCase = mockk()
        cancelFriendRequestUseCase = mockk()
        getUserFriendsUseCase = mockk(relaxed = true)
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
        testScope.cancel()
        Dispatchers.resetMain()
        unmockkStatic(FirebaseAuth::class)
    }

    @Test
    fun `initial state is empty and not loading`() = runTest {
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
    fun `onSearchQueryChange with blank query clears users`() = runTest {
        val currentUser: User = mockk()
        val otherUser: User = mockk()
        every { currentUser.uuid } returns currentUserId
        every { otherUser.uuid } returns otherUserId

        coEvery { searchUsersUseCase.invoke("old") } returns Result.success(listOf(currentUser, otherUser))
        coEvery { checkFriendshipStatusUseCase.invoke(currentUserId, otherUserId) } returns Result.success(
            FriendshipStatus.FRIENDS
        )

        viewModel.onSearchQueryChange("old")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.users.isEmpty())

        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        val state = viewModel.uiState
        assertEquals("", state.searchQuery)
        assertTrue(state.users.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onSearchQueryChange with non-empty query and empty results shows empty results`() = runTest {
        coEvery { searchUsersUseCase.invoke("john") } returns Result.success(emptyList())

        viewModel.onSearchQueryChange("john")
        advanceUntilIdle()

        val state = viewModel.uiState
        assertEquals("john", state.searchQuery)
        assertTrue(state.users.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)

        coVerify { searchUsersUseCase.invoke("john") }
    }

    @Test
    fun `onSearchQueryChange with results maps to UserWithStatus and filters current user`() = runTest {
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
        advanceUntilIdle()

        val state = viewModel.uiState
        assertEquals(1, state.users.size)
        assertSame(otherUser, state.users.first().user)
        assertEquals(FriendshipStatus.FRIENDS, state.users.first().status)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `sendFriendRequest success triggers loadPendingRequests`() = runTest {
        coEvery { sendFriendRequestUseCase.invoke(currentUserId, otherUserId) } returns Result.success(Unit)
        every { getPendingFriendRequestsUseCase.invoke(currentUserId) } returns flowOf(emptyList())
        every { getSentFriendRequestsUseCase.invoke(currentUserId) } returns flowOf(emptyList())

        viewModel.sendFriendRequest(otherUserId)
        advanceUntilIdle()

        assertNull(viewModel.uiState.errorMessage)
        coVerify { sendFriendRequestUseCase.invoke(currentUserId, otherUserId) }
    }

    @Test
    fun `sendFriendRequest with SelfFriendRequestException shows specific error`() = runTest {
        coEvery { sendFriendRequestUseCase.invoke(currentUserId, otherUserId) } returns Result.failure(
            SelfFriendRequestException()
        )

        viewModel.sendFriendRequest(otherUserId)
        advanceUntilIdle()

        assertEquals("Cannot send request to yourself", viewModel.uiState.errorMessage)
    }

    @Test
    fun `sendFriendRequest with AlreadyFriendsException shows specific error`() = runTest {
        coEvery { sendFriendRequestUseCase.invoke(currentUserId, otherUserId) } returns Result.failure(
            AlreadyFriendsException(currentUserId, otherUserId)
        )

        viewModel.sendFriendRequest(otherUserId)
        advanceUntilIdle()

        assertEquals("Already friends", viewModel.uiState.errorMessage)
    }

    @Test
    fun `sendFriendRequest with UserNotFoundException shows specific error`() = runTest {
        coEvery { sendFriendRequestUseCase.invoke(currentUserId, otherUserId) } returns Result.failure(
            UserNotFoundException(otherUserId)
        )

        viewModel.sendFriendRequest(otherUserId)
        advanceUntilIdle()

        assertEquals("User not found", viewModel.uiState.errorMessage)
    }

    @Test
    fun `loadPendingRequests success sets received and sent lists`() = runTest {
        val pending: FriendRequestWithUser = mockk()
        val sent: SentFriendRequestWithUser = mockk()

        every { getPendingFriendRequestsUseCase.invoke(currentUserId) } returns flowOf(
            listOf(pending)
        )
        every { getSentFriendRequestsUseCase.invoke(currentUserId) } returns flowOf(
            listOf(sent)
        )

        viewModel.loadPendingRequests()
        advanceUntilIdle()

        val state = viewModel.uiState
        assertEquals(1, state.receivedRequests.size)
        assertEquals(1, state.sentRequests.size)
        assertSame(pending, state.receivedRequests.first())
        assertSame(sent, state.sentRequests.first())
        assertNull(state.errorMessage)
    }

    @Test
    fun `cancelFriendRequest with FriendRequestNotFoundException shows specific error`() = runTest {
        val requestId = UUID.random()

        coEvery {
            cancelFriendRequestUseCase.invoke(requestId, currentUserId)
        } returns Result.failure(FriendRequestNotFoundException(requestId))

        viewModel.cancelFriendRequest(requestId)
        advanceUntilIdle()

        assertEquals("Request not found", viewModel.uiState.errorMessage)
    }

    @Test
    fun `acceptFriendRequest success reloads pending requests and friends`() = runTest {
        val requestId = UUID.random()

        coEvery { acceptFriendRequestUseCase.invoke(requestId, currentUserId) } returns Result.success(
            Unit
        )
        every { getPendingFriendRequestsUseCase.invoke(currentUserId) } returns flowOf(
            emptyList()
        )
        every { getSentFriendRequestsUseCase.invoke(currentUserId) } returns flowOf(
            emptyList()
        )
        every { getUserFriendsUseCase.invoke(currentUserId) } returns flowOf(emptyList())

        viewModel.acceptFriendRequest(requestId)
        advanceUntilIdle()

        coVerify { acceptFriendRequestUseCase.invoke(requestId, currentUserId) }
    }

    @Test
    fun `rejectFriendRequest with UnauthorizedFriendshipActionException shows specific error`() = runTest {
        val requestId = UUID.random()

        coEvery {
            rejectFriendRequestUseCase.invoke(requestId, currentUserId)
        } returns Result.failure(UnauthorizedFriendshipActionException("Not authorized to reject"))

        viewModel.rejectFriendRequest(requestId)
        advanceUntilIdle()

        assertEquals("Not authorized to reject", viewModel.uiState.errorMessage)
    }

    @Test
    fun `loadFriends success sets friends list`() = runTest {
        val friend: User = mockk()
        every { friend.uuid } returns otherUserId

        every { getUserFriendsUseCase.invoke(currentUserId) } returns flowOf(
            listOf(friend)
        )

        viewModel.loadFriends()
        advanceUntilIdle()

        val state = viewModel.uiState
        assertEquals(1, state.friends.size)
        assertSame(friend, state.friends.first())
        assertNull(state.errorMessage)
    }

    @Test
    fun `show and hide remove friend dialog toggle friendToRemove`() = runTest {
        val friend: User = mockk()

        viewModel.showRemoveFriendDialog(friend)
        assertSame(friend, viewModel.uiState.friendToRemove)

        viewModel.hideRemoveFriendDialog()
        assertNull(viewModel.uiState.friendToRemove)
    }

    @Test
    fun `confirmRemoveFriend success hides dialog and reloads friends`() = runTest {
        val friend: User = mockk()
        every { friend.uuid } returns otherUserId

        coEvery { removeFriendUseCase.invoke(currentUserId, otherUserId) } returns Result.success(
            Unit
        )
        every { getUserFriendsUseCase.invoke(currentUserId) } returns flowOf(
            emptyList()
        )

        viewModel.showRemoveFriendDialog(friend)
        viewModel.confirmRemoveFriend()
        advanceUntilIdle()

        val state = viewModel.uiState
        assertNull(state.friendToRemove)
        coVerify { removeFriendUseCase.invoke(currentUserId, otherUserId) }
    }

    @Test
    fun `clearSearch resets query and users but keeps other data`() = runTest {
        val pending: FriendRequestWithUser = mockk()
        val sent: SentFriendRequestWithUser = mockk()
        val friend: User = mockk()
        every { friend.uuid } returns otherUserId

        every { getPendingFriendRequestsUseCase.invoke(currentUserId) } returns flowOf(
            listOf(pending)
        )
        every { getSentFriendRequestsUseCase.invoke(currentUserId) } returns flowOf(
            listOf(sent)
        )
        every { getUserFriendsUseCase.invoke(currentUserId) } returns flowOf(
            listOf(friend)
        )
        coEvery { searchUsersUseCase.invoke("john") } returns Result.success(emptyList())

        viewModel.loadPendingRequests()
        viewModel.loadFriends()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("john")
        advanceUntilIdle()

        viewModel.showError("error")

        viewModel.clearSearch()
        advanceUntilIdle()

        val state = viewModel.uiState
        assertEquals("", state.searchQuery)
        assertTrue(state.users.isEmpty())
        assertNull(state.errorMessage)
        assertEquals(1, state.friends.size)
        assertEquals(1, state.receivedRequests.size)
        assertEquals(1, state.sentRequests.size)
    }

    @Test
    fun `showError sets message and stops loading`() = runTest {
        viewModel.showLoading()

        viewModel.showError("error msg")

        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals("error msg", state.errorMessage)
    }
}
