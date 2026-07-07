package com.gallery.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gallery.app.viewmodel.DateGrouping
import com.gallery.app.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentGrouping: DateGrouping,
    onGroupingChange: (DateGrouping) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Setelan") },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Bagian Pemisahan Tanggal
            Text(
                text = "Tampilan & Pengelompokan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pemisahan Foto Berdasarkan Tanggal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Pilih bagaimana foto dikelompokkan di halaman utama",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    DateGrouping.entries.forEach { grouping ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onGroupingChange(grouping) }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            RadioButton(
                                selected = (currentGrouping == grouping),
                                onClick = { onGroupingChange(grouping) }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = when (grouping) {
                                    DateGrouping.DAILY -> "Harian (Hari)"
                                    DateGrouping.WEEKLY -> "Mingguan (Minggu)"
                                    DateGrouping.MONTHLY -> "Bulanan (Bulan)"
                                    DateGrouping.YEARLY -> "Tahunan (Tahun)"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (currentGrouping == grouping) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Bagian Izin Aplikasi & Informasi Teknis
            Text(
                text = "Izin & Koneksi Sistem",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Izin Penyimpanan
                    val hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        checkPermission(context, Manifest.permission.READ_MEDIA_IMAGES) &&
                                checkPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
                    } else {
                        checkPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    PermissionStatusItem(
                        icon = Icons.Rounded.Storage,
                        title = "Read Contents of Shared Storage",
                        description = "Izin membaca file multimedia di penyimpanan bersama.",
                        isGranted = hasStoragePermission
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                    // Izin Lokasi Media
                    val hasLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        checkPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION)
                    } else true
                    PermissionStatusItem(
                        icon = Icons.Rounded.LocationOn,
                        title = "Read Locations from Media",
                        description = "Izin membaca informasi geografis (lokasi) dari foto/video.",
                        isGranted = hasLocationPermission
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                    // Izin Internet
                    val hasNetworkPermission = checkPermission(context, Manifest.permission.INTERNET)
                    PermissionStatusItem(
                        icon = Icons.Rounded.Info,
                        title = "Have Network Access",
                        description = "Deklarasi akses jaringan (Internet) untuk rendering web/data.",
                        isGranted = hasNetworkPermission
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                    // Status Koneksi Jaringan
                    val isConnected = checkNetworkConnected(context)
                    PermissionStatusItem(
                        icon = Icons.Rounded.NetworkCheck,
                        title = "View Network Connections",
                        description = "Status koneksi internet aktif perangkat Anda saat ini.",
                        isGranted = isConnected,
                        trueText = "Terhubung",
                        falseText = "Terputus"
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionStatusItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    trueText: String = "Diizinkan",
    falseText: String = "Belum Diizinkan"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (isGranted) trueText else falseText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
        }
    }
}

private fun checkPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun checkNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNetwork = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
