package com.gallery.app.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val size: Long,
    val mimeType: String,
    val bucketId: String?,
    val bucketName: String?,
    val isFavorite: Boolean,
    val duration: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}

/**
 * A [MediaItem] with its EXIF GPS coordinates already resolved. Produced once by
 * the ViewModel during the geotag scan so the map layer never has to re-read EXIF.
 */
data class GeoMedia(
    val item: MediaItem,
    val lat: Double,
    val lon: Double,
) {
    val id: Long get() = item.id
}
