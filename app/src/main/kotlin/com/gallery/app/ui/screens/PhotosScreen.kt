package com.gallery.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gallery.app.data.MediaItem
import com.gallery.app.ui.components.MediaThumbnail
import com.gallery.app.ui.components.ShimmerBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    groupedMediaItems: List<Pair<String, List<MediaItem>>>,
    isLoading: Boolean,
    onMediaClick: (MediaItem) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Foto") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start = 2.dp, end = 2.dp,
                    top = innerPadding.calculateTopPadding() + 2.dp,
                    bottom = innerPadding.calculateBottomPadding() + 2.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(24) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start = 2.dp, end = 2.dp,
                    top = innerPadding.calculateTopPadding() + 2.dp,
                    bottom = innerPadding.calculateBottomPadding() + 2.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                groupedMediaItems.forEach { (dateHeader, items) ->
                    // Tanggal Header Span Penuh
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)
                        )
                    }

                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        val animProgress = remember { Animatable(0f) }
                        LaunchedEffect(item.id) {
                            animProgress.animateTo(
                                1f,
                                tween(300, delayMillis = (index % 12) * 30)
                            )
                        }
                        MediaThumbnail(
                            item = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.small)
                                .graphicsLayer {
                                    alpha = animProgress.value
                                    scaleX = 0.85f + 0.15f * animProgress.value
                                    scaleY = 0.85f + 0.15f * animProgress.value
                                }
                                .animateItem()
                                .clickable { onMediaClick(item) },
                        )
                    }
                }
            }
        }
    }
}
