package com.gallery.app.ui.screens

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Placeholder — nanti dari MediaStore (isFavorite == true)
private val favoritePlaceholder = emptyList<Int>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Favorit") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                )
            )
        }
    ) { innerPadding ->
        if (favoritePlaceholder.isEmpty()) {
            // Empty state
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector        = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier           = Modifier.size(56.dp),
                        tint               = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        text      = "Belum ada favorit",
                        style     = MaterialTheme.typography.titleMedium,
                        modifier  = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text      = "Ketuk lama foto untuk menandainya sebagai favorit",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp),
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns        = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start  = 2.dp,
                    end    = 2.dp,
                    top    = innerPadding.calculateTopPadding() + 2.dp,
                    bottom = innerPadding.calculateBottomPadding() + 2.dp,
                ),
                verticalArrangement   = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(favoritePlaceholder.size) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.small,
                        content = {}
                    )
                }
            }
        }
    }
}
