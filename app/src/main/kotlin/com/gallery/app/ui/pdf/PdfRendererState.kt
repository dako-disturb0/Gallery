package com.gallery.app.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Pembungkus aman untuk [PdfRenderer] bawaan Android.
 *
 * [PdfRenderer] tidak thread-safe dan hanya boleh membuka satu halaman pada satu
 * waktu, jadi semua akses diserialkan lewat [mutex]. Setiap [renderPage] membuka
 * dan menutup halamannya sendiri di dalam kunci sehingga tidak pernah ada dua
 * halaman terbuka bersamaan. [close] juga mengambil kunci agar tidak menutup
 * renderer saat sebuah render masih berjalan (mencegah crash native).
 */
class PdfRendererState private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) {
    val pageCount: Int get() = renderer.pageCount

    private val mutex = Mutex()
    @Volatile private var closed = false

    /**
     * Render halaman [index] menjadi bitmap selebar [targetWidth] piksel, dengan
     * tinggi mengikuti rasio aspek asli halaman. Mengembalikan null jika gagal
     * atau renderer sudah ditutup.
     */
    suspend fun renderPage(index: Int, targetWidth: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (closed || targetWidth <= 0 || index < 0 || index >= renderer.pageCount) return@withContext null
        mutex.withLock {
            if (closed) return@withLock null
            try {
                renderer.openPage(index).use { page ->
                    val ratio = page.height.toFloat() / page.width.toFloat()
                    val height = (targetWidth * ratio).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                    // Latar putih — PDF transparan akan tampil hitam tanpa ini.
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /** Menutup renderer & file descriptor. Menunggu render yang sedang berjalan. */
    fun close() {
        closed = true
        runBlocking {
            mutex.withLock {
                runCatching { renderer.close() }
                runCatching { descriptor.close() }
            }
        }
    }

    companion object {
        /** Membuka PDF dari [uri]. Mengembalikan null jika tidak bisa dibuka/rusak. */
        fun open(context: Context, uri: Uri): PdfRendererState? {
            return try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
                try {
                    PdfRendererState(pfd, PdfRenderer(pfd))
                } catch (e: Exception) {
                    runCatching { pfd.close() }
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
