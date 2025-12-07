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
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class CancelFriendRequestUseCaseTest {

    private lateinit var friendRequestRepository: FriendRequestRepository
    private lateinit var useCase: CancelFriendRequestUseCase

    @Before
    fun setup() {
        friendRequestRepository = mockk()
        useCase = CancelFriendRequestUseCase(friendRequestRepository)
    }

    @Test
    fun `cancels request successfully`() = runTest {
        val requestId = UUID.random()
        val userId = UUID.random()
        val receiverId = UUID.random()
        val request = FriendRequest(
            id = requestId,
            senderId = userId,
            receiverId = receiverId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )

        coEvery { friendRequestRepository.getById(requestId) } returns Result.success(request)
        coEvery { friendRequestRepository.delete(requestId) } returns Result.success(Unit)

        val result = useCase.invoke(requestId, userId)

        assertTrue(result.isSuccess)
        coVerify { friendRequestRepository.getById(requestId) }
        coVerify { friendRequestRepository.delete(requestId) }
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
        coVerify(exactly = 0) { friendRequestRepository.delete(requestId) }
    }

    @Test
    fun `fails when user is not sender`() = runTest {
        val requestId = UUID.random()
        val senderId = UUID.random()
        val otherUserId = UUID.random()
        val receiverId = UUID.random()
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
        coVerify(exactly = 0) { friendRequestRepository.delete(requestId) }
    }
}
