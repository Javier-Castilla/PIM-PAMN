package software.ulpgc.wherewhen.domain.usecases.friendship

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.friendship.FriendRequestNotFoundException
import software.ulpgc.wherewhen.domain.exceptions.friendship.UnauthorizedFriendshipActionException
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.model.friendship.Friendship
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class AcceptFriendRequestUseCaseTest {

    private lateinit var friendRequestRepository: FriendRequestRepository
    private lateinit var friendshipRepository: FriendshipRepository
    private lateinit var useCase: AcceptFriendRequestUseCase

    @Before
    fun setup() {
        friendRequestRepository = mockk()
        friendshipRepository = mockk()
        useCase = AcceptFriendRequestUseCase(friendRequestRepository, friendshipRepository)
    }

    @Test
    fun `accepts friend request successfully`() = runTest {
        val requestId = UUID.random()
        val senderId = UUID.random()
        val receiverId = UUID.random()
        val userId = receiverId
        val request = FriendRequest(
            id = requestId,
            senderId = senderId,
            receiverId = receiverId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )
        val friendship = Friendship(
            id = UUID.random(),
            user1Id = senderId,
            user2Id = receiverId,
            createdAt = LocalDateTime.now()
        )

        coEvery { friendRequestRepository.getById(requestId) } returns Result.success(request)
        coEvery {
            friendRequestRepository.updateStatus(
                requestId,
                FriendRequestStatus.ACCEPTED,
                any()
            )
        } returns Result.success(request.copy(status = FriendRequestStatus.ACCEPTED))
        coEvery { friendshipRepository.create(any()) } returns Result.success(friendship)

        val result = useCase.invoke(requestId, userId)

        assertTrue(result.isSuccess)
        coVerify { friendRequestRepository.getById(requestId) }
        coVerify {
            friendRequestRepository.updateStatus(
                requestId,
                FriendRequestStatus.ACCEPTED,
                any()
            )
        }
        coVerify { friendshipRepository.create(any()) }
    }

    @Test
    fun `fails when request not found`() = runTest {
        val requestId = UUID.random()
        val userId = UUID.random()

        coEvery { friendRequestRepository.getById(requestId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(requestId, userId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FriendRequestNotFoundException)
        coVerify { friendRequestRepository.getById(requestId) }
    }

    @Test
    fun `fails when user is not receiver`() = runTest {
        val requestId = UUID.random()
        val senderId = UUID.random()
        val receiverId = UUID.random()
        val otherUserId = UUID.random()
        val request = FriendRequest(
            id = requestId,
            senderId = senderId,
            receiverId = receiverId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )

        coEvery { friendRequestRepository.getById(requestId) } returns Result.success(request)

        val result = useCase.invoke(requestId, otherUserId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnauthorizedFriendshipActionException)
        coVerify { friendRequestRepository.getById(requestId) }
    }
}
