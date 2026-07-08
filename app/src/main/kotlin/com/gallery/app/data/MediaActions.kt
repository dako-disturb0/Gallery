package com.gallery.app.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object MediaActions {

    fun share(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType.ifEmpty { if (item.isVideo) "video/*" else "image/*" }
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launch(context, Intent.createChooser(intent, "Bagikan via"), "Tidak ada aplikasi untuk berbagi")
    }

    fun edit(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(item.uri, item.mimeType.ifEmpty { "image/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        launch(context, Intent.createChooser(intent, "Edit dengan"), "Tidak ada aplikasi editor")
    }

    fun openWith(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(item.uri, item.mimeType.ifEmpty { if (item.isVideo) "video/*" else "image/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launch(context, Intent.createChooser(intent, "Buka dengan"), "Tidak ada aplikasi")
    }

    fun useAs(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
            setDataAndType(item.uri, item.mimeType.ifEmpty { "image/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("mimeType", item.mimeType.ifEmpty { "image/*" })
        }
        launch(context, Intent.createChooser(intent, "Jadikan sebagai"), "Tidak ada aplikasi")
    }

    fun openLocation(context: Context, latitude: Double, longitude: Double, label: String) {
        val encoded = Uri.encode(label)
        val geo = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encoded)")
        val intent = Intent(Intent.ACTION_VIEW, geo)
        launch(context, intent, "Tidak ada aplikasi peta")
    }

    private fun launch(context: Context, intent: Intent, error: String) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }
}
