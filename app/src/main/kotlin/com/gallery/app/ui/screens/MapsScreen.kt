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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

/** Max zoom for ESRI satellite tiles (beyond this → "data not available"). */
private const val SATELLITE_MAX_ZOOM = 18.0

class PhotoMarkerDrawable(
    private val bitmap: android.graphics.Bitmap,
    private val count: Int,
    private val isSelected: Boolean
) : android.graphics.drawable.Drawable() {
    private val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) android.graphics.Color.parseColor("#1A73E8") else android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val backgroundPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    private val badgePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        style = android.graphics.Paint.Style.FILL
    }
    private val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 24f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    override fun draw(canvas: android.graphics.Canvas) {
        val bounds = bounds
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()

        val badgeRadius = 18f
        val padding = badgeRadius
        val photoLeft = padding
        val photoTop = padding
        val photoRight = w - padding
        val photoBottom = h - padding

        val rectF = android.graphics.RectF(photoLeft, photoTop, photoRight, photoBottom)
        
        canvas.drawRect(rectF, backgroundPaint)

        val srcSize = Math.min(bitmap.width, bitmap.height)
        val srcLeft = (bitmap.width - srcSize) / 2
        val srcTop = (bitmap.height - srcSize) / 2
        val cropSrcRect = android.graphics.Rect(srcLeft, srcTop, srcLeft + srcSize, srcTop + srcSize)

        canvas.drawBitmap(bitmap, cropSrcRect, rectF, null)

        canvas.drawRect(rectF, borderPaint)

        if (count > 0) {
            val badgeX = photoRight
            val badgeY = photoTop
            
            canvas.drawCircle(badgeX, badgeY, badgeRadius, badgePaint)

            val badgeBorderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawCircle(badgeX, badgeY, badgeRadius, badgeBorderPaint)

            val textHeight = textPaint.descent() - textPaint.ascent()
            val textOffset = textHeight / 2 - textPaint.descent()
            canvas.drawText(count.toString(), badgeX, badgeY + textOffset, textPaint)
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    
    override fun getIntrinsicWidth(): Int = 120
    override fun getIntrinsicHeight(): Int = 120
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    geotaggedItems: List<GeoMedia>,
    selectedItemId: Long? = null,
    onBackClick: () -> Unit,
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

    var showBottomPanel by remember { mutableStateOf(false) }

    val groupedLocations = remember(resolvedItems) {
        resolvedItems.groupBy { String.format(Locale.US, "%.5f,%.5f", it.lat, it.lon) }
    }

    LaunchedEffect(groupedLocations, selectedStyle, selectedItem) {
        if (resolvedItems.isEmpty()) return@LaunchedEffect
        mapView.setTileSource(selectedStyle)
        mapView.overlays.clear()

        groupedLocations.forEach { (_, itemsAtLocation) ->
            val primaryItem = itemsAtLocation.firstOrNull() ?: return@forEach
            val isSelected = itemsAtLocation.any { it.id == selectedItem?.id }
            val count = itemsAtLocation.size

            val marker = Marker(mapView).apply {
                position = GeoPoint(primaryItem.lat, primaryItem.lon)
                
                val placeholderDrawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    setColor(android.graphics.Color.LTGRAY)
                    setStroke(2, android.graphics.Color.WHITE)
                    setSize(100, 100)
                }
                icon = placeholderDrawable
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                
                title = primaryItem.item.displayName
                
                setOnMarkerClickListener { _, _ ->
                    selectedItem = primaryItem
                    showBottomPanel = true
                    scope.launch {
                        val idx = resolvedItems.indexOf(primaryItem)
                        if (idx >= 0) listState.animateScrollToItem(idx)
                    }
                    true
                }
            }
            mapView.overlays.add(marker)

            scope.launch {
                val request = ImageRequest.Builder(context)
                    .data(primaryItem.item.uri)
                    .size(120)
                    .build()
                val imageLoader = coil3.SingletonImageLoader.get(context)
                val result = imageLoader.execute(request)
                if (result is coil3.request.SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is android.graphics.drawable.BitmapDrawable) {
                        val bitmap = drawable.bitmap
                        marker.icon = PhotoMarkerDrawable(bitmap, count, isSelected)
                        mapView.invalidate()
                    }
                }
            }
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
            if (idx >= 0) {
                selectedItem = resolvedItems[idx]
                showBottomPanel = true
                listState.animateScrollToItem(idx)
            }
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("MAP VIEW", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Kembali"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "MAP VIEW",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                TextButton(
                    onClick = { showBottomPanel = !showBottomPanel }
                ) {
                    Text(
                        text = "LIHAT PILIHAN",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 90.dp, start = 12.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(
                "Standard" to TileSourceFactory.MAPNIK,
                "Satelit" to esriSatellite
            ).forEach { (label, source) ->
                val isSelected = selectedStyle == source
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) MapChipSelectedBg else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            selectedStyle = source
                            if (source == esriSatellite && mapView.zoomLevelDouble > SATELLITE_MAX_ZOOM) {
                                mapView.controller.setZoom(SATELLITE_MAX_ZOOM)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MapChipSelectedText else MapChipUnselectedText
                    )
                }
            }
        }

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

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 90.dp, end = 16.dp),
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

        AnimatedVisibility(
            visible = showBottomPanel && resolvedItems.isNotEmpty(),
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(top = 12.dp, bottom = 16.dp)
            ) {
                selectedItem?.let { sel ->
                    Text(
                        text = sel.item.displayName.substringBeforeLast("."),
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
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
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
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            val date = remember(geoItem.item.dateAdded) {
                SimpleDateFormat("d MMM", Locale.forLanguageTag("id-ID"))
                    .format(Date(geoItem.item.dateAdded * 1000))
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
            .size(40.dp)
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
