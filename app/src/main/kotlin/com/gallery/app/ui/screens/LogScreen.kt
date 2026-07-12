package com.gallery.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gallery.app.BuildConfig
import com.gallery.app.data.CrashLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current

    // reloadTick memaksa daftar dibaca ulang setelah capture/hapus.
    var reloadTick by remember { mutableIntStateOf(0) }
    val reports by remember(reloadTick) { mutableStateOf(CrashLogger.listReports(context)) }
    var selected by remember(reloadTick) { mutableStateOf<File?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selected == null) "Log & Diagnostik" else "Detail Log") },
                navigationIcon = {
                    IconButton(onClick = { if (selected != null) selected = null else onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                )
            )
        }
    ) { innerPadding ->
        val current = selected
        if (current != null) {
            LogDetail(
                file = current,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LogList(
                context = context,
                reports = reports,
                onOpen = { selected = it },
                onChanged = { reloadTick++ },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
private fun LogList(
    context: Context,
    reports: List<File>,
    onOpen: (File) -> Unit,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Catatan crash tersimpan otomatis. Untuk crash native (mis. PDF), " +
                    "buka lagi aplikasi lalu tekan \"Ambil logcat\" — jejaknya sering masih " +
                    "ada di buffer sistem.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {
                        val file = CrashLogger.captureNow(context)
                        Toast.makeText(
                            context,
                            if (file != null) "Logcat tersimpan" else "Gagal mengambil logcat",
                            Toast.LENGTH_SHORT
                        ).show()
                        onChanged()
                    },
                    label = { Text("Ambil logcat") },
                    leadingIcon = { Icon(Icons.Outlined.SaveAlt, null, Modifier.size(18.dp)) }
                )
                if (reports.isNotEmpty()) {
                    AssistChip(
                        onClick = {
                            CrashLogger.clearAll(context)
                            Toast.makeText(context, "Log dihapus", Toast.LENGTH_SHORT).show()
                            onChanged()
                        },
                        label = { Text("Hapus semua") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.error,
                            leadingIconContentColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }

        if (BuildConfig.DEBUG) {
            item {
                AssistChip(
                    onClick = { throw RuntimeException("Uji crash dari Log & Diagnostik") },
                    label = { Text("Uji crash (debug)") },
                    leadingIcon = { Icon(Icons.Outlined.BugReport, null, Modifier.size(18.dp)) }
                )
            }
        }

        if (reports.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "Belum ada catatan crash",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = "Bagus — berarti belum ada kejadian yang tercatat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp)
                    )
                }
            }
        } else {
            items(items = reports, key = { it.name }) { file ->
                LogRow(file = file, onClick = { onOpen(file) })
            }
        }
    }
}

private val ROW_STAMP = SimpleDateFormat("d MMM yyyy, HH:mm:ss", Locale.forLanguageTag("id-ID"))

@Composable
private fun LogRow(file: File, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.BugReport,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ROW_STAMP.format(Date(file.lastModified())),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LogDetail(file: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val text = remember(file) { CrashLogger.readReport(file) }
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { CrashLogger.shareText(context, text, "Log Gallery — ${file.name}") },
                label = { Text("Bagikan") },
                leadingIcon = { Icon(Icons.Outlined.Share, null, Modifier.size(18.dp)) }
            )
            AssistChip(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Log Gallery", text))
                    Toast.makeText(context, "Disalin ke clipboard", Toast.LENGTH_SHORT).show()
                },
                label = { Text("Salin") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp)) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                softWrap = false,
                modifier = Modifier
                    .verticalScroll(vScroll)
                    .horizontalScroll(hScroll)
                    .padding(12.dp)
            )
        }
    }
}
