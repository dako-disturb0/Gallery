package com.gallery.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns

/**
 * Menemukan dokumen PDF di perangkat melalui [MediaStore.Files].
 *
 * Catatan: pada Android 10+ (scoped storage) kueri ini hanya mengembalikan PDF
 * yang terlihat oleh aplikasi (mis. dibuat aplikasi ini atau di folder publik
 * yang terindeks). Untuk membuka PDF apa pun secara andal, UI menyediakan jalur
 * Storage Access Framework (buka dari file) yang tidak butuh izin tambahan.
 */
class PdfRepository(private val context: Context) {

    fun getAllPdfs(): List<PdfItem> {
        val items = mutableListOf<PdfItem>()
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
        )
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->
                    val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                    val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)

                    if (idCol < 0) return items

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val uri = ContentUris.withAppendedId(collection, id)
                        items.add(
                            PdfItem(
                                id = id,
                                uri = uri,
                                displayName = if (nameCol >= 0) cursor.getString(nameCol) ?: "Dokumen.pdf" else "Dokumen.pdf",
                                size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                                dateModified = if (dateCol >= 0) cursor.getLong(dateCol) else 0L,
                            )
                        )
                    }
                }
        } catch (_: Exception) {
            // Akses ditolak atau kueri gagal — kembalikan apa yang sudah terkumpul.
        }
        return items
    }

    /** Nama tampilan dari sebuah URI (mis. hasil Storage Access Framework). */
    fun resolveDisplayName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameCol >= 0 && cursor.moveToFirst()) cursor.getString(nameCol) else null
                } ?: uri.lastPathSegment ?: "Dokumen"
        } catch (_: Exception) {
            uri.lastPathSegment ?: "Dokumen"
        }
    }
}
