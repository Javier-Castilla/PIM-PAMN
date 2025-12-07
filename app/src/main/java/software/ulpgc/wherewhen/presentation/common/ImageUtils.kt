package software.ulpgc.wherewhen.presentation.common

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

fun Context.normalizeImageUriToJpegBytes(uri: Uri, quality: Int = 90): ByteArray {
    val resolver: ContentResolver = contentResolver
    val inputStream: InputStream = resolver.openInputStream(uri) ?: return ByteArray(0)

    val originalBytes = inputStream.use { it.readBytes() }
    if (originalBytes.isEmpty()) return originalBytes

    val exif = ExifInterface(originalBytes.inputStream())
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )

    val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
        ?: return originalBytes

    val rotatedBitmap = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
        else -> bitmap
    }

    val outputStream = ByteArrayOutputStream()
    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    val result = outputStream.toByteArray()
    if (rotatedBitmap != bitmap) {
        rotatedBitmap.recycle()
    }
    return result
}

private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}
