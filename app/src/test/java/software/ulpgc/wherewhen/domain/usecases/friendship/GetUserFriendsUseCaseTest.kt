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
import software.ulpgc.wherewhen.domain.model.friendship.Friendship
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.LocalDateTime

class GetUserFriendsUseCaseTest {

    private lateinit var friendshipRepository: FriendshipRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: GetUserFriendsUseCase

    @Before
    fun setup() {
        friendshipRepository = mockk()
        userRepository = mockk()
        useCase = GetUserFriendsUseCase(friendshipRepository, userRepository)
    }

    @Test
    fun `returns user friends successfully`() = runTest {
        val userId = UUID.random()
        val friendId = UUID.random()
        val friendship = Friendship(
            id = UUID.random(),
            user1Id = userId,
            user2Id = friendId,
            createdAt = LocalDateTime.now()
        )
        val friendUser = mockk<User>()

        coEvery { friendshipRepository.getFriendshipsForUser(userId) } returns Result.success(listOf(friendship))
        coEvery { userRepository.getPublicUser(friendId) } returns Result.success(friendUser)

        val result = useCase.invoke(userId)

        assertTrue(result.isSuccess)
        val list = result.getOrNull()
        assertNotNull(list)
        assertEquals(1, list?.size)
        assertEquals(friendUser, list?.first())

        coVerify { friendshipRepository.getFriendshipsForUser(userId) }
        coVerify { userRepository.getPublicUser(friendId) }
    }

    @Test
    fun `skips friendships when other user cannot be resolved`() = runTest {
        val userId = UUID.random()
        val friendId = UUID.random()
        val friendship = Friendship(
            id = UUID.random(),
            user1Id = userId,
            user2Id = friendId,
            createdAt = LocalDateTime.now()
        )

        coEvery { friendshipRepository.getFriendshipsForUser(userId) } returns Result.success(listOf(friendship))
        coEvery { userRepository.getPublicUser(friendId) } returns Result.failure(RuntimeException("not found"))

        val result = useCase.invoke(userId)

        assertTrue(result.isSuccess)
        val list = result.getOrNull()
        assertNotNull(list)
        assertTrue(list!!.isEmpty())

        coVerify { friendshipRepository.getFriendshipsForUser(userId) }
        coVerify { userRepository.getPublicUser(friendId) }
    }
}