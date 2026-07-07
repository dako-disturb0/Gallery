package com.gallery.app.ui.screens

import android.net.Uri
import android.text.format.Formatter
import androidx.annotation.OptIn as AndroidOptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.gallery.app.data.MediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewScreen(
    initialItemId: Long,
    albumId: String?,
    isFavorite: Boolean,
    allMediaItems: List<MediaItem>,
    favoritesList: List<MediaItem>,
    onBackClick: () -> Unit
) {
    val displayList = remember(allMediaItems, favoritesList, albumId, isFavorite) {
        when {
            albumId != null -> allMediaItems.filter { it.bucketId == albumId }
            isFavorite -> favoritesList
            else -> allMediaItems
        }
    }

    val initialIndex = remember(displayList, initialItemId) {
        val index = displayList.indexOfFirst { it.id == initialItemId }
        if (index >= 0) index else 0
    }

    if (displayList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Media tidak ditemukan", color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { displayList.size }
    )
    val scope = rememberCoroutineScope()

    var showUi by remember { mutableStateOf(true) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var currentPageZoomed by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dismissAnimJob by remember { mutableStateOf<Job?>(null) }

    val density = LocalDensity.current
    val dismissThreshold = with(density) { 150.dp.toPx() }

    val currentItem by remember {
        derivedStateOf { displayList[pagerState.currentPage] }
    }

    val backgroundAlpha by remember {
        derivedStateOf { (1f - abs(dragOffsetY) / 800f).coerceIn(0f, 1f) }
    }

    LaunchedEffect(pagerState.currentPage) {
        currentPageZoomed = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !currentPageZoomed && dragOffsetY == 0f
        ) { page ->
            val item = displayList[page]
            val isSelected = page == pagerState.currentPage

            ZoomablePage(
                item = item,
                isSelected = isSelected,
                dragOffsetY = if (page == pagerState.currentPage) dragOffsetY else 0f,
                onDragOffsetChange = { dy ->
                    if (page == pagerState.currentPage) {
                        dismissAnimJob?.cancel()
                        dragOffsetY = dy
                    }
                },
                onDragEnd = {
                    if (abs(dragOffsetY) > dismissThreshold) {
                        dismissAnimJob = scope.launch {
                            val target = if (dragOffsetY > 0) 2000f else -2000f
                            animate(dragOffsetY, target) { v, _ -> dragOffsetY = v }
                            onBackClick()
                        }
                    } else {
                        dismissAnimJob = scope.launch {
                            animate(
                                initialValue = dragOffsetY,
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.75f)
                            ) { v, _ -> dragOffsetY = v }
                        }
                    }
                },
                onTap = { showUi = !showUi },
                onZoomChanged = { isZoomed ->
                    if (page == pagerState.currentPage) {
                        currentPageZoomed = isZoomed
                        if (isZoomed) showUi = false
                    }
                }
            )
        }

        // Top bar
        AnimatedVisibility(
            visible = showUi && dragOffsetY == 0f && !currentPageZoomed,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatDate(currentItem.dateAdded),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = formatTime(currentItem.dateAdded),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = { showDetailsSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Detail",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom action bar
        AnimatedVisibility(
            visible = showUi && dragOffsetY == 0f && !currentPageZoomed,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomAction(Icons.Outlined.Share, "Bagikan") { }
                BottomAction(Icons.Outlined.Edit, "Edit") { }
                BottomAction(Icons.Outlined.Info, "Detail") { showDetailsSheet = true }
                BottomAction(Icons.Outlined.Delete, "Hapus") { }
            }
        }

        // Details bottom sheet
        if (showDetailsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDetailsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                MediaDetailsContent(item = currentItem)
            }
        }
    }
}

