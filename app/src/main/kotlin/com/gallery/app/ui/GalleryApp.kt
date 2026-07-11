package com.gallery.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.gallery.app.ui.navigation.bottomNavItems
import com.gallery.app.ui.screens.AlbumDetailScreen
import com.gallery.app.ui.screens.AlbumsScreen
import com.gallery.app.ui.screens.FavoritesScreen
import com.gallery.app.ui.screens.MediaPreviewScreen
import com.gallery.app.ui.screens.PhotosScreen
import com.gallery.app.ui.screens.SearchScreen
import com.gallery.app.ui.screens.SettingsScreen
import com.gallery.app.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch

private const val NAV_ANIM_DURATION = 280

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryApp(viewModel: GalleryViewModel = viewModel()) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasMediaAccess(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = hasMediaAccess(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasMediaAccess(context)
            }
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

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
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
                    fadeIn(tween(NAV_ANIM_DURATION)) +
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start,
                            tween(NAV_ANIM_DURATION),
                            initialOffset = { it / 10 }
                        )
                },
                exitTransition = {
                    fadeOut(tween(NAV_ANIM_DURATION)) +
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start,
                            tween(NAV_ANIM_DURATION),
                            targetOffset = { it / 10 }
                        )
                },
                popEnterTransition = {
                    fadeIn(tween(NAV_ANIM_DURATION)) +
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.End,
                            tween(NAV_ANIM_DURATION),
                            initialOffset = { it / 10 }
                        )
                },
                popExitTransition = {
                    fadeOut(tween(NAV_ANIM_DURATION)) +
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.End,
                            tween(NAV_ANIM_DURATION),
                            targetOffset = { it / 10 }
                        )
                },
            ) {
                // Main Screen: Swipeable Tabs
                composable("main") {
                    val coroutineScope = rememberCoroutineScope()
                    val pagerState = rememberPagerState(
                        initialPage = 0,
                        pageCount = { bottomNavItems.size }
                    )

                    Scaffold(
                        contentWindowInsets = WindowInsets(0.dp),
                        bottomBar = {
                            val isDark = isSystemInDarkTheme()
                            val iosSelectedColor = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
                            val iosUnselectedColor = Color(0xFF8E8E93)
                            val barContainerColor = if (isDark) Color(0xFF161616) else Color(0xFFF9F9F9)

                            NavigationBar(
                                containerColor = barContainerColor,
                                tonalElevation = 8.dp
                            ) {
                                bottomNavItems.forEachIndexed { index, item ->
                                    val selected = pagerState.currentPage == index

                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label,
                                            )
                                        },
                                        label = { Text(item.label) },
                                        alwaysShowLabel = true, // iOS style
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = iosSelectedColor,
                                            selectedTextColor = iosSelectedColor,
                                            unselectedIconColor = iosUnselectedColor,
                                            unselectedTextColor = iosUnselectedColor,
                                            indicatorColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    ) { tabPadding ->
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(tabPadding)
                            ) {
                                when (bottomNavItems[page].screen) {
                                    Screen.Photos -> PhotosScreen(
                                        groupedMediaItems = groupedMediaItems,
                                        isLoading = isLoading,
                                        onMediaClick = { mediaItem ->
                                            navController.navigate(Screen.MediaPreview.createRoute(mediaItem.id))
                                        }
                                    )
                                    Screen.Albums -> AlbumsScreen(
                                        albums = albums,
                                        isLoading = isLoading,
                                        onAlbumClick = { album ->
                                            navController.navigate(Screen.AlbumDetail.createRoute(album.id, album.name))
                                        }
                                    )
                                    Screen.Search -> SearchScreen(
                                        query = searchQuery,
                                        onQueryChange = viewModel::updateSearchQuery,
                                        results = searchResults,
                                        onMediaClick = { mediaItem ->
                                            navController.navigate(Screen.MediaPreview.createRoute(mediaItem.id))
                                        }
                                    )
                                    Screen.Favorites -> FavoritesScreen(
                                        favorites = favorites,
                                        onMediaClick = { mediaItem ->
                                            navController.navigate(Screen.MediaPreview.createRoute(mediaItem.id, isFavorite = true))
                                        }
                                    )
                                    Screen.Settings -> SettingsScreen(
                                        currentGrouping = dateGrouping,
                                        onGroupingChange = viewModel::setDateGrouping
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }
                }

                // Detail Screens (Pushed on top of the main layout, hiding bottom bar)
                composable(Screen.AlbumDetail.route) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                    val albumName = backStackEntry.arguments?.getString("albumName") ?: ""
                    AlbumDetailScreen(
                        albumId = albumId,
                        albumName = albumName,
                        mediaItems = mediaItems,
                        onBackClick = { navController.popBackStack() },
                        onMediaClick = { mediaItem ->
                            navController.navigate(Screen.MediaPreview.createRoute(mediaItem.id, albumId = albumId))
                        }
                    )
                }

                composable(Screen.MediaPreview.route) { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getString("itemId")?.toLongOrNull() ?: 0L
                    val albumId = backStackEntry.arguments?.getString("albumId")
                    val isFavorite = backStackEntry.arguments?.getString("isFavorite")?.toBoolean() ?: false

                    MediaPreviewScreen(
                        initialItemId = itemId,
                        albumId = albumId,
                        isFavorite = isFavorite,
                        allMediaItems = mediaItems,
                        favoritesList = favorites,
                        onBackClick = { navController.popBackStack() },
                        onFavoriteRequest = viewModel::favoriteRequest,
                        onDeleteRequest = viewModel::deleteRequest
                    )
                }
            }
        }
    }
}
