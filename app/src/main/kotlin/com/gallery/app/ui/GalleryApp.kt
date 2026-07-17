package com.gallery.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.gallery.app.ui.screens.LogScreen
import com.gallery.app.ui.screens.MapsScreen
import com.gallery.app.ui.screens.MediaPreviewScreen
import com.gallery.app.ui.screens.PdfListScreen
import com.gallery.app.ui.screens.PdfViewerScreen
import com.gallery.app.ui.screens.PhotosScreen
import com.gallery.app.ui.screens.SearchScreen
import com.gallery.app.ui.screens.SettingsScreen
import com.gallery.app.viewmodel.GalleryViewModel

private const val TAB_FOTO = 0
private const val TAB_KOLEKSI = 1
private const val TAB_CARI = 2
private const val TAB_PETA = 3
private const val NAV_ANIM_DURATION = 280

private data class BottomNavEntry(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavEntries = listOf(
    BottomNavEntry(TAB_FOTO, "Foto", Icons.Rounded.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    BottomNavEntry(TAB_KOLEKSI, "Koleksi", Icons.Rounded.Collections, Icons.Outlined.Collections),
    BottomNavEntry(TAB_CARI, "Cari", Icons.Rounded.Search, Icons.Outlined.Search),
    BottomNavEntry(TAB_PETA, "Peta", Icons.Rounded.Map, Icons.Outlined.Map),
)

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

    var selectedTab by remember { mutableIntStateOf(TAB_FOTO) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Switch ke tab Peta jika ada pending item dari MediaPreview
    LaunchedEffect(pendingMapItemId) {
        if (pendingMapItemId > 0L) selectedTab = TAB_PETA
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                bottomNavEntries.forEach { entry ->
                    NavigationBarItem(
                        selected = selectedTab == entry.index,
                        onClick = {
                            selectedTab = entry.index
                            if (entry.index != TAB_PETA) viewModel.clearPendingMapItem()
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == entry.index) entry.selectedIcon else entry.unselectedIcon,
                                contentDescription = entry.label,
                            )
                        },
                        label = { Text(entry.label) },
                        alwaysShowLabel = true,
                    )
                }
                // More menu as last item
                Box {
                    NavigationBarItem(
                        selected = false,
                        onClick = { showMoreMenu = true },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Lainnya",
                            )
                        },
                        label = { Text("Lainnya") },
                        alwaysShowLabel = true,
                    )
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Favorit") },
                            onClick = { showMoreMenu = false; navController.navigate("favorites_overlay") },
                            leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("PDF") },
                            onClick = { showMoreMenu = false; navController.navigate("pdf_list_overlay") },
                            leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Log & Diagnostik") },
                            onClick = { showMoreMenu = false; navController.navigate("log_overlay") },
                            leadingIcon = { Icon(Icons.Outlined.BugReport, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Setelan") },
                            onClick = { showMoreMenu = false; navController.navigate("settings_overlay") },
                            leadingIcon = { Icon(Icons.Outlined.Settings, null) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
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
                // ── Main: tab content ──
                composable("main") {
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
                        }
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

                // ── Favorit ──
                composable("favorites_overlay") {
                    FavoritesScreen(
                        favorites = favorites,
                        onMediaClick = { item ->
                            navController.navigate(Screen.MediaPreview.createRoute(item.id, isFavorite = true))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // ── Setelan ──
                composable("settings_overlay") {
                    SettingsScreen(
                        currentGrouping = dateGrouping,
                        onGroupingChange = viewModel::setDateGrouping,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // ── PDF list ──
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

                // ── PDF viewer ──
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

                // ── Log & Diagnostik ──
                composable("log_overlay") {
                    LogScreen(onBackClick = { navController.popBackStack() })
                }
            }
        }
    }
}
