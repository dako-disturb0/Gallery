package com.gallery.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.content.IntentSender
import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.gallery.app.data.MediaActions
import com.gallery.app.data.MediaItem
import com.gallery.app.data.MediaMetadata
import com.gallery.app.data.MetadataReader
import com.gallery.app.data.MetadataSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    onBackClick: () -> Unit,
    onFavoriteRequest: (List<Uri>, Boolean) -> IntentSender?,
    onDeleteRequest: (List<Uri>) -> IntentSender?
) {
    val context = LocalContext.current

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

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { displayList.size }
    )
    val scope = rememberCoroutineScope()

    var showUi by remember { mutableStateOf(true) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var currentPageZoomed by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dismissAnimJob by remember { mutableStateOf<Job?>(null) }

    val activePlayers = remember { mutableStateMapOf<Int, ExoPlayer>() }
    val activePlayer by remember {
        derivedStateOf { activePlayers[pagerState.currentPage] }
    }

    val density = LocalDensity.current
    val dismissThreshold = with(density) { 150.dp.toPx() }

    val currentItem = displayList.getOrNull(pagerState.currentPage)

    val backgroundAlpha by remember {
        derivedStateOf { (1f - abs(dragOffsetY) / 800f).coerceIn(0f, 1f) }
    }

    val favoriteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {}
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {}

    val favoriteSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun toggleFavorite(target: MediaItem) {
        val sender = onFavoriteRequest(listOf(target.uri), !target.isFavorite) ?: return
        favoriteLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }

    fun requestDelete(target: MediaItem) {
        val sender = onDeleteRequest(listOf(target.uri))
        if (sender != null) {
            deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
        } else {
            Toast.makeText(context, "Menghapus butuh Android 11+", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        currentPageZoomed = false
    }

    LaunchedEffect(displayList.isEmpty()) {
        if (displayList.isEmpty()) onBackClick()
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
                },
                onPlayerActive = { player ->
                    activePlayers[page] = player
                },
                onPlayerInactive = { player ->
                    if (activePlayers[page] == player) {
                        activePlayers.remove(page)
                    }
                }
            )
        }

        val topItem = currentItem

        AnimatedVisibility(
            visible = showUi && dragOffsetY == 0f && !currentPageZoomed && topItem != null,
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
                            text = formatDate(topItem?.dateAdded ?: 0L),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = formatTime(topItem?.dateAdded ?: 0L),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    if (favoriteSupported) {
                        IconButton(onClick = { topItem?.let { toggleFavorite(it) } }) {
                            Icon(
                                imageVector = if (topItem?.isFavorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorit",
                                tint = if (topItem?.isFavorite == true) Color(0xFFE57373) else Color.White
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Detail") },
                                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showDetailsSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Buka dengan") },
                                leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    topItem?.let { MediaActions.openWith(context, it) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Jadikan sebagai") },
                                leadingIcon = { Icon(Icons.Outlined.Wallpaper, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    topItem?.let { MediaActions.useAs(context, it) }
                                }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showUi && dragOffsetY == 0f && !currentPageZoomed && topItem != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                if (topItem?.isVideo == true && activePlayer != null) {
                    VideoControls(
                        player = activePlayer!!,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomAction(Icons.Outlined.Share, "Bagikan") {
                        topItem?.let { MediaActions.share(context, it) }
                    }
                    BottomAction(Icons.Outlined.Edit, "Edit") {
                        topItem?.let { MediaActions.edit(context, it) }
                    }
                    BottomAction(Icons.Outlined.Info, "Detail") { showDetailsSheet = true }
                    BottomAction(Icons.Outlined.Delete, "Hapus") {
                        topItem?.let { requestDelete(it) }
                    }
                }
            }
        }

        if (showDetailsSheet && topItem != null) {
            ModalBottomSheet(
                onDismissRequest = { showDetailsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                MediaDetailsContent(item = topItem)
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
    onZoomChanged: (Boolean) -> Unit,
    onPlayerActive: (ExoPlayer) -> Unit,
    onPlayerInactive: (ExoPlayer) -> Unit
) {
    if (item.isVideo) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = dragOffsetY },
            contentAlignment = Alignment.Center
        ) {
            VideoPlayer(
                uri = item.uri,
                isSelected = isSelected,
                onPlayerActive = onPlayerActive,
                onPlayerInactive = onPlayerInactive
            )
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
                translationX = if (isZoomed) panX else 0f
                translationY = if (isZoomed) panY else dragOffsetY
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
                                    val tapChange = event.changes.firstOrNull()
                                    val tapX = tapChange?.position?.x ?: (size.width / 2f)
                                    val tapY = tapChange?.position?.y ?: (size.height / 2f)
                                    val tapXFromCenter = tapX - size.width / 2f
                                    val tapYFromCenter = tapY - size.height / 2f

                                    scope.launch {
                                        if (scale > 1.1f) {
                                            val startScale = scale
                                            val startPanX = panX
                                            val startPanY = panY
                                            animate(0f, 1f) { progress, _ ->
                                                scale = startScale + (1f - startScale) * progress
                                                panX = startPanX + (0f - startPanX) * progress
                                                panY = startPanY + (0f - startPanY) * progress
                                            }
                                            onZoomChanged(false)
                                        } else {
                                            val targetScale = 2.5f
                                            val maxPanX = size.width * (targetScale - 1f) / 2f
                                            val maxPanY = size.height * (targetScale - 1f) / 2f
                                            val targetPanX = (-tapXFromCenter * (targetScale - 1f)).coerceIn(-maxPanX, maxPanX)
                                            val targetPanY = (-tapYFromCenter * (targetScale - 1f)).coerceIn(-maxPanY, maxPanY)

                                            animate(0f, 1f) { progress, _ ->
                                                scale = 1f + (targetScale - 1f) * progress
                                                panX = targetPanX * progress
                                                panY = targetPanY * progress
                                            }
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
fun VideoPlayer(
    uri: Uri,
    isSelected: Boolean,
    onPlayerActive: (ExoPlayer) -> Unit,
    onPlayerInactive: (ExoPlayer) -> Unit
) {
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
            onPlayerActive(exoPlayer)
        } else {
            exoPlayer.pause()
            onPlayerInactive(exoPlayer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onPlayerInactive(exoPlayer)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun VideoControls(player: ExoPlayer, modifier: Modifier = Modifier) {
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var currentPosition by remember(player) { mutableLongStateOf(player.currentPosition) }
    var duration by remember(player) { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var volume by remember(player) { mutableFloatStateOf(player.volume) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = player.duration.coerceAtLeast(0L)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                currentPosition = newPosition.positionMs
            }
        }
        player.addListener(listener)

        isPlaying = player.isPlaying
        duration = player.duration.coerceAtLeast(0L)
        currentPosition = player.currentPosition
        volume = player.volume

        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(player, isPlaying) {
        if (isPlaying) {
            while (true) {
                currentPosition = player.currentPosition
                delay(200)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White
                )
            }

            Text(
                text = "${formatDuration(currentPosition)} / ${formatDuration(duration)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            IconButton(
                onClick = {
                    if (volume > 0f) {
                        player.volume = 0f
                        volume = 0f
                    } else {
                        player.volume = 1f
                        volume = 1f
                    }
                }
            ) {
                Icon(
                    imageVector = if (volume > 0f) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    contentDescription = if (volume > 0f) "Mute" else "Unmute",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { newValue ->
                currentPosition = newValue.toLong()
                player.seekTo(currentPosition)
            },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MediaDetailsContent(item: MediaItem) {
    val context = LocalContext.current
    val metadata by produceState<MediaMetadata?>(initialValue = null, item.id) {
        value = withContext(Dispatchers.IO) { MetadataReader.read(context, item) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Info",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        val data = metadata
        if (data == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            data.sections.forEach { section ->
                MetadataSectionCard(section)
                Spacer(modifier = Modifier.height(12.dp))
            }

            val lat = data.latitude
            val lon = data.longitude
            if (lat != null && lon != null) {
                TextButton(
                    onClick = { MediaActions.openLocation(context, lat, lon, item.displayName) }
                ) {
                    Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lihat di peta")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MetadataSectionCard(section: MetadataSection) {
    Text(
        text = section.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(6.dp))
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            section.fields.forEachIndexed { index, field ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = field.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(110.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = field.value,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (index < section.fields.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
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
