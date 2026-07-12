package com.gallery.app

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File

/**
 * Membuat PDF valid berisi [pages] halaman A4 di runtime menggunakan
 * [PdfDocument] bawaan Android — lalu dibaca kembali oleh [PdfRenderer] pada test.
 * Ini menguji round-trip render sungguhan tanpa perlu membundel aset.
 */
fun writeSamplePdf(file: File, pages: Int) {
    val doc = PdfDocument()
    try {
        repeat(pages) { i ->
            val info = PdfDocument.PageInfo.Builder(595, 842, i + 1).create()
            val page = doc.startPage(info)
            page.canvas.apply {
                drawColor(Color.WHITE)
                drawText(
                    "Halaman ${i + 1}",
                    80f,
                    120f,
                    Paint().apply { color = Color.BLACK; textSize = 42f },
                )
            }
            doc.finishPage(page)
        }
        file.outputStream().use { doc.writeTo(it) }
    } finally {
        doc.close()
    }
}
