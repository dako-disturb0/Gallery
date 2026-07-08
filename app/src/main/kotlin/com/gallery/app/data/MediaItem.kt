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
