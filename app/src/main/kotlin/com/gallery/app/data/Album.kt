package com.gallery.app.data

import android.net.Uri

data class Album(
    val id: String,
    val name: String,
    val coverUri: Uri,
    val itemCount: Int,
)
