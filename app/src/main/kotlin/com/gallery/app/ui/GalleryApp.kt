package com.gallery.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
private const val TAB_MORE = 4
private const val NAV_ANIM_DURATION = 280

private data class BottomNavEntry(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showLabel: Boolean = true
)

private val bottomNavEntries = listOf(
    BottomNavEntry(TAB_FOTO, "Photos", Icons.Rounded.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    BottomNavEntry(TAB_KOLEKSI, "Albums", Icons.Rounded.Collections, Icons.Outlined.Collections),
    BottomNavEntry(TAB_CARI, "Search", Icons.Rounded.Search, Icons.Outlined.Search, showLabel = false),
    BottomNavEntry(TAB_PETA, "Places", Icons.Rounded.Map, Icons.Outlined.Map),
    BottomNavEntry(TAB_MORE, "More", Icons.Filled.MoreVert, Icons.Filled.MoreVert, showLabel = true),
)

@OptIn(ExperimentalFoundationApi::class)
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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

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

    LaunchedEffect(pendingMapItemId) {
        if (pendingMapItemId > 0L) selectedTab = TAB_PETA
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            composable("main") {
                val pagerState = rememberPagerState(pageCount = { 4 })
                
                LaunchedEffect(selectedTab) {
                    if (selectedTab < 4) {
                        pagerState.animateScrollToPage(selectedTab)
                    }
                }
                
                LaunchedEffect(pagerState.currentPage) {
                    if (selectedTab != TAB_MORE) {
                        selectedTab = pagerState.currentPage
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
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

                    // Material 3 Expressive / iOS style Floating Navigation Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .shadow(16.dp, RoundedCornerShape(100))
                                .clip(RoundedCornerShape(100))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) // Translucent iOS effect
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomNavEntries.forEach { entry ->
                                val isSelected = if (entry.index == TAB_MORE) showMoreMenu else selectedTab == entry.index
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    label = "pillBgColor"
                                )
                                val contentColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    label = "pillContentColor"
                                )

                                Box(modifier = Modifier.wrapContentSize()) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100))
                                            .background(bgColor)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                if (entry.index == TAB_MORE) {
                                                    showMoreMenu = true
                                                } else {
                                                    selectedTab = entry.index
                                                    showMoreMenu = false
                                                    if (entry.index != TAB_PETA) viewModel.clearPendingMapItem()
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 12.dp)
                                            .animateContentSize(tween(250)),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) entry.selectedIcon else entry.unselectedIcon,
                                            contentDescription = entry.label,
                                            tint = contentColor,
                                            modifier = Modifier.size(24.dp)
                                        )

                                        if (entry.showLabel && isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = entry.label,
                                                color = contentColor,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    if (entry.index == TAB_MORE) {
                                        DropdownMenu(
                                            expanded = showMoreMenu,
                                            onDismissRequest = { showMoreMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Favorites") },
                                                onClick = { showMoreMenu = false; navController.navigate("favorites_overlay") },
                                                leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("PDFs") },
                                                onClick = { showMoreMenu = false; navController.navigate("pdf_list_overlay") },
                                                leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) }
                                            )
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Logs & Diagnostics") },
                                                onClick = { showMoreMenu = false; navController.navigate("log_overlay") },
                                                leadingIcon = { Icon(Icons.Outlined.BugReport, null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Settings") },
                                                onClick = { showMoreMenu = false; navController.navigate("settings_overlay") },
                                                leadingIcon = { Icon(Icons.Outlined.Settings, null) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

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

            composable("favorites_overlay") {
                FavoritesScreen(
                    favorites = favorites,
                    onMediaClick = { item ->
                        navController.navigate(Screen.MediaPreview.createRoute(item.id, isFavorite = true))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("settings_overlay") {
                SettingsScreen(
                    currentGrouping = dateGrouping,
                    onGroupingChange = viewModel::setDateGrouping,
                    onBackClick = { navController.popBackStack() }
                )
            }

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

            composable("log_overlay") {
                LogScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}
}
