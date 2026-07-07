package com.gallery.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gallery.app.ui.components.PermissionScreen
import com.gallery.app.ui.components.requiredPermissions
import com.gallery.app.ui.navigation.Screen
import com.gallery.app.ui.navigation.bottomNavItems
import com.gallery.app.ui.screens.AlbumsScreen
import com.gallery.app.ui.screens.FavoritesScreen
import com.gallery.app.ui.screens.PhotosScreen
import com.gallery.app.ui.screens.SearchScreen
import com.gallery.app.viewmodel.GalleryViewModel

private const val NAV_ANIM_DURATION = 280

@Composable
fun GalleryApp(viewModel: GalleryViewModel = viewModel()) {
    val context = LocalContext.current
    var hasPermission by rememberSaveable {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.loadMedia()
    }

    if (!hasPermission) {
        PermissionScreen(onRequestPermission = { permissionLauncher.launch(requiredPermissions) })
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val mediaItems by viewModel.mediaItems.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == item.screen.route } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                        alwaysShowLabel = true,
                    )
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
                startDestination = Screen.Photos.route,
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
                composable(Screen.Photos.route) {
                    PhotosScreen(mediaItems = mediaItems, isLoading = isLoading)
                }
                composable(Screen.Albums.route) {
                    AlbumsScreen(albums = albums, isLoading = isLoading)
                }
                composable(Screen.Search.route) {
                    SearchScreen(
                        query = searchQuery,
                        onQueryChange = viewModel::updateSearchQuery,
                        results = searchResults,
                    )
                }
                composable(Screen.Favorites.route) {
                    FavoritesScreen(favorites = favorites)
                }
            }
        }
    }
}