@Composable
private fun BottomAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun ZoomablePage(
    item: MediaItem,
    isSelected: Boolean,
    dragOffsetY: Float,
    onDragOffsetChange: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit
) {
    if (item.isVideo) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = dragOffsetY },
            contentAlignment = Alignment.Center
        ) {
            VideoPlayer(uri = item.uri, isSelected = isSelected)
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSelected) {
        if (!isSelected && scale != 1f) {
            scale = 1f
            panX = 0f
            panY = 0f
        }
    }

    val isZoomed = scale > 1.01f
    val dismissProgress = if (isZoomed) 0f else (abs(dragOffsetY) / 800f).coerceAtMost(1f)
    val dismissScaleFactor = 1f - dismissProgress * 0.15f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale * dismissScaleFactor
                scaleY = scale * dismissScaleFactor
                translationX = if (isZoomed) panX / scale else 0f
                translationY = if (isZoomed) panY / scale else dragOffsetY
            }
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
                var lastTapTime = 0L
                var tapJob: Job? = null

                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalDrag = Offset.Zero
                    var wasDragged = false
                    var isDismissDrag = false
                    var dismissDragAccum = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed }

                        if (!anyPressed) {
                            if (!wasDragged) {
                                val now = event.changes.firstOrNull()?.uptimeMillis ?: 0L
                                if (lastTapTime != 0L && now - lastTapTime < doubleTapTimeout) {
                                    tapJob?.cancel()
                                    lastTapTime = 0L
                                    scope.launch {
                                        if (scale > 1.1f) {
                                            animate(scale, 1f) { v, _ -> scale = v }
                                            panX = 0f
                                            panY = 0f
                                            onZoomChanged(false)
                                        } else {
                                            animate(scale, 2.5f) { v, _ -> scale = v }
                                            onZoomChanged(true)
                                        }
                                    }
                                } else {
                                    lastTapTime = now
                                    tapJob?.cancel()
                                    tapJob = scope.launch {
                                        delay(doubleTapTimeout)
                                        onTap()
                                    }
                                }
                            } else if (isDismissDrag) {
                                onDragEnd()
                            } else if (scale <= 1.01f) {
                                scale = 1f
                                panX = 0f
                                panY = 0f
                                onZoomChanged(false)
                            }
                            break
                        }

                        val pointerCount = event.changes.count { it.pressed }
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()

                        if (!wasDragged) {
                            totalDrag += pan
                            if (totalDrag.getDistance() > touchSlop || pointerCount >= 2) {
                                wasDragged = true
                                if (scale <= 1.01f && pointerCount < 2 &&
                                    abs(totalDrag.x) > abs(totalDrag.y)
                                ) {
                                    break
                                }
                                if (scale <= 1.01f && pointerCount < 2) {
                                    isDismissDrag = true
                                }
                            }
                        }

                        if (wasDragged) {
                            if (isDismissDrag) {
                                dismissDragAccum += pan.y
                                onDragOffsetChange(dismissDragAccum)
                                event.changes.forEach { it.consume() }
                            } else {
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                if (scale > 1.01f) {
                                    val maxPanX = size.width * (scale - 1) / 2f
                                    val maxPanY = size.height * (scale - 1) / 2f
                                    panX = (panX + pan.x).coerceIn(-maxPanX, maxPanX)
                                    panY = (panY + pan.y).coerceIn(-maxPanY, maxPanY)
                                } else {
                                    panX = 0f
                                    panY = 0f
                                }
                                onZoomChanged(scale > 1.01f)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@AndroidOptIn(UnstableApi::class)
@Composable
fun VideoPlayer(uri: Uri, isSelected: Boolean) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3MediaItem.fromUri(uri))
            prepare()
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MediaDetailsContent(item: MediaItem) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = "${formatDate(item.dateAdded)}  •  ${formatTime(item.dateAdded)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tambahkan teks...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Detail",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${Formatter.formatFileSize(context, item.size)}  •  ${item.mimeType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.bucketName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.bucketName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.isVideo && item.duration > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Durasi: ${formatDuration(item.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun formatDate(epochSeconds: Long): String {
    val date = Date(epochSeconds * 1000)
    return SimpleDateFormat("EEE, d MMM yyyy", Locale("id", "ID")).format(date)
}

private fun formatTime(epochSeconds: Long): String {
    val date = Date(epochSeconds * 1000)
    return SimpleDateFormat("HH.mm", Locale("id", "ID")).format(date)
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = durationMs / (1000 * 60 * 60)
    return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
