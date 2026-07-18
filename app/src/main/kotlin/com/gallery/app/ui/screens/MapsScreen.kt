package com.gallery.app.ui.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.gallery.app.data.GeoMedia
import com.gallery.app.data.MediaItem
import com.gallery.app.ui.components.ShimmerBox
import com.gallery.app.ui.map.EsriSatelliteTileSource
import com.gallery.app.ui.theme.MapChipSelectedBg
import com.gallery.app.ui.theme.MapChipSelectedText
import com.gallery.app.ui.theme.MapChipUnselectedBg
import com.gallery.app.ui.theme.MapChipUnselectedText
import com.gallery.app.ui.theme.MarkerDefault
import com.gallery.app.ui.theme.MarkerSelected
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SATELLITE_MAX_ZOOM = 18.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    geotaggedItems: List<GeoMedia>,
    selectedItemId: Long? = null,
    onMediaClick: (MediaItem) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val resolvedItems = geotaggedItems

    var selectedItem by remember(selectedItemId, resolvedItems) {
        mutableStateOf(
            if (selectedItemId != null) resolvedItems.find { it.id == selectedItemId }
            else resolvedItems.firstOrNull()
        )
    }

    val listState = rememberLazyListState()

    val mapView = remember {
        MapView(context).apply {
            Configuration.getInstance().userAgentValue = context.packageName
            setBuiltInZoomControls(false)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
        }
    }

    val esriSatellite = remember { EsriSatelliteTileSource }
    var selectedStyle by remember {
        mutableStateOf<org.osmdroid.tileprovider.tilesource.ITileSource>(TileSourceFactory.MAPNIK)
    }
    val isSatellite = selectedStyle == esriSatellite

    LaunchedEffect(resolvedItems, selectedStyle, selectedItem) {
        if (resolvedItems.isEmpty()) return@LaunchedEffect
        mapView.setTileSource(selectedStyle)
        mapView.overlays.clear()

        resolvedItems.forEach { geoItem ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(geoItem.lat, geoItem.lon)
                val isSelected = geoItem.id == selectedItem?.id
                val markerDrawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    val c = if (isSelected) MarkerSelected else MarkerDefault
                    setColor(android.graphics.Color.argb(
                        (c.alpha * 255).toInt(),
                        (c.red * 255).toInt(),
                        (c.green * 255).toInt(),
                        (c.blue * 255).toInt()
                    ))
                    setStroke(4, android.graphics.Color.WHITE)
                    setSize(if (isSelected) 56 else 40, if (isSelected) 56 else 40)
                }
                icon = markerDrawable
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = geoItem.item.displayName
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

    LaunchedEffect(isSatellite) {
        if (isSatellite && mapView.zoomLevelDouble > SATELLITE_MAX_ZOOM) {
            mapView.controller.setZoom(SATELLITE_MAX_ZOOM)
        }
    }

    LaunchedEffect(selectedItem) {
        val sel = selectedItem ?: return@LaunchedEffect
        mapView.controller.animateTo(GeoPoint(sel.lat, sel.lon))
        val targetZoom = if (isSatellite) SATELLITE_MAX_ZOOM.coerceAtMost(16.0) else 16.0
        mapView.controller.setZoom(targetZoom)
    }

    LaunchedEffect(selectedItemId, resolvedItems) {
        if (selectedItemId != null && resolvedItems.isNotEmpty()) {
            val idx = resolvedItems.indexOfFirst { it.id == selectedItemId }
            if (idx >= 0) listState.animateScrollToItem(idx)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        mapView.onResume()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    if (geotaggedItems.isEmpty()) {
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
        // Map
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Style switcher — top left
        MapStyleSwitcher(
            selectedStyle = selectedStyle,
            onStyleSelected = { source ->
                selectedStyle = source
                if (source == esriSatellite && mapView.zoomLevelDouble > SATELLITE_MAX_ZOOM) {
                    mapView.controller.setZoom(SATELLITE_MAX_ZOOM)
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 12.dp, start = 12.dp)
        )

        // Zoom controls — right center
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MapZoomButton(
                icon = Icons.Rounded.Add,
                contentDesc = "Zoom In",
                onClick = {
                    val newZoom = mapView.zoomLevelDouble + 1.0
                    if (isSatellite && newZoom > SATELLITE_MAX_ZOOM) {
                        mapView.controller.setZoom(SATELLITE_MAX_ZOOM)
                    } else {
                        mapView.controller.zoomIn()
                    }
                }
            )
            MapZoomButton(
                icon = Icons.Rounded.Remove,
                contentDesc = "Zoom Out",
                onClick = { mapView.controller.zoomOut() }
            )
        }

        // Photo count badge — top right
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "${resolvedItems.size} Foto",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Bottom panel: horizontal scroll
        AnimatedVisibility(
            visible = resolvedItems.isNotEmpty(),
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(top = 12.dp, bottom = 8.dp)
                ) {
                    selectedItem?.let { sel ->
                        Text(
                            text = sel.item.displayName.substringBeforeLast("."),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 2.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.5f, %.5f", sel.lat, sel.lon),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            key = { _, item -> item.id }
                        ) { _, geoItem ->
                            val isSelected = geoItem.id == selectedItem?.id
                            MapPhotoThumbnail(
                                geoItem = geoItem,
                                isSelected = isSelected,
                                onClick = { selectedItem = geoItem },
                                onDoubleClick = { onMediaClick(geoItem.item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapStyleSwitcher(
    selectedStyle: org.osmdroid.tileprovider.tilesource.ITileSource,
    onStyleSelected: (org.osmdroid.tileprovider.tilesource.ITileSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val esriSatellite = remember { EsriSatelliteTileSource }
    val styles = listOf(
        "Standard" to TileSourceFactory.MAPNIK,
        "Satelit" to esriSatellite
    )

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            styles.forEach { (label, source) ->
                val isSelected = selectedStyle == source
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onStyleSelected(source) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MapPhotoThumbnail(
    geoItem: GeoMedia,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    val context = LocalContext.current
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable {
                val now = System.currentTimeMillis()
                if (now - lastClickTime < 400) onDoubleClick() else onClick()
                lastClickTime = now
            }
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(geoItem.item.uri)
                .crossfade(200)
                .size(144)
                .build(),
            contentDescription = geoItem.item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { ShimmerBox(Modifier.fillMaxSize()) },
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                    RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            val date = remember(geoItem.item.dateAdded) {
                SimpleDateFormat("d MMM", Locale.forLanguageTag("id-ID"))
                    .format(Date(geoItem.item.dateAdded * 1000))
            }
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
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
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
