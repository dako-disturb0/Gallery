package com.gallery.app.data

import android.net.Uri

/**
 * Sebuah dokumen PDF di perangkat. Berbeda dari [MediaItem] karena PDF bukan
 * media visual (foto/video) dan disimpan lewat [android.provider.MediaStore.Files].
 */
data class PdfItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val size: Long,
    val dateModified: Long,
)
