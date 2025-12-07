package software.ulpgc.wherewhen.domain.usecases.user

import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.ports.storage.ImageUploadService
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class UpdateProfileImageUseCaseTest {

    private lateinit var imageUploadService: ImageUploadService
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: UpdateProfileImageUseCase

    @Before
    fun setup() {
        imageUploadService = mockk()
        userRepository = mockk()
        useCase = UpdateProfileImageUseCase(imageUploadService, userRepository)
    }

    @Test
    fun `updates profile image successfully`() = runTest {
        val userId = UUID.Companion.random()
        val uri = mockk<Uri>()
        val imageUrl = "https://example.com/image.jpg"

        coEvery { imageUploadService.uploadImage(uri) } returns Result.success(imageUrl)
        coEvery { userRepository.updateProfileImage(userId, imageUrl) } returns Result.success(Unit)

        val result = useCase.invoke(userId, uri)

        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(imageUrl, result.getOrNull())
        coVerify { imageUploadService.uploadImage(uri) }
        coVerify { userRepository.updateProfileImage(userId, imageUrl) }
    }

    @Test
    fun `fails when image upload fails`() = runTest {
        val userId = UUID.Companion.random()
        val uri = mockk<Uri>()
        val error = RuntimeException("upload error")

        coEvery { imageUploadService.uploadImage(uri) } returns Result.failure(error)

        val result = useCase.invoke(userId, uri)

        Assert.assertTrue(result.isFailure)
        Assert.assertEquals(error, result.exceptionOrNull())
        coVerify { imageUploadService.uploadImage(uri) }
        coVerify(exactly = 0) { userRepository.updateProfileImage(any(), any()) }
    }

    @Test
    fun `fails when repository update fails`() = runTest {
        val userId = UUID.Companion.random()
        val uri = mockk<Uri>()
        val imageUrl = "https://example.com/image.jpg"
        val error = RuntimeException("db error")

        coEvery { imageUploadService.uploadImage(uri) } returns Result.success(imageUrl)
        coEvery {
            userRepository.updateProfileImage(
                userId,
                imageUrl
            )
        } returns Result.failure(error)

        val result = useCase.invoke(userId, uri)

        Assert.assertTrue(result.isFailure)
        Assert.assertEquals(error, result.exceptionOrNull())
        coVerify { imageUploadService.uploadImage(uri) }
        coVerify { userRepository.updateProfileImage(userId, imageUrl) }
    }
}