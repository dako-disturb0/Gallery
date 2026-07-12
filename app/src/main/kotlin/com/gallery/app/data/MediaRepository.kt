package com.gallery.app.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * Hasil permintaan hapus. Di Android 11+ (dan Android 10 untuk media milik app
 * lain) sistem meminta konfirmasi lewat [NeedsConsent]. Di Android 10 ke bawah
 * media dihapus langsung ([Deleted]).
 */
sealed interface DeleteResult {
    /** Media sudah dihapus langsung (Android 10 ke bawah). */
    data object Deleted : DeleteResult
    /** Perlu konfirmasi user via IntentSender (Android 11+ / RecoverableSecurityException). */
    data class NeedsConsent(val intentSender: IntentSender) : DeleteResult
    /** Gagal menghapus. */
    data object Failed : DeleteResult
}

class MediaRepository(private val context: Context) {

    fun deleteRequest(uris: List<Uri>): DeleteResult {
        if (uris.isEmpty()) return DeleteResult.Failed
        val resolver = context.contentResolver
        return when {
            // Android 11+ : sistem menangani penghapusan setelah user setuju.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                try {
                    val sender = MediaStore.createDeleteRequest(resolver, uris).intentSender
                    DeleteResult.NeedsConsent(sender)
                } catch (e: Exception) {
                    DeleteResult.Failed
                }
            }
            // Android 10 : coba hapus langsung; jika bukan milik app → minta izin.
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                try {
                    uris.forEach { resolver.delete(it, null, null) }
                    DeleteResult.Deleted
                } catch (e: RecoverableSecurityException) {
                    DeleteResult.NeedsConsent(e.userAction.actionIntent.intentSender)
                } catch (e: Exception) {
                    DeleteResult.Failed
                }
            }
            // Android 7.1–9 : hapus langsung (butuh WRITE_EXTERNAL_STORAGE).
            else -> {
                try {
                    uris.forEach { resolver.delete(it, null, null) }
                    DeleteResult.Deleted
                } catch (e: Exception) {
                    DeleteResult.Failed
                }
            }
        }
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
