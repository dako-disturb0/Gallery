package com.gallery.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.gallery.app.data.MediaItem

@Composable
fun MediaThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(300)
                .size(256)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { ShimmerBox(Modifier.fillMaxSize()) },
            error = { ShimmerBox(Modifier.fillMaxSize()) },
        )
        if (item.isVideo) {
            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp),
            )
        }
    }
}

@Composable
fun AlbumCoverThumbnail(
    coverUri: android.net.Uri,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(coverUri)
            .crossfade(300)
            .size(512)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { ShimmerBox(Modifier.fillMaxSize()) },
        error = { ShimmerBox(Modifier.fillMaxSize()) },
    )
}
