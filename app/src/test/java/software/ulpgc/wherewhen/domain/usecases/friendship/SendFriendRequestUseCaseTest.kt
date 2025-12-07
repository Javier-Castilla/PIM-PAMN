package software.ulpgc.wherewhen.domain.usecases.friendship

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.friendship.AlreadyFriendsException
import software.ulpgc.wherewhen.domain.exceptions.friendship.FriendRequestAlreadyExistsException
import software.ulpgc.wherewhen.domain.exceptions.friendship.FriendshipNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.friendship.SelfFriendRequestException
import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.model.friendship.Friendship
import software.ulpgc.wherewhen.domain.model.friendship.SentFriendRequestWithUser
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class SendFriendRequestUseCaseTest {

    private lateinit var friendRequestRepository: FriendRequestRepository
    private lateinit var friendshipRepository: FriendshipRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: SendFriendRequestUseCase

    @Before
    fun setup() {
        friendRequestRepository = mockk()
        friendshipRepository = mockk()
        userRepository = mockk()
        useCase = SendFriendRequestUseCase(friendRequestRepository, friendshipRepository, userRepository)
    }

    @Test
    fun `fails when sending request to self`() = runTest {
        val userId = UUID.random()

        val result = useCase.invoke(userId, userId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SelfFriendRequestException)
        coVerify(exactly = 0) { userRepository.getPublicUser(any()) }
        coVerify(exactly = 0) { friendshipRepository.existsBetweenUsers(any(), any()) }
        coVerify(exactly = 0) { friendRequestRepository.getPendingBetweenUsers(any(), any()) }
        coVerify(exactly = 0) { friendRequestRepository.create(any()) }
    }

    @Test
    fun `fails when receiver user does not exist`() = runTest {
        val senderId = UUID.random()
        val receiverId = UUID.random()

        coEvery { userRepository.getPublicUser(receiverId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(senderId, receiverId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UserNotFoundException)
        coVerify { userRepository.getPublicUser(receiverId) }
        coVerify(exactly = 0) { friendshipRepository.existsBetweenUsers(any(), any()) }
        coVerify(exactly = 0) { friendRequestRepository.getPendingBetweenUsers(any(), any()) }
        coVerify(exactly = 0) { friendRequestRepository.create(any()) }
    }

    @Test
    fun `fails when users are already friends`() = runTest {
        val senderId = UUID.random()
        val receiverId = UUID.random()

        coEvery { userRepository.getPublicUser(receiverId) } returns Result.success(mockk<User>())
        coEvery { friendshipRepository.existsBetweenUsers(senderId, receiverId) } returns Result.success(true)

        val result = useCase.invoke(senderId, receiverId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AlreadyFriendsException)
        coVerify { userRepository.getPublicUser(receiverId) }
        coVerify { friendshipRepository.existsBetweenUsers(senderId, receiverId) }
        coVerify(exactly = 0) { friendRequestRepository.getPendingBetweenUsers(any(), any()) }
        coVerify(exactly = 0) { friendRequestRepository.create(any()) }
    }

    @Test
    fun `fails when pending request already exists`() = runTest {
        val senderId = UUID.random()
        val receiverId = UUID.random()
        val existingRequest = FriendRequest(
            id = UUID.random(),
            senderId = senderId,
            receiverId = receiverId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )

        coEvery { userRepository.getPublicUser(receiverId) } returns Result.success(mockk<User>())
        coEvery { friendshipRepository.existsBetweenUsers(senderId, receiverId) } returns Result.success(false)
        coEvery {
            friendRequestRepository.getPendingBetweenUsers(senderId, receiverId)
        } returns Result.success(existingRequest)

        val result = useCase.invoke(senderId, receiverId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FriendRequestAlreadyExistsException)
        coVerify { userRepository.getPublicUser(receiverId) }
        coVerify { friendshipRepository.existsBetweenUsers(senderId, receiverId) }
        coVerify { friendRequestRepository.getPendingBetweenUsers(senderId, receiverId) }
        coVerify(exactly = 0) { friendRequestRepository.create(any()) }
    }

    @Test
    fun `creates friend request successfully`() = runTest {
        val senderId = UUID.random()
        val receiverId = UUID.random()
        val createdRequest = FriendRequest(
            id = UUID.random(),
            senderId = senderId,
            receiverId = receiverId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )

        coEvery { userRepository.getPublicUser(receiverId) } returns Result.success(mockk<User>())
        coEvery { friendshipRepository.existsBetweenUsers(senderId, receiverId) } returns Result.success(false)
        coEvery {
            friendRequestRepository.getPendingBetweenUsers(senderId, receiverId)
        } returns Result.success<FriendRequest?>(null)
        coEvery { friendRequestRepository.create(any()) } returns Result.success(createdRequest)

        val result = useCase.invoke(senderId, receiverId)

        assertTrue(result.isSuccess)
        coVerify { userRepository.getPublicUser(receiverId) }
        coVerify { friendshipRepository.existsBetweenUsers(senderId, receiverId) }
        coVerify { friendRequestRepository.getPendingBetweenUsers(senderId, receiverId) }
        coVerify { friendRequestRepository.create(any()) }
    }
}
