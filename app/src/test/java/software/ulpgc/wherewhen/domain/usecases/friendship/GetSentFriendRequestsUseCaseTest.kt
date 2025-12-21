package software.ulpgc.wherewhen.domain.usecases.friendship

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequest
import software.ulpgc.wherewhen.domain.model.friendship.FriendRequestStatus
import software.ulpgc.wherewhen.domain.model.friendship.SentFriendRequestWithUser
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class GetSentFriendRequestsUseCaseTest {

    private lateinit var friendRequestRepository: FriendRequestRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: GetSentFriendRequestsUseCase

    @Before
    fun setup() {
        friendRequestRepository = mockk()
        userRepository = mockk()
        useCase = GetSentFriendRequestsUseCase(friendRequestRepository, userRepository)
    }

    @Test
    fun `returns sent friend requests with users`() = runTest {
        val userId = UUID.random()
        val receiverId = UUID.random()
        val request = FriendRequest(
            id = UUID.random(),
            senderId = userId,
            receiverId = receiverId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )
        val receiverUser = mockk<User>()
        val expected = SentFriendRequestWithUser(request, receiverUser)

        coEvery { friendRequestRepository.getSentRequestsFromUser(userId) } returns flowOf(listOf(request))
        coEvery { userRepository.getPublicUser(receiverId) } returns Result.success(receiverUser)

        val result = useCase.invoke(userId).first()

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(expected, result.first())

        coVerify { friendRequestRepository.getSentRequestsFromUser(userId) }
        coVerify { userRepository.getPublicUser(receiverId) }
    }

    @Test
    fun `filters out sent requests when user lookup fails`() = runTest {
        val userId = UUID.random()
        val receiverId = UUID.random()
        val request = FriendRequest(
            id = UUID.random(),
            senderId = userId,
            receiverId = receiverId,
            status = FriendRequestStatus.PENDING,
            createdAt = LocalDateTime.now()
        )

        coEvery { friendRequestRepository.getSentRequestsFromUser(userId) } returns flowOf(listOf(request))
        coEvery { userRepository.getPublicUser(receiverId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(userId).first()

        assertNotNull(result)
        assertTrue(result.isEmpty())

        coVerify { friendRequestRepository.getSentRequestsFromUser(userId) }
        coVerify { userRepository.getPublicUser(receiverId) }
    }
}
