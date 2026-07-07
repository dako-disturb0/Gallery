package com.gallery.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Album sementara
private data class AlbumData(val name: String, val count: Int)
private val placeholderAlbums = listOf(
    AlbumData("Kamera", 248),
    AlbumData("Screenshot", 91),
    AlbumData("Download", 37),
    AlbumData("WhatsApp", 163),
    AlbumData("Instagram", 54),
    AlbumData("Telegram", 22),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Album") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                )
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns        = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start  = 16.dp,
                end    = 16.dp,
                top    = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 8.dp,
            ),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                count        = placeholderAlbums.size,
                key          = { placeholderAlbums[it].name },
            ) { idx ->
                val album = placeholderAlbums[idx]
                AlbumCard(album)
            }
        }
    }
}

@Composable
private fun AlbumCard(album: AlbumData) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape  = MaterialTheme.shapes.medium,
            color  = MaterialTheme.colorScheme.surfaceContainerHighest,
            content = {}
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text  = album.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Text(
            text  = "${album.count} item",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
