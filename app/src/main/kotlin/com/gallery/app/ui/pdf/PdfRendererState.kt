package com.gallery.app.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Pembungkus aman untuk [PdfRenderer] bawaan Android.
 *
 * [PdfRenderer] tidak thread-safe dan hanya boleh membuka satu halaman pada satu
 * waktu. Semua akses (render maupun close) diserialkan lewat satu executor
 * single-thread, sehingga penutupan renderer selalu terjadi SETELAH render yang
 * sedang berjalan selesai — tanpa memblokir main thread dan tanpa crash native.
 */
class PdfRendererState private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) {
    val pageCount: Int get() = if (closed) 0 else renderer.pageCount

    private val executor = Executors.newSingleThreadExecutor()
    private val dispatcher = executor.asCoroutineDispatcher()
    @Volatile private var closed = false

    /**
     * Render halaman [index] menjadi bitmap selebar [targetWidth] piksel (dibatasi
     * agar tidak OOM), tinggi mengikuti rasio aspek asli halaman. Mengembalikan
     * null jika gagal / renderer sudah ditutup / kehabisan memori.
     */
    suspend fun renderPage(index: Int, targetWidth: Int): Bitmap? {
        if (closed || targetWidth <= 0) return null
        return try {
            withContext(dispatcher) {
                if (closed || index < 0 || index >= renderer.pageCount) return@withContext null
                try {
                    renderer.openPage(index).use { page ->
                        val ratio = page.height.toFloat() / page.width.toFloat()

                        // Batasi dimensi agar alokasi bitmap tetap aman.
                        var width = targetWidth.coerceAtMost(MAX_DIMENSION)
                        var height = (width * ratio).toInt().coerceAtLeast(1)
                        if (height > MAX_DIMENSION) {
                            val shrink = MAX_DIMENSION / height.toFloat()
                            height = MAX_DIMENSION
                            width = (width * shrink).toInt().coerceAtLeast(1)
                        }

                        // createBitmap bisa melempar OutOfMemoryError (Error, bukan
                        // Exception), jadi tangkap Throwable di sini.
                        val bitmap = try {
                            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        } catch (t: Throwable) {
                            null
                        } ?: return@use null

                        bitmap.eraseColor(Color.WHITE) // PDF transparan → hitam tanpa ini
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                } catch (_: Throwable) {
                    null
                }
            }
        } catch (_: Throwable) {
            // Mis. RejectedExecutionException bila executor sudah shutdown.
            null
        }
    }

    /**
     * Menutup renderer & file descriptor. Non-blocking: tugas penutupan
     * dijadwalkan di executor yang sama sehingga berjalan setelah render terakhir.
     */
    fun close() {
        if (closed) return
        closed = true
        try {
            executor.execute {
                runCatching { renderer.close() }
                runCatching { descriptor.close() }
            }
        } catch (_: Throwable) {
            runCatching { renderer.close() }
            runCatching { descriptor.close() }
        } finally {
            executor.shutdown()
        }
    }

    companion object {
        /** Batas dimensi bitmap (px) untuk mencegah OOM pada halaman ekstrem. */
        private const val MAX_DIMENSION = 2600

        /** Membuka PDF dari [uri]. Mengembalikan null jika tidak bisa dibuka/rusak. */
        fun open(context: Context, uri: Uri): PdfRendererState? {
            return try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
                try {
                    PdfRendererState(pfd, PdfRenderer(pfd))
                } catch (_: Throwable) {
                    runCatching { pfd.close() }
                    null
                }
            } catch (_: Throwable) {
                null
            }
        }
    }
}
