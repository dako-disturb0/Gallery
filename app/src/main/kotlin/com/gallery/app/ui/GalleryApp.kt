package com.gallery.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gallery.app.ui.components.PermissionScreen
import com.gallery.app.ui.components.hasMediaAccess
import com.gallery.app.ui.components.requiredPermissions
import com.gallery.app.ui.navigation.Screen
import com.gallery.app.ui.screens.AlbumDetailScreen
import com.gallery.app.ui.screens.AlbumsScreen
import com.gallery.app.ui.screens.FavoritesScreen
import com.gallery.app.ui.screens.MapsScreen
import com.gallery.app.ui.screens.MediaPreviewScreen
import com.gallery.app.ui.screens.PdfListScreen
import com.gallery.app.ui.screens.PdfViewerScreen
import com.gallery.app.ui.screens.PhotosScreen
import com.gallery.app.ui.screens.SearchScreen
import com.gallery.app.ui.screens.SettingsScreen
import com.gallery.app.viewmodel.DateGrouping
import com.gallery.app.viewmodel.GalleryViewModel

private const val TAB_FOTO = 0
private const val TAB_KOLEKSI = 1
private const val TAB_CARI = 2
private const val TAB_PETA = 3
private const val NAV_ANIM_DURATION = 280

@Composable
fun GalleryApp(viewModel: GalleryViewModel = viewModel()) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasMediaAccess(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasPermission = hasMediaAccess(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasPermission = hasMediaAccess(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.loadMedia()
    }

    if (!hasPermission) {
        PermissionScreen(onRequestPermission = { permissionLauncher.launch(requiredPermissions) })
        return
    }

    val navController = rememberNavController()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val groupedMediaItems by viewModel.groupedMediaItems.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val dateGrouping by viewModel.dateGrouping.collectAsState()
    val geotaggedItems by viewModel.geotaggedItems.collectAsState()
    val pendingMapItemId by viewModel.pendingMapItemId.collectAsState()
    val pdfs by viewModel.pdfs.collectAsState()
    val isLoadingPdfs by viewModel.isLoadingPdfs.collectAsState()

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = "main",
                enterTransition = {
                    fadeIn(tween(NAV_ANIM_DURATION)) + slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        tween(NAV_ANIM_DURATION),
                        initialOffset = { it / 10 }
                    )
                },
                exitTransition = {
                    fadeOut(tween(NAV_ANIM_DURATION)) + slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        tween(NAV_ANIM_DURATION),
                        targetOffset = { it / 10 }
                    )
                },
                popEnterTransition = {
                    fadeIn(tween(NAV_ANIM_DURATION)) + slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        tween(NAV_ANIM_DURATION),
                        initialOffset = { it / 10 }
                    )
                },
                popExitTransition = {
                    fadeOut(tween(NAV_ANIM_DURATION)) + slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        tween(NAV_ANIM_DURATION),
                        targetOffset = { it / 10 }
                    )
                },
            ) {
                // ── Main: Floating Pill Tabs ──
                composable("main") {
                    var selectedTab by remember { mutableIntStateOf(TAB_FOTO) }

                    // Switch ke tab Peta jika ada pending item dari MediaPreview
                    LaunchedEffect(pendingMapItemId) {
                        if (pendingMapItemId > 0L) {
                            selectedTab = TAB_PETA
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                            modifier = Modifier.fillMaxSize(),
                            label = "tabContent"
                        ) { tab ->
                            when (tab) {
                                TAB_FOTO -> PhotosScreen(
                                    groupedMediaItems = groupedMediaItems,
                                    isLoading = isLoading,
                                    onMediaClick = { item ->
                                        navController.navigate(Screen.MediaPreview.createRoute(item.id))
                                    }
                                )
                                TAB_KOLEKSI -> AlbumsScreen(
                                    albums = albums,
                                    isLoading = isLoading,
                                    geotaggedCount = geotaggedItems.size,
                                    onAlbumClick = { album ->
                                        navController.navigate(Screen.AlbumDetail.createRoute(album.id, album.name))
                                    },
                                    onLocationAlbumClick = { selectedTab = TAB_PETA }
                                )
                                TAB_CARI -> SearchScreen(
                                    query = searchQuery,
                                    onQueryChange = viewModel::updateSearchQuery,
                                    results = searchResults,
                                    onMediaClick = { item ->
                                        navController.navigate(Screen.MediaPreview.createRoute(item.id))
                                    }
                                )
                                TAB_PETA -> MapsScreen(
                                    geotaggedItems = geotaggedItems,
                                    selectedItemId = if (pendingMapItemId > 0L) pendingMapItemId else null,
                                    onMediaClick = { item ->
                                        viewModel.clearPendingMapItem()
                                        navController.navigate(Screen.MediaPreview.createRoute(item.id, fromMaps = true))
                                    }
                                )
                                else -> PhotosScreen(
                                    groupedMediaItems = groupedMediaItems,
                                    isLoading = isLoading,
                                    onMediaClick = { item ->
                                        navController.navigate(Screen.MediaPreview.createRoute(item.id))
                                    }
                                )
                            }
                        }

                        // Floating Pill Nav — overlay di bawah
                        FloatingPillNav(
                            selectedTab = selectedTab,
                            onTabSelect = { tab ->
                                selectedTab = tab
                                if (tab != TAB_PETA) viewModel.clearPendingMapItem()
                            },
                            currentGrouping = dateGrouping,
                            onGroupingChange = viewModel::setDateGrouping,
                            onFavoritesClick = { navController.navigate("favorites_overlay") },
                            onSettingsClick = { navController.navigate("settings_overlay") },
                            onPdfClick = { navController.navigate("pdf_list_overlay") },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }

                // ── Album Detail ──
                composable(Screen.AlbumDetail.route) { back ->
                    val albumId = back.arguments?.getString("albumId") ?: ""
                    val albumName = back.arguments?.getString("albumName") ?: ""
                    AlbumDetailScreen(
                        albumId = albumId,
                        albumName = albumName,
                        mediaItems = mediaItems,
                        onBackClick = { navController.popBackStack() },
                        onMediaClick = { item ->
                            navController.navigate(Screen.MediaPreview.createRoute(item.id, albumId = albumId))
                        }
                    )
                }

                // ── Media Preview ──
                composable(Screen.MediaPreview.route) { back ->
                    val itemId = back.arguments?.getString("itemId")?.toLongOrNull() ?: 0L
                    val albumId = back.arguments?.getString("albumId")
                    val isFavorite = back.arguments?.getString("isFavorite")?.toBoolean() ?: false

                    val hasGeoTag = remember(itemId, geotaggedItems) {
                        geotaggedItems.any { it.id == itemId }
                    }

                    MediaPreviewScreen(
                        initialItemId = itemId,
                        albumId = albumId,
                        isFavorite = isFavorite,
                        allMediaItems = mediaItems,
                        favoritesList = favorites,
                        onBackClick = { navController.popBackStack() },
                        onFavoriteRequest = viewModel::favoriteRequest,
                        onDeleteRequest = viewModel::deleteRequest,
                        onMapClick = if (hasGeoTag) {
                            { item ->
                                viewModel.requestOpenInMap(item.id)
                                navController.popBackStack("main", inclusive = false)
                            }
                        } else null
                    )
                }

                // ── Favorit (push overlay) ──
                composable("favorites_overlay") {
                    FavoritesScreen(
                        favorites = favorites,
                        onMediaClick = { item ->
                            navController.navigate(Screen.MediaPreview.createRoute(item.id, isFavorite = true))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // ── Setelan (push overlay) ──
                composable("settings_overlay") {
                    SettingsScreen(
                        currentGrouping = dateGrouping,
                        onGroupingChange = viewModel::setDateGrouping,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // ── PDF: daftar dokumen (push overlay) ──
                composable("pdf_list_overlay") {
                    LaunchedEffect(Unit) { viewModel.loadPdfs() }
                    PdfListScreen(
                        pdfs = pdfs,
                        isLoading = isLoadingPdfs,
                        onPdfClick = { uri, name ->
                            navController.navigate(Screen.PdfViewer.createRoute(uri.toString(), name))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // ── PDF: penampil dokumen ──
                composable(Screen.PdfViewer.route) { back ->
                    val uriStr = back.arguments?.getString("uri")
                    val name = back.arguments?.getString("name") ?: "Dokumen"
                    val uri = uriStr?.let { Uri.parse(it) }
                    if (uri != null) {
                        PdfViewerScreen(
                            uri = uri,
                            title = name,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Floating Pill Navigation Component
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FloatingPillNav(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    currentGrouping: DateGrouping,
    onGroupingChange: (DateGrouping) -> Unit,
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPdfClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val pillBg = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.93f) else Color(0xFFF0F0F3).copy(alpha = 0.96f)
    val selectedColor = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val unselectedColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)

    var showMoreMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(50),
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(50)),
        color = pillBg,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Tab Foto
            PillTab(
                icon = if (selectedTab == TAB_FOTO) Icons.Rounded.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                label = "Foto",
                isSelected = selectedTab == TAB_FOTO,
                showLabel = true,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                onClick = { onTabSelect(TAB_FOTO) }
            )
            // Tab Koleksi
            PillTab(
                icon = if (selectedTab == TAB_KOLEKSI) Icons.Rounded.Collections else Icons.Outlined.Collections,
                label = "Koleksi",
                isSelected = selectedTab == TAB_KOLEKSI,
                showLabel = true,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                onClick = { onTabSelect(TAB_KOLEKSI) }
            )
            // Tab Cari — hanya ikon
            PillTab(
                icon = if (selectedTab == TAB_CARI) Icons.Rounded.Search else Icons.Outlined.Search,
                label = "",
                isSelected = selectedTab == TAB_CARI,
                showLabel = false,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                onClick = { onTabSelect(TAB_CARI) }
            )
            // Tab Peta
            PillTab(
                icon = if (selectedTab == TAB_PETA) Icons.Rounded.Map else Icons.Outlined.Map,
                label = "Peta",
                isSelected = selectedTab == TAB_PETA,
                showLabel = true,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                onClick = { onTabSelect(TAB_PETA) }
            )

            // Divider tipis
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6))
            )

            // More (...) button
            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Lainnya",
                        tint = unselectedColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    // Sort by date section
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Sortir Berdasarkan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {},
                        enabled = false
                    )
                    DateGrouping.entries.forEach { grouping ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = currentGrouping == grouping,
                                        onClick = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = when (grouping) {
                                            DateGrouping.DAILY -> "Hari"
                                            DateGrouping.WEEKLY -> "Minggu"
                                            DateGrouping.MONTHLY -> "Bulan"
                                            DateGrouping.YEARLY -> "Tahun"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (currentGrouping == grouping) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                onGroupingChange(grouping)
                                showMoreMenu = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Favorit") },
                        onClick = { showMoreMenu = false; onFavoritesClick() },
                        leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(17.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("PDF") },
                        onClick = { showMoreMenu = false; onPdfClick() },
                        leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null, modifier = Modifier.size(17.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Setelan") },
                        onClick = { showMoreMenu = false; onSettingsClick() },
                        leadingIcon = { Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(17.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PillTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    showLabel: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "pillBgAlpha"
    )
    val selectedBg = if (isDark) Color(0xFF2C2C2E) else Color.White

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(selectedBg.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = if (showLabel && label.isNotEmpty()) 12.dp else 10.dp,
                vertical = 8.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label.ifEmpty { null },
                tint = if (isSelected) selectedColor else unselectedColor,
                modifier = Modifier.size(18.dp)
            )
            if (showLabel && label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) selectedColor else unselectedColor,
                )
            }
        }
    }
}
