package com.gallery.app.data

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.Formatter
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class MetadataField(val label: String, val value: String)

data class MetadataSection(val title: String, val fields: List<MetadataField>)

data class MediaMetadata(
    val sections: List<MetadataSection>,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

object MetadataReader {

    fun read(context: Context, item: MediaItem): MediaMetadata =
        if (item.isVideo) readVideo(context, item) else readImage(context, item)

    private fun readImage(context: Context, item: MediaItem): MediaMetadata {
        val sections = mutableListOf<MetadataSection>()
        sections.add(fileSection(context, item))

        var width = item.width
        var height = item.height
        var latitude: Double? = null
        var longitude: Double? = null
        val camera = mutableListOf<MetadataField>()
        var captured: String? = null

        val exifUris = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(exifUri(item.uri))
            add(item.uri)
        }
        for (source in exifUris) {
            try {
                context.contentResolver.openInputStream(source).use { stream ->
                    if (stream == null) return@use
                    val exif = ExifInterface(stream)
                    if (width == 0) width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                    if (height == 0) height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

                    if (camera.isEmpty()) {
                        val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                        val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
                        if (!make.isNullOrEmpty()) camera.add(MetadataField("Produsen", make))
                        if (!model.isNullOrEmpty()) camera.add(MetadataField("Model", model))

                        val aperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                        if (aperture > 0) camera.add(MetadataField("Apertur", "f/" + trimNumber(aperture)))
                        val exposure = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                        if (exposure > 0) camera.add(MetadataField("Kecepatan rana", formatExposure(exposure)))
                        val iso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
                        if (iso > 0) camera.add(MetadataField("ISO", iso.toString()))
                        val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                        if (focal > 0) camera.add(MetadataField("Panjang fokus", trimNumber(focal) + " mm"))

                        captured = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    }

                    val latLong = exif.latLong
                    if (latLong != null) {
                        latitude = latLong[0]
                        longitude = latLong[1]
                    }
                }
                if (latitude != null) break
            } catch (e: Exception) {
            }
        }

        if (width == 0 || height == 0) {
            try {
                context.contentResolver.openInputStream(item.uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, opts)
                    if (width == 0) width = opts.outWidth
                    if (height == 0) height = opts.outHeight
                }
            } catch (e: Exception) {
            }
        }

        val imageFields = mutableListOf<MetadataField>()
        if (width > 0 && height > 0) {
            imageFields.add(MetadataField("Dimensi", "$width × $height"))
            val megapixels = width.toLong() * height / 1_000_000.0
            imageFields.add(MetadataField("Resolusi", String.format(Locale.US, "%.1f MP", megapixels)))
        }
        captured?.let { imageFields.add(MetadataField("Diambil", it)) }
        if (imageFields.isNotEmpty()) sections.add(MetadataSection("Gambar", imageFields))
        if (camera.isNotEmpty()) sections.add(MetadataSection("Kamera", camera))

        val lat = latitude
        val lon = longitude
        if (lat != null && lon != null) {
            sections.add(locationSection(lat, lon))
        }
        return MediaMetadata(sections, lat, lon)
    }

    private fun readVideo(context: Context, item: MediaItem): MediaMetadata {
        val sections = mutableListOf<MetadataSection>()
        sections.add(fileSection(context, item))

        val videoFields = mutableListOf<MetadataField>()
        var latitude: Double? = null
        var longitude: Double? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, item.uri)
            var width = item.width
            var height = item.height
            if (width == 0) {
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            }
            if (height == 0) {
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            }
            if (width > 0 && height > 0) videoFields.add(MetadataField("Dimensi", "$width × $height"))
            if (item.duration > 0) videoFields.add(MetadataField("Durasi", formatDuration(item.duration)))

            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()?.let {
                videoFields.add(MetadataField("Bitrate", "${it / 1000} kbps"))
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.let {
                val fps = it.toDoubleOrNull()
                videoFields.add(MetadataField("Frame rate", if (fps != null) "${trimNumber(fps)} fps" else "$it fps"))
            }

            val parsed = parseIso6709(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION))
            if (parsed != null) {
                latitude = parsed.first
                longitude = parsed.second
            }
        } catch (e: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
            }
        }

        if (videoFields.isNotEmpty()) sections.add(MetadataSection("Video", videoFields))
        val lat = latitude
        val lon = longitude
        if (lat != null && lon != null) {
            sections.add(locationSection(lat, lon))
        }
        return MediaMetadata(sections, lat, lon)
    }

    private fun fileSection(context: Context, item: MediaItem): MetadataSection {
        val fields = mutableListOf(
            MetadataField("Nama", item.displayName),
            MetadataField("Ukuran", Formatter.formatFileSize(context, item.size)),
            MetadataField("Jenis", item.mimeType.ifEmpty { "-" }),
        )
        item.bucketName?.let { fields.add(MetadataField("Folder", it)) }
        fields.add(MetadataField("Ditambahkan", formatFullDate(item.dateAdded)))
        return MetadataSection("File", fields)
    }

    private fun locationSection(lat: Double, lon: Double): MetadataSection =
        MetadataSection(
            "Lokasi",
            listOf(MetadataField("Koordinat", String.format(Locale.US, "%.6f, %.6f", lat, lon))),
        )

    private fun trimNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else String.format(Locale.US, "%.1f", value)

    private fun formatExposure(seconds: Double): String =
        if (seconds < 1.0) "1/${(1.0 / seconds).roundToInt()} dtk" else "${trimNumber(seconds)} dtk"

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }

    private fun formatFullDate(epochSeconds: Long): String {
        if (epochSeconds <= 0) return "-"
        val formatter = SimpleDateFormat("d MMM yyyy, HH.mm", Locale("id", "ID"))
        return formatter.format(Date(epochSeconds * 1000))
    }

    private fun parseIso6709(value: String?): Pair<Double, Double>? {
        if (value.isNullOrBlank()) return null
        val match = Regex("([+-]\\d+(?:\\.\\d+)?)([+-]\\d+(?:\\.\\d+)?)").find(value) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        return lat to lon
    }

    private fun exifUri(uri: Uri): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.setRequireOriginal(uri) else uri
}
