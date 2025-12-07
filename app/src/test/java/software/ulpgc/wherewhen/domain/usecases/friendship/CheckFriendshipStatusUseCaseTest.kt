package software.ulpgc.wherewhen.domain.usecases.friendship

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class CheckFriendshipStatusUseCaseTest {

    private lateinit var friendshipRepository: FriendshipRepository
    private lateinit var friendRequestRepository: FriendRequestRepository
    private lateinit var useCase: CheckFriendshipStatusUseCase

    @Before
    fun setup() {
        friendshipRepository = mockk()
        friendRequestRepository = mockk()
        useCase = CheckFriendshipStatusUseCase(friendshipRepository, friendRequestRepository)
    }

    @Test
    fun `returns FRIENDS when users are friends`() = runTest {
        val currentUserId = UUID.random()
        val targetUserId = UUID.random()

        coEvery { friendshipRepository.existsBetweenUsers(currentUserId, targetUserId) } returns Result.success(true)

        val result = useCase.invoke(currentUserId, targetUserId)

        assertTrue(result.isSuccess)
        assertEquals(FriendshipStatus.FRIENDS, result.getOrNull())
        coVerify { friendshipRepository.existsBetweenUsers(currentUserId, targetUserId) }
    }

    @Test
    fun `returns REQUEST_SENT when current user sent request`() = runTest {
        val currentUserId = UUID.random()
        val targetUserId = UUID.random()
        val request = FriendRequest(
            id = UUID.random(),
            senderId = currentUserId,
            receiverId = targetUserId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )

        coEvery { friendshipRepository.existsBetweenUsers(currentUserId, targetUserId) } returns Result.success(false)
        coEvery {
            friendRequestRepository.getPendingBetweenUsers(currentUserId, targetUserId)
        } returns Result.success<FriendRequest?>(request)
        coEvery {
            friendRequestRepository.getPendingBetweenUsers(targetUserId, currentUserId)
        } returns Result.success<FriendRequest?>(null)

        val result = useCase.invoke(currentUserId, targetUserId)

        assertTrue(result.isSuccess)
        assertEquals(FriendshipStatus.REQUEST_SENT, result.getOrNull())
        coVerify { friendshipRepository.existsBetweenUsers(currentUserId, targetUserId) }
        coVerify { friendRequestRepository.getPendingBetweenUsers(currentUserId, targetUserId) }
    }

    @Test
    fun `returns REQUEST_RECEIVED when current user received request`() = runTest {
        val currentUserId = UUID.random()
        val targetUserId = UUID.random()
        val request = FriendRequest(
            id = UUID.random(),
            senderId = targetUserId,
            receiverId = currentUserId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )

        coEvery { friendshipRepository.existsBetweenUsers(currentUserId, targetUserId) } returns Result.success(false)
        coEvery {
            friendRequestRepository.getPendingBetweenUsers(currentUserId, targetUserId)
        } returns Result.success<FriendRequest?>(null)
        coEvery {
            friendRequestRepository.getPendingBetweenUsers(targetUserId, currentUserId)
        } returns Result.success<FriendRequest?>(request)

        val result = useCase.invoke(currentUserId, targetUserId)

        assertTrue(result.isSuccess)
        assertEquals(FriendshipStatus.REQUEST_RECEIVED, result.getOrNull())
        coVerify { friendshipRepository.existsBetweenUsers(currentUserId, targetUserId) }
        coVerify { friendRequestRepository.getPendingBetweenUsers(targetUserId, currentUserId) }
    }

    @Test
    fun `returns NOT_FRIENDS when there is no relation`() = runTest {
        val currentUserId = UUID.random()
        val targetUserId = UUID.random()

        coEvery { friendshipRepository.existsBetweenUsers(currentUserId, targetUserId) } returns Result.success(false)
        coEvery {
            friendRequestRepository.getPendingBetweenUsers(currentUserId, targetUserId)
        } returns Result.success<FriendRequest?>(null)
        coEvery {
            friendRequestRepository.getPendingBetweenUsers(targetUserId, currentUserId)
        } returns Result.success<FriendRequest?>(null)

        val result = useCase.invoke(currentUserId, targetUserId)

        assertTrue(result.isSuccess)
        assertEquals(FriendshipStatus.NOT_FRIENDS, result.getOrNull())
        coVerify { friendshipRepository.existsBetweenUsers(currentUserId, targetUserId) }
    }
}
