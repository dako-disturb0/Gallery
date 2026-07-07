package com.gallery.app.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

class MediaRepository(private val context: Context) {

    fun getAllMedia(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        items.addAll(queryMedia(isVideo = false))
        items.addAll(queryMedia(isVideo = true))
        items.sortByDescending { it.dateAdded }
        return items
    }

    private fun queryMedia(isVideo: Boolean): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.BUCKET_ID)
            add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.MediaColumns.IS_FAVORITE)
            }
            if (isVideo) {
                add(MediaStore.MediaColumns.DURATION)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val favCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
            } else -1
            val durationCol = if (isVideo) {
                cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                items.add(
                    MediaItem(
                        id = id,
                        uri = uri,
                        displayName = cursor.getString(nameCol) ?: "",
                        dateAdded = cursor.getLong(dateCol),
                        size = cursor.getLong(sizeCol),
                        mimeType = cursor.getString(mimeCol) ?: "",
                        bucketId = cursor.getString(bucketIdCol),
                        bucketName = cursor.getString(bucketNameCol),
                        isFavorite = if (favCol >= 0) cursor.getInt(favCol) == 1 else false,
                        duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0,
                    )
                )
            }
        }
        return items
    }
}
