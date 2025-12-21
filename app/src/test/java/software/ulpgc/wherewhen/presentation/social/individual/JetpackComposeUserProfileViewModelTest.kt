package software.ulpgc.wherewhen.presentation.social.individual

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
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.friendship.SentFriendRequestWithUser
import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.usecases.friendship.AcceptFriendRequestUseCase
import software.ulpgc.wherewhen.domain.usecases.friendship.CancelFriendRequestUseCase
import software.ulpgc.wherewhen.domain.usecases.friendship.CheckFriendshipStatusUseCase
import software.ulpgc.wherewhen.domain.usecases.friendship.FriendRequestWithUser
import software.ulpgc.wherewhen.domain.usecases.friendship.FriendshipStatus
import software.ulpgc.wherewhen.domain.usecases.friendship.GetPendingFriendRequestsUseCase
import software.ulpgc.wherewhen.domain.usecases.friendship.GetSentFriendRequestsUseCase
import software.ulpgc.wherewhen.domain.usecases.friendship.RemoveFriendUseCase
import software.ulpgc.wherewhen.domain.usecases.friendship.RejectFriendRequestUseCase
import software.ulpgc.wherewhen.domain.usecases.friendship.SendFriendRequestUseCase
import software.ulpgc.wherewhen.domain.usecases.user.GetUserUseCase
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackComposeUserProfileViewModelTest {

    private lateinit var getUserUseCase: GetUserUseCase
    private lateinit var checkFriendshipStatusUseCase: CheckFriendshipStatusUseCase
    private lateinit var sendFriendRequestUseCase: SendFriendRequestUseCase
    private lateinit var cancelFriendRequestUseCase: CancelFriendRequestUseCase
    private lateinit var acceptFriendRequestUseCase: AcceptFriendRequestUseCase
    private lateinit var rejectFriendRequestUseCase: RejectFriendRequestUseCase
    private lateinit var getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase
    private lateinit var getSentFriendRequestsUseCase: GetSentFriendRequestsUseCase
    private lateinit var removeFriendUseCase: RemoveFriendUseCase

    private lateinit var viewModel: JetpackComposeUserProfileViewModel

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private val dispatcher = UnconfinedTestDispatcher()

    private val currentUserId: UUID = UUID.random()
    private val currentUserIdString: String = currentUserId.toString()
    private val targetUserId: UUID = UUID.random()
    private val targetUserIdString: String = targetUserId.toString()
    private val email: Email = Email.create("target@example.com").getOrThrow()
    private val profile: Profile = Profile(
        uuid = targetUserId,
        email = email,
        name = "Target User",
        description = "Desc"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        mockkStatic(FirebaseAuth::class)
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns currentUserIdString

        getUserUseCase = mockk()
        checkFriendshipStatusUseCase = mockk()
        sendFriendRequestUseCase = mockk()
        cancelFriendRequestUseCase = mockk()
        acceptFriendRequestUseCase = mockk()
        rejectFriendRequestUseCase = mockk()
        getPendingFriendRequestsUseCase = mockk()
        getSentFriendRequestsUseCase = mockk()
        removeFriendUseCase = mockk()

        coEvery { getUserUseCase.invoke(targetUserId) } returns Result.success(profile)
        coEvery { checkFriendshipStatusUseCase.invoke(currentUserId, targetUserId) } returns Result.success(
            FriendshipStatus.NOT_FRIENDS
        )

        viewModel = JetpackComposeUserProfileViewModel(
            getUserUseCase,
            checkFriendshipStatusUseCase,
            sendFriendRequestUseCase,
            cancelFriendRequestUseCase,
            acceptFriendRequestUseCase,
            rejectFriendRequestUseCase,
            getPendingFriendRequestsUseCase,
            getSentFriendRequestsUseCase,
            removeFriendUseCase
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `loadUserProfile with invalid uuid shows error`() {
        viewModel.loadUserProfile("invalid-uuid")

        val state = viewModel.uiState
        assertNull(state.profile)
        assertEquals("Invalid user ID", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadUserProfile when getUserUseCase fails shows error`() {
        coEvery { getUserUseCase.invoke(targetUserId) } returns Result.failure(RuntimeException("Not found"))

        viewModel.loadUserProfile(targetUserIdString)

        val state = viewModel.uiState
        assertNull(state.profile)
        assertEquals("Not found", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadUserProfile success sets profile and friendship status`() {
        coEvery { checkFriendshipStatusUseCase.invoke(currentUserId, targetUserId) } returns Result.success(
            FriendshipStatus.FRIENDS
        )

        viewModel.loadUserProfile(targetUserIdString)

        val state = viewModel.uiState
        assertEquals(profile, state.profile)
        assertEquals(FriendshipStatus.FRIENDS, state.friendshipStatus)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `loadUserProfile with REQUEST_RECEIVED loads pending request id for later actions`() {
        val requestId = UUID.random()
        val friendRequest: FriendRequest = mockk()
        val user: User = mockk()
        val requestWithUser: FriendRequestWithUser = mockk()

        every { requestWithUser.request } returns friendRequest
        every { friendRequest.senderId } returns targetUserId
        every { friendRequest.id } returns requestId

        coEvery { checkFriendshipStatusUseCase.invoke(currentUserId, targetUserId) } returns Result.success(
            FriendshipStatus.REQUEST_RECEIVED
        )
        coEvery { getPendingFriendRequestsUseCase.invoke(currentUserId) } returns flowOf(
            listOf(requestWithUser)
        )
        coEvery { acceptFriendRequestUseCase.invoke(requestId, currentUserId) } returns Result.success(Unit)

        viewModel.loadUserProfile(targetUserIdString)
        viewModel.acceptFriendRequest()

        val stateAfter = viewModel.uiState
        assertEquals(FriendshipStatus.FRIENDS, stateAfter.friendshipStatus)
        coVerify { acceptFriendRequestUseCase.invoke(requestId, currentUserId) }
    }

    @Test
    fun `sendFriendRequest success updates status to REQUEST_SENT`() {
        coEvery { sendFriendRequestUseCase.invoke(currentUserId, targetUserId) } returns Result.success(Unit)

        viewModel.loadUserProfile(targetUserIdString)
        viewModel.sendFriendRequest()

        val state = viewModel.uiState
        assertEquals(FriendshipStatus.REQUEST_SENT, state.friendshipStatus)
        assertNull(state.errorMessage)
    }

    @Test
    fun `sendFriendRequest failure shows error`() {
        coEvery { sendFriendRequestUseCase.invoke(currentUserId, targetUserId) } returns Result.failure(
            RuntimeException("Send error")
        )

        viewModel.loadUserProfile(targetUserIdString)
        viewModel.sendFriendRequest()

        val state = viewModel.uiState
        assertEquals("Send error", state.errorMessage)
    }

    @Test
    fun `show and hide cancel request dialog toggle flag`() {
        viewModel.showCancelRequestDialog()
        assertTrue(viewModel.uiState.showCancelRequestDialog)

        viewModel.hideCancelRequestDialog()
        assertFalse(viewModel.uiState.showCancelRequestDialog)
    }

    @Test
    fun `confirmCancelRequest finds request and cancels it`() {
        val requestId = UUID.random()
        val friendRequest: FriendRequest = mockk()
        val user: User = mockk()
        val sentWithUser: SentFriendRequestWithUser = mockk()

        every { sentWithUser.request } returns friendRequest
        every { friendRequest.receiverId } returns targetUserId
        every { friendRequest.id } returns requestId

        coEvery { getSentFriendRequestsUseCase.invoke(currentUserId) } returns flowOf(
            listOf(sentWithUser)
        )
        coEvery { cancelFriendRequestUseCase.invoke(requestId, currentUserId) } returns Result.success(Unit)

        viewModel.loadUserProfile(targetUserIdString)
        viewModel.showCancelRequestDialog()
        viewModel.confirmCancelRequest()

        val state = viewModel.uiState
        assertEquals(FriendshipStatus.NOT_FRIENDS, state.friendshipStatus)
        assertFalse(state.showCancelRequestDialog)
        coVerify { cancelFriendRequestUseCase.invoke(requestId, currentUserId) }
    }

    @Test
    fun `show and hide remove friend dialog toggle flag`() {
        viewModel.showRemoveFriendDialog()
        assertTrue(viewModel.uiState.showRemoveDialog)

        viewModel.hideRemoveFriendDialog()
        assertFalse(viewModel.uiState.showRemoveDialog)
    }

    @Test
    fun `confirmRemoveFriend success sets NOT_FRIENDS and hides dialog`() {
        coEvery { removeFriendUseCase.invoke(currentUserId, targetUserId) } returns Result.success(Unit)

        viewModel.loadUserProfile(targetUserIdString)
        viewModel.showRemoveFriendDialog()
        viewModel.confirmRemoveFriend()

        val state = viewModel.uiState
        assertEquals(FriendshipStatus.NOT_FRIENDS, state.friendshipStatus)
        assertFalse(state.showRemoveDialog)
        coVerify { removeFriendUseCase.invoke(currentUserId, targetUserId) }
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
    fun `updateFriendshipStatus updates status only`() {
        viewModel.updateFriendshipStatus(FriendshipStatus.FRIENDS)

        val state = viewModel.uiState
        assertEquals(FriendshipStatus.FRIENDS, state.friendshipStatus)
    }
}
