package com.gallery.app.data

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.gallery.app.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Penangkap crash & log ringan tanpa dependency eksternal.
 *
 * Dua jalur diagnosis:
 *  1. [install] memasang handler uncaught-exception yang menyimpan stacktrace +
 *     logcat ke file — menangkap crash JVM otomatis.
 *  2. [captureNow] membaca ring buffer `logcat -d` saat ini — berguna untuk crash
 *     NATIVE (mis. PdfRenderer/Pdfium) yang TIDAK memicu handler JVM; setelah app
 *     dibuka ulang jejaknya sering masih ada di buffer.
 *
 * Laporan disimpan di penyimpanan privat app (`filesDir/crash_logs`) dan bisa
 * dilihat/dibagikan lewat layar Log in-app.
 */
object CrashLogger {

    private const val DIR = "crash_logs"
    private const val MAX_FILES = 15
    private const val LOGCAT_MAX_LINES = 400

    private val FILE_STAMP = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val HUMAN_STAMP = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val report = buildReport(
                    title = "UNCAUGHT EXCEPTION",
                    thread = thread,
                    throwable = throwable,
                )
                writeReport(appContext, report)
            } catch (_: Throwable) {
                // Jangan pernah gagal di dalam handler crash.
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
    }

    /** Ambil snapshot logcat saat ini ke sebuah laporan baru. Mengembalikan file-nya. */
    fun captureNow(context: Context): File? {
        return try {
            val report = buildReport(
                title = "MANUAL LOGCAT CAPTURE",
                thread = null,
                throwable = null,
            )
            writeReport(context.applicationContext, report)
        } catch (_: Throwable) {
            null
        }
    }

    fun listReports(context: Context): List<File> {
        val dir = File(context.applicationContext.filesDir, DIR)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") } ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }
    }

    fun readReport(file: File): String = try {
        file.readText()
    } catch (_: Throwable) {
        "(gagal membaca berkas log)"
    }

    fun clearAll(context: Context) {
        listReports(context).forEach { runCatching { it.delete() } }
    }

    fun shareText(context: Context, text: String, subject: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Bagikan log"))
        }
    }

    // ── internal ──────────────────────────────────────────────────────────────

    private fun buildReport(title: String, thread: Thread?, throwable: Throwable?): String {
        return buildString {
            appendLine("=== Gallery — $title ===")
            appendLine(deviceHeader())
            if (thread != null) appendLine("Thread    : ${thread.name}")
            appendLine()
            if (throwable != null) {
                appendLine("--- STACKTRACE ---")
                appendLine(Log.getStackTraceString(throwable))
                appendLine()
            }
            appendLine("--- LOGCAT (maks $LOGCAT_MAX_LINES baris terakhir) ---")
            appendLine(readLogcat())
        }
    }

    private fun deviceHeader(): String = buildString {
        appendLine("Waktu     : ${HUMAN_STAMP.format(Date())}")
        appendLine("App       : ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})")
        appendLine("Perangkat : ${Build.MANUFACTURER} ${Build.MODEL}")
        append("Android   : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
    }

    /**
     * Baca logcat proses sendiri (tak perlu izin READ_LOGS). Best-effort.
     *
     * `-t N` membatasi ke N baris terakhir langsung di sumber, jadi buffer besar
     * tak perlu dibaca seluruhnya ke memori. Prosesnya juga di-destroy eksplisit
     * agar tak menyisakan child process.
     */
    private fun readLogcat(): String {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-v", "time", "-t", LOGCAT_MAX_LINES.toString())
            )
            val lines = process.inputStream.bufferedReader().use { it.readLines() }
            if (lines.isEmpty()) "(logcat kosong)" else lines.joinToString("\n")
        } catch (t: Throwable) {
            "(gagal membaca logcat: ${t.message})"
        } finally {
            runCatching { process?.destroy() }
        }
    }

    private fun writeReport(context: Context, text: String): File {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val file = File(dir, "crash_${FILE_STAMP.format(Date())}.txt")
        file.writeText(text)
        pruneOld(dir)
        return file
    }

    private fun pruneOld(dir: File) {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") } ?: return
        if (files.size <= MAX_FILES) return
        files.sortedByDescending { it.lastModified() }
            .drop(MAX_FILES)
            .forEach { runCatching { it.delete() } }
    }
}
