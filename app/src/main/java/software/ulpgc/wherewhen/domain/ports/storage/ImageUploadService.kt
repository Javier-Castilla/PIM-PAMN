package software.ulpgc.wherewhen.domain.ports.storage

import android.net.Uri

interface ImageUploadService {
    suspend fun uploadImage(imageUri: Uri): Result<String>
}
