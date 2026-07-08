package com.gallery.app.data

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

class MediaRepository(private val context: Context) {

    fun deleteRequest(uris: List<Uri>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
    }

    fun favoriteRequest(uris: List<Uri>, favorite: Boolean): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return MediaStore.createFavoriteRequest(context.contentResolver, uris, favorite).intentSender
    }

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
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.MediaColumns.IS_FAVORITE)
            }
            if (isVideo) {
                add(MediaStore.MediaColumns.DURATION)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val favCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
                } else -1
                val durationCol = if (isVideo) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                } else -1

                if (idCol < 0) return items

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            displayName = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else "",
                            dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L,
                            size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                            mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else "",
                            bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) else null,
                            bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) else null,
                            isFavorite = if (favCol >= 0) cursor.getInt(favCol) == 1 else false,
                            duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0,
                            width = if (widthCol >= 0) cursor.getInt(widthCol) else 0,
                            height = if (heightCol >= 0) cursor.getInt(heightCol) else 0,
                        )
                    )
                }
            }
        } catch (e: Exception) {
        }
        return items
    }
}
