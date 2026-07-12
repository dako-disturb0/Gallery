package com.gallery.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.gallery.app.data.MediaItem
import com.gallery.app.data.MetadataReader
import com.gallery.app.ui.components.ShimmerBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class untuk item bergeotag yang sudah di-resolve koordinatnya
data class GeoItem(
    val mediaItem: MediaItem,
    val lat: Double,
    val lon: Double,
)

@Composable
fun MapsScreen(
    geotaggedItems: List<MediaItem>,
    selectedItemId: Long? = null,
    onMediaClick: (MediaItem) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Resolve koordinat dari items (sudah di-cache di ViewModel, tapi perlu lat/lon per item)
    val resolvedItems by produceState<List<GeoItem>>(initialValue = emptyList(), geotaggedItems) {
        value = withContext(Dispatchers.IO) {
            geotaggedItems.mapNotNull { item ->
                try {
                    val meta = MetadataReader.read(context, item)
                    val lat = meta.latitude ?: return@mapNotNull null
                    val lon = meta.longitude ?: return@mapNotNull null
                    GeoItem(item, lat, lon)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    val isLoading = resolvedItems.isEmpty() && geotaggedItems.isNotEmpty()
    var selectedItem by remember(selectedItemId, resolvedItems) {
        mutableStateOf(
            if (selectedItemId != null) resolvedItems.find { it.mediaItem.id == selectedItemId }
            else resolvedItems.firstOrNull()
        )
    }

    val listState = rememberLazyListState()

    // MapView
    val mapView = remember {
        MapView(context).apply {
            Configuration.getInstance().userAgentValue = context.packageName
            setBuiltInZoomControls(false)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
        }
    }

    val esriSatellite = remember {
        XYTileSource(
            "Satelit", 0, 19, 256, ".jpg",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
        )
    }
    var selectedStyle by remember {
        mutableStateOf<org.osmdroid.tileprovider.tilesource.ITileSource>(TileSourceFactory.MAPNIK)
    }

    // Tambahkan marker & navigate saat data siap
    LaunchedEffect(resolvedItems, selectedStyle) {
        if (resolvedItems.isEmpty()) return@LaunchedEffect
        mapView.setTileSource(selectedStyle)
        mapView.overlays.clear()

        // Tambah marker untuk setiap item
        resolvedItems.forEach { geoItem ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(geoItem.lat, geoItem.lon)
                val isSelected = geoItem.mediaItem.id == selectedItem?.mediaItem?.id
                val markerDrawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(if (isSelected) android.graphics.Color.parseColor("#FF3B30") else android.graphics.Color.parseColor("#007AFF"))
                    setStroke(4, android.graphics.Color.WHITE)
                    setSize(if (isSelected) 56 else 40, if (isSelected) 56 else 40)
                }
                icon = markerDrawable
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = geoItem.mediaItem.displayName
                setOnMarkerClickListener { _, _ ->
                    selectedItem = geoItem
                    scope.launch {
                        val idx = resolvedItems.indexOf(geoItem)
                        if (idx >= 0) listState.animateScrollToItem(idx)
                    }
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    // Arahkan peta ke item terpilih
    LaunchedEffect(selectedItem) {
        val sel = selectedItem ?: return@LaunchedEffect
        mapView.controller.animateTo(GeoPoint(sel.lat, sel.lon))
        mapView.controller.setZoom(16.0)
    }

    // Scroll list ke item terpilih saat pertama kali
    LaunchedEffect(selectedItemId, resolvedItems) {
        if (selectedItemId != null && resolvedItems.isNotEmpty()) {
            val idx = resolvedItems.indexOfFirst { it.mediaItem.id == selectedItemId }
            if (idx >= 0) listState.animateScrollToItem(idx)
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    if (geotaggedItems.isEmpty()) {
        // Tidak ada foto bergeotag
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Tidak Ada Foto Berlokasi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Foto dengan informasi GPS akan muncul di sini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Peta
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Indikator loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Memuat lokasi...", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Style switcher: Standard / Topo / Satelit
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 60.dp, start = 12.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(
                "Standard" to TileSourceFactory.MAPNIK,
                "Topo" to TileSourceFactory.USGS_TOPO,
                "Satelit" to esriSatellite
            ).forEach { (label, source) ->
                val isSelected = selectedStyle == source
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedStyle = source }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White
                    )
                }
            }
        }

        // Zoom controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapZoomButton(
                icon = Icons.Rounded.Add,
                contentDesc = "Zoom In",
                onClick = { mapView.controller.zoomIn() }
            )
            MapZoomButton(
                icon = Icons.Rounded.Remove,
                contentDesc = "Zoom Out",
                onClick = { mapView.controller.zoomOut() }
            )
        }

        // Jumlah foto bergeotag badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 12.dp),
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "${resolvedItems.size} Foto",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Panel bawah: horizontal scroll foto bergeotag
        AnimatedVisibility(
            visible = resolvedItems.isNotEmpty(),
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                // Label lokasi item terpilih
                selectedItem?.let { sel ->
                    Text(
                        text = sel.mediaItem.displayName.substringBeforeLast("."),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 4.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.5f, %.5f", sel.lat, sel.lon),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                    )
                }

                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = resolvedItems,
                        key = { _, item -> item.mediaItem.id }
                    ) { _, geoItem ->
                        val isSelected = geoItem.mediaItem.id == selectedItem?.mediaItem?.id
                        MapPhotoThumbnail(
                            geoItem = geoItem,
                            isSelected = isSelected,
                            onClick = {
                                selectedItem = geoItem
                            },
                            onDoubleClick = { onMediaClick(geoItem.mediaItem) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPhotoThumbnail(
    geoItem: GeoItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    val context = LocalContext.current
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) Color(0xFF007AFF) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable {
                val now = System.currentTimeMillis()
                if (now - lastClickTime < 400) {
                    onDoubleClick()
                } else {
                    onClick()
                }
                lastClickTime = now
            }
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(geoItem.mediaItem.uri)
                .crossfade(200)
                .size(144)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = geoItem.mediaItem.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { ShimmerBox(Modifier.fillMaxSize()) },
        )

        // Date overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            val date = remember(geoItem.mediaItem.dateAdded) {
                SimpleDateFormat("d MMM", Locale("id", "ID"))
                    .format(Date(geoItem.mediaItem.dateAdded * 1000))
            }
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MapZoomButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
