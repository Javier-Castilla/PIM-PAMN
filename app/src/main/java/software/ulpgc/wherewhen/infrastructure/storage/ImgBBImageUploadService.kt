package software.ulpgc.wherewhen.infrastructure.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import software.ulpgc.wherewhen.domain.ports.storage.ImageUploadService
import java.io.ByteArrayOutputStream

class ImgBBImageUploadService(
    private val context: Context,
    private val apiKey: String
) : ImageUploadService {

    private val client = OkHttpClient()

    override suspend fun uploadImage(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val base64Image = convertImageToBase64(imageUri)

            val requestBody = FormBody.Builder()
                .add("key", apiKey)
                .add("image", base64Image)
                .build()

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val imageUrl = json.getJSONObject("data").getString("url")
                Log.d("ImgBB", "Image uploaded successfully: $imageUrl")
                Result.success(imageUrl)
            } else {
                Log.e("ImgBB", "Upload failed: $responseBody")
                Result.failure(Exception("Failed to upload image: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ImgBB", "Error uploading image", e)
            Result.failure(e)
        }
    }

    private fun convertImageToBase64(imageUri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw IllegalArgumentException("Cannot open image URI")

        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val scaledBitmap = if (bitmap.width > 1920 || bitmap.height > 1920) {
            val ratio = minOf(1920f / bitmap.width, 1920f / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else {
            bitmap
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()

        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
