package com.gallery.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gallery.app.data.CrashLogger
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Menguji [CrashLogger]: capture manual menulis laporan yang bisa dibaca, daftar
 * bertambah, dan clearAll benar-benar mengosongkan. Menyentuh filesDir + exec
 * logcat sungguhan di perangkat.
 */
@RunWith(AndroidJUnit4::class)
class CrashLoggerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun clean() = CrashLogger.clearAll(context)

    @After
    fun cleanup() = CrashLogger.clearAll(context)

    @Test
    fun captureNow_writesReadableReport() {
        val before = CrashLogger.listReports(context).size
        val file = CrashLogger.captureNow(context)
        assertNotNull("captureNow harus menghasilkan file", file)

        val reports = CrashLogger.listReports(context)
        assertTrue("Daftar laporan harus bertambah", reports.size >= before + 1)

        val text = CrashLogger.readReport(file!!)
        assertTrue("Laporan memuat header aplikasi", text.contains("Gallery"))
        assertTrue("Laporan memuat bagian logcat", text.contains("LOGCAT"))
    }

    @Test
    fun clearAll_removesEverything() {
        CrashLogger.captureNow(context)
        assertTrue(CrashLogger.listReports(context).isNotEmpty())
        CrashLogger.clearAll(context)
        assertTrue(
            "Setelah clearAll daftar harus kosong",
            CrashLogger.listReports(context).isEmpty(),
        )
    }
}
