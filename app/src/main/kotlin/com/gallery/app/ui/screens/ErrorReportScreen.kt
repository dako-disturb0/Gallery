package com.gallery.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.data.CrashLogger
import java.io.File

/** Merah "Guru Meditation" ala laporan galat NewPipe. */
private val ErrorRed = Color(0xFFC62828)

/**
 * Menampilkan satu laporan crash dengan gaya terstruktur ("Laporan galat"):
 * header merah, ringkasan "Yang terjadi", grid Info key→value, lalu blok Detail
 * monospace berisi stacktrace + logcat. Mengikuti pola ErrorActivity NewPipe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorReportScreen(file: File, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val report = remember(file) { CrashLogger.parseReport(file) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan galat", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        CrashLogger.shareText(context, report.raw, "Laporan galat — ${file.name}")
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Bagikan", tint = Color.White)
                    }
                    IconButton(onClick = { copyToClipboard(context, report.raw) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Salin", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ErrorRed),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Maaf, hal tersebut seharusnya tidak terjadi.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            SectionLabel("Yang terjadi:")
            Text(report.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

            if (report.info.isNotEmpty()) {
                SectionLabel("Info:")
                InfoGrid(report.info)
            }

            SectionLabel("Detail:")
            DetailBlock(report.detail)

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 24.dp, bottom = 6.dp),
    )
}

@Composable
private fun InfoGrid(info: List<Pair<String, String>>) {
    Column {
        info.forEach { (key, value) ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(112.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DetailBlock(detail: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(10.dp))
    ) {
        Text(
            text = detail,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            softWrap = false,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Laporan galat", text))
    Toast.makeText(context, "Disalin ke clipboard", Toast.LENGTH_SHORT).show()
}
