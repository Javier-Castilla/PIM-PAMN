package software.ulpgc.wherewhen.domain.usecases.user

import android.net.Uri
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.ports.storage.ImageUploadService
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class UpdateProfileImageUseCase(
    private val imageUploadService: ImageUploadService,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: UUID, imageUri: Uri): Result<String> {
        return try {
            val imageUrl = imageUploadService.uploadImage(imageUri).getOrThrow()
            
            userRepository.updateProfileImage(userId, imageUrl).getOrThrow()
            
            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
