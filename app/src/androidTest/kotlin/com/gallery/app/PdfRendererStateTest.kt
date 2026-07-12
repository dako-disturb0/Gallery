package com.gallery.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gallery.app.ui.pdf.PdfRendererState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Menguji [PdfRendererState] terhadap PdfRenderer native sungguhan — jalur yang
 * dulu crash. Membuka PDF 3 halaman, render, cek batas indeks, dan close ganda.
 */
@RunWith(AndroidJUnit4::class)
class PdfRendererStateTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var pdf: File

    @Before
    fun setUp() {
        pdf = File.createTempFile("sample", ".pdf", context.cacheDir)
        writeSamplePdf(pdf, pages = 3)
    }

    @After
    fun tearDown() {
        pdf.delete()
    }

    @Test
    fun open_reportsCorrectPageCount() {
        val state = PdfRendererState.open(context, android.net.Uri.fromFile(pdf))
        assertNotNull("Renderer seharusnya terbuka", state)
        assertEquals(3, state!!.pageCount)
        state.close()
    }

    @Test
    fun renderPage_returnsBitmapWithRequestedWidth() = runBlocking {
        val state = PdfRendererState.open(context, android.net.Uri.fromFile(pdf))!!
        val bmp = state.renderPage(index = 0, targetWidth = 400)
        assertNotNull("Bitmap halaman 0 tidak boleh null", bmp)
        assertEquals(400, bmp!!.width)
        // Tinggi mengikuti rasio A4 (842/595 ≈ 1.415).
        assertTrue("Tinggi harus proporsional", bmp.height in 540..580)
        state.close()
    }

    @Test
    fun renderPage_outOfRange_returnsNull() = runBlocking {
        val state = PdfRendererState.open(context, android.net.Uri.fromFile(pdf))!!
        assertNull(state.renderPage(index = 99, targetWidth = 400))
        assertNull(state.renderPage(index = -1, targetWidth = 400))
        state.close()
    }

    @Test
    fun renderPage_afterClose_returnsNull() = runBlocking {
        val state = PdfRendererState.open(context, android.net.Uri.fromFile(pdf))!!
        state.close()
        assertNull("Render setelah close harus null", state.renderPage(0, 400))
    }

    @Test
    fun close_isIdempotent() {
        val state = PdfRendererState.open(context, android.net.Uri.fromFile(pdf))!!
        state.close()
        state.close() // tidak boleh crash / lempar
    }

    @Test
    fun open_corruptFile_returnsNull() {
        val bad = File.createTempFile("bad", ".pdf", context.cacheDir)
        bad.writeText("ini bukan pdf")
        val state = PdfRendererState.open(context, android.net.Uri.fromFile(bad))
        assertNull("PDF rusak harus mengembalikan null, bukan crash", state)
        bad.delete()
    }
}
