package com.gallery.app.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewScreen(
    initialItemId: Long,
    albumId: String?,
    isFavorite: Boolean,
    allMediaItems: List<MediaItem>,
    favoritesList: List<MediaItem>,
    onBackClick: () -> Unit
) {
    // 1. Filter the media collection to match the source screen (Album, Favorites, or main grid)
    val displayList = remember(allMediaItems, favoritesList, albumId, isFavorite) {
        when {
            albumId != null -> allMediaItems.filter { it.bucketId == albumId }
            isFavorite -> favoritesList
            else -> allMediaItems
        }
    }

    // 2. Find the initial index of the media item in the list
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

    var showUi by remember { mutableStateOf(true) }
    val currentItem by remember(pagerState.currentPage, displayList) {
        derivedStateOf { displayList[pagerState.currentPage] }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            if (showUi) {
                TopAppBar(
                    title = {
                        Text(
                            text = currentItem.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { showUi = !showUi }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = displayList[page]
                val isSelected = page == pagerState.currentPage

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isVideo) {
                        VideoPlayer(uri = item.uri, isSelected = isSelected)
                    } else {
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
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(uri: Uri, isSelected: Boolean) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = Media3MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
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
        onDispose {
            exoPlayer.release()
        }
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
