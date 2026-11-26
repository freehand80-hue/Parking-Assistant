package com.example.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

data class PendingPhotoCapture(
    val file: File,
    val contentUri: Uri
)

object PhotoFileManager {

    private const val PHOTOS_DIR = "parking_photos"

    fun createCaptureFile(context: Context): PendingPhotoCapture {
        val dir = File(context.filesDir, PHOTOS_DIR).apply {
            if (!exists()) mkdirs()
        }
        val photoFile = File(dir, "pillar_${System.currentTimeMillis()}.jpg")
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, photoFile)
        return PendingPhotoCapture(file = photoFile, contentUri = contentUri)
    }

    fun deleteFileSafely(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
            // Ignore cleanup failure
        }
    }
}
