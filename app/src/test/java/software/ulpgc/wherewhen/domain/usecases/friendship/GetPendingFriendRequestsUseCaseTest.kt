package software.ulpgc.wherewhen.domain.usecases.friendship

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetPendingFriendRequestsUseCaseTest {

    private lateinit var friendRequestRepository: FriendRequestRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: GetPendingFriendRequestsUseCase

    @Before
    fun setup() {
        friendRequestRepository = mockk()
        userRepository = mockk()
        useCase = GetPendingFriendRequestsUseCase(friendRequestRepository, userRepository)
    }

    @Test
    fun `returns pending friend requests with users`() = runTest {
        val userId = UUID.random()
        val senderId = UUID.random()
        val request = FriendRequest(
            id = UUID.random(),
            senderId = senderId,
            receiverId = userId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )
        val senderUser = mockk<User>()
        val expected = FriendRequestWithUser(request, senderUser)

        coEvery { friendRequestRepository.getPendingRequestsForUser(userId) } returns flowOf(listOf(request))
        coEvery { userRepository.getPublicUser(senderId) } returns Result.success(senderUser)

        val result = useCase.invoke(userId).first()

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(expected, result.first())

        coVerify { friendRequestRepository.getPendingRequestsForUser(userId) }
        coVerify { userRepository.getPublicUser(senderId) }
    }

    @Test
    fun `filters out requests when user lookup fails`() = runTest {
        val userId = UUID.random()
        val senderId = UUID.random()
        val request = FriendRequest(
            id = UUID.random(),
            senderId = senderId,
            receiverId = userId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )

        coEvery { friendRequestRepository.getPendingRequestsForUser(userId) } returns flowOf(listOf(request))
        coEvery { userRepository.getPublicUser(senderId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(userId).first()

        assertNotNull(result)
        assertTrue(result.isEmpty())

        coVerify { friendRequestRepository.getPendingRequestsForUser(userId) }
        coVerify { userRepository.getPublicUser(senderId) }
    }
}
