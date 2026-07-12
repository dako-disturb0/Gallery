package com.gallery.app.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Format raster yang didukung untuk ekspor halaman PDF. */
enum class ImageFormat(val label: String, val extension: String, val mimeType: String) {
    PNG("PNG", "png", "image/png"),
    JPEG("JPEG", "jpg", "image/jpeg"),
    WEBP("WebP", "webp", "image/webp");

    fun compress(bitmap: Bitmap, out: java.io.OutputStream) {
        val format = when (this) {
            PNG -> Bitmap.CompressFormat.PNG
            JPEG -> Bitmap.CompressFormat.JPEG
            WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }
        bitmap.compress(format, 95, out)
    }
}

object PdfExport {

    private val TIMESTAMP = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Menyimpan [bitmap] sebagai gambar ke koleksi Pictures/Gallery lewat
     * MediaStore. Mengembalikan nama file bila berhasil, atau null bila gagal.
     */
    suspend fun saveImage(
        context: Context,
        bitmap: Bitmap,
        baseName: String,
        format: ImageFormat,
    ): String? = withContext(Dispatchers.IO) {
        val safeBase = baseName.substringBeforeLast('.').ifBlank { "PDF" }
            .replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
        val fileName = "${safeBase}_${TIMESTAMP.format(Date())}.${format.extension}"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Gallery")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        var uri: android.net.Uri? = null
        try {
            uri = resolver.insert(collection, values) ?: return@withContext null
            resolver.openOutputStream(uri)?.use { out ->
                format.compress(bitmap, out)
            } ?: return@withContext null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            fileName
        } catch (_: Exception) {
            uri?.let { runCatching { resolver.delete(it, null, null) } }
            null
        }
    }
}
