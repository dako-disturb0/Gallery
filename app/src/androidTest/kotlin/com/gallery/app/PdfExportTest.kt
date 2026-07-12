package com.gallery.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gallery.app.data.ImageFormat
import com.gallery.app.data.PdfExport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * Menguji ekspor Save As: kompresi tiap format menghasilkan byte, dan penyimpanan
 * ke MediaStore mengembalikan nama berkas (di API Q+ tanpa perlu izin).
 */
@RunWith(AndroidJUnit4::class)
class PdfExportTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun sampleBitmap(): Bitmap =
        Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }

    @Test
    fun compress_producesBytesForEveryFormat() {
        val bmp = sampleBitmap()
        for (format in ImageFormat.entries) {
            val out = ByteArrayOutputStream()
            format.compress(bmp, out)
            assertTrue("${format.label} harus menghasilkan byte", out.size() > 0)
        }
    }

    @Test
    fun saveImage_returnsFileNameWithExtension() = runBlocking {
        // Jalur pra-Q butuh WRITE_EXTERNAL_STORAGE yang tak diminta app; lewati.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

        val name = PdfExport.saveImage(context, sampleBitmap(), "instrumented_test", ImageFormat.PNG)
        assertNotNull("saveImage harus mengembalikan nama berkas", name)
        assertTrue("Nama berakhiran .png", name!!.endsWith(".png"))
    }
}
