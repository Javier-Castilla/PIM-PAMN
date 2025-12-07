package software.ulpgc.wherewhen.domain.usecases.friendship

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.exceptions.friendship.FriendshipNotFoundException
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class RemoveFriendUseCaseTest {

    private lateinit var friendshipRepository: FriendshipRepository
    private lateinit var useCase: RemoveFriendUseCase

    @Before
    fun setup() {
        friendshipRepository = mockk()
        useCase = RemoveFriendUseCase(friendshipRepository)
    }

    @Test
    fun `fails when friendship does not exist`() = runTest {
        val userId = UUID.random()
        val friendId = UUID.random()

        coEvery { friendshipRepository.existsBetweenUsers(userId, friendId) } returns Result.success(false)

        val result = useCase.invoke(userId, friendId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FriendshipNotFoundException)
        coVerify { friendshipRepository.existsBetweenUsers(userId, friendId) }
        coVerify(exactly = 0) { friendshipRepository.deleteBetweenUsers(any(), any()) }
    }

    @Test
    fun `removes friendship successfully`() = runTest {
        val userId = UUID.random()
        val friendId = UUID.random()

        coEvery { friendshipRepository.existsBetweenUsers(userId, friendId) } returns Result.success(true)
        coEvery { friendshipRepository.deleteBetweenUsers(userId, friendId) } returns Result.success(Unit)

        val result = useCase.invoke(userId, friendId)

        assertTrue(result.isSuccess)
        coVerify { friendshipRepository.existsBetweenUsers(userId, friendId) }
        coVerify { friendshipRepository.deleteBetweenUsers(userId, friendId) }
    }
}