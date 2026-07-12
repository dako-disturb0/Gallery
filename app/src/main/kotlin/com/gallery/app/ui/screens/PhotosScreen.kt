package com.gallery.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.gallery.app.data.MediaItem
import com.gallery.app.ui.components.ShimmerBox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotosScreen(
    groupedMediaItems: List<Pair<String, List<MediaItem>>>,
    isLoading: Boolean,
    onMediaClick: (MediaItem) -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()

    // Deteksi apakah sedang scroll (untuk minimize tanggal header)
    val isScrolled by remember {
        derivedStateOf { gridState.firstVisibleItemScrollOffset > 0 || gridState.firstVisibleItemIndex > 0 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            // Skeleton shimmer grid
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start = 2.dp, end = 2.dp,
                    top = 56.dp + 2.dp,
                    bottom = 120.dp
                ),
                verticalItemSpacing = 2.dp,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(18) { idx ->
                    // Variasikan tinggi skeleton secara deterministik
                    val ratio = when (idx % 5) {
                        0 -> 0.6f   // landscape-ish
                        1 -> 1.4f   // portrait
                        else -> 1f  // square
                    }
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((80.dp / ratio).coerceIn(60.dp, 130.dp))
                            .clip(MaterialTheme.shapes.small)
                    )
                }
            }
        } else if (groupedMediaItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        text = "Belum ada foto atau video",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start = 2.dp, end = 2.dp,
                    top = 56.dp + 2.dp,
                    bottom = 120.dp  // ruang untuk floating pill
                ),
                verticalItemSpacing = 2.dp,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                groupedMediaItems.forEach { (dateHeader, items) ->
                    // Tanggal Header — format singkat saat scroll, panjang saat idle
                    item(span = StaggeredGridItemSpan.FullLine) {
                        val headerAlpha by animateFloatAsState(
                            targetValue = if (isScrolled) 0.6f else 1f,
                            animationSpec = tween(200),
                            label = "headerAlpha"
                        )
                        // Format singkat dari dateHeader (ambil dd MM saja)
                        val shortDate = remember(dateHeader, isScrolled) {
                            shortDateFromHeader(dateHeader, isScrolled)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 12.dp, end = 8.dp, bottom = 6.dp)
                        ) {
                            Text(
                                text = shortDate,
                                style = if (isScrolled)
                                    MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                else
                                    MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = headerAlpha),
                            )
                        }
                    }

                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        val animProgress = remember { Animatable(0f) }
                        LaunchedEffect(item.id) {
                            animProgress.animateTo(
                                1f,
                                tween(280, delayMillis = (index % 9) * 25)
                            )
                        }

                        // Hitung rasio aspek: gunakan width/height dari MediaItem
                        val aspectRatio = remember(item.width, item.height) {
                            if (item.width > 0 && item.height > 0)
                                item.width.toFloat() / item.height.toFloat()
                            else 1f
                        }

                        DynamicMediaThumbnail(
                            item = item,
                            aspectRatio = aspectRatio,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .graphicsLayer {
                                    alpha = animProgress.value
                                    scaleX = 0.88f + 0.12f * animProgress.value
                                    scaleY = 0.88f + 0.12f * animProgress.value
                                }
                                .clickable { onMediaClick(item) },
                        )
                    }
                }
            }
        }

        // Header compact floating di atas grid
        CompactHeader(isScrolled = isScrolled, isLoading = isLoading)
    }
}

@Composable
private fun CompactHeader(isScrolled: Boolean, isLoading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isScrolled)
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                else
                    Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val titleAlpha by animateFloatAsState(
            targetValue = if (isScrolled) 0.85f else 1f,
            animationSpec = tween(200),
            label = "titleAlpha"
        )
        Text(
            text = "Foto",
            style = if (isScrolled)
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            else
                MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
            modifier = Modifier.weight(1f)
        )
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Menyiapkan...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DynamicMediaThumbnail(
    item: MediaItem,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val heightFactor = when {
        aspectRatio >= 1.5f -> 0.6f   // wide landscape → lebih pendek
        aspectRatio <= 0.6f -> 1.6f   // tall portrait → lebih tinggi
        else -> 1f                     // normal/square
    }
    // Gunakan fillMaxWidth() — height diatur oleh staggered grid secara otomatis via aspectRatio
    Box(modifier = modifier) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(200)
                .size(300)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .allowHardware(false)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                // Tinggi dinamis berdasarkan rasio aspek — clamp agar tidak terlalu ekstrem
                .height(
                    (80.dp * heightFactor * (1f / aspectRatio.coerceIn(0.4f, 2.5f))).coerceIn(60.dp, 200.dp)
                ),
            loading = { ShimmerBox(Modifier.fillMaxSize()) },
            error = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.PhotoLibrary,
                        null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
        // Overlay video badge
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.55f), MaterialTheme.shapes.small)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatVideoDuration(item.duration),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White
                )
            }
        }
    }
}

/** Dari header panjang seperti "12 Juli 2026", kembalikan "12 Jul" saat isScrolled, atau versi lengkap saat idle */
private fun shortDateFromHeader(header: String, isScrolled: Boolean): String {
    if (!isScrolled) return header
    // Coba ambil 2 token pertama: "12 Jul"
    val parts = header.trim().split(" ")
    return if (parts.size >= 2) "${parts[0]} ${parts[1]}" else header
}

private fun formatVideoDuration(durationMs: Long): String {
    if (durationMs <= 0) return ""
    val totalSec = durationMs / 1000
    val m = (totalSec / 60) % 60
    val s = totalSec % 60
    val h = totalSec / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
