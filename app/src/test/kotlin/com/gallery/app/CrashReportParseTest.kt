package com.gallery.app

import com.gallery.app.data.CrashLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Menguji [CrashLogger.parseText] — logika murni string yang memecah laporan
 * menjadi judul, Info (key→value), dan Detail untuk layar "Laporan galat".
 */
class CrashReportParseTest {

    private val sample = """
        === Gallery — UNCAUGHT EXCEPTION ===
        Waktu     : 2026-07-12 19:11:31
        App       : 1.0.0 (build 1)
        Perangkat : samsung SM-J710F
        Android   : 7.1.1 (SDK 25)
        Thread    : main

        --- STACKTRACE ---
        java.lang.RuntimeException: Dummy
        	at com.gallery.app.Foo.bar(Foo.kt:42)

        --- LOGCAT (maks 400 baris terakhir) ---
        07-12 19:11:31.873 D/Foo: hello
    """.trimIndent()

    @Test
    fun parse_extractsTitle() {
        assertEquals("UNCAUGHT EXCEPTION", CrashLogger.parseText(sample).title)
    }

    @Test
    fun parse_extractsInfoPairs() {
        val info = CrashLogger.parseText(sample).info.toMap()
        assertEquals("1.0.0 (build 1)", info["App"])
        assertEquals("7.1.1 (SDK 25)", info["Android"])
        assertEquals("main", info["Thread"])
        // Nilai dengan ':' di dalamnya (waktu) tidak boleh terpotong salah.
        assertEquals("2026-07-12 19:11:31", info["Waktu"])
    }

    @Test
    fun parse_detailStartsAtFirstSection() {
        val detail = CrashLogger.parseText(sample).detail
        assertTrue(detail.startsWith("--- STACKTRACE ---"))
        assertTrue(detail.contains("RuntimeException: Dummy"))
        assertTrue(detail.contains("--- LOGCAT"))
    }

    @Test
    fun parse_titleFallback_whenMalformed() {
        assertEquals("Laporan galat", CrashLogger.parseText("garbage without markers").title)
    }
}
