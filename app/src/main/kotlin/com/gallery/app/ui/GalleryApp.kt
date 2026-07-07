package com.gallery.app.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gallery.app.ui.navigation.Screen
import com.gallery.app.ui.navigation.bottomNavItems
import com.gallery.app.ui.screens.AlbumsScreen
import com.gallery.app.ui.screens.FavoritesScreen
import com.gallery.app.ui.screens.PhotosScreen
import com.gallery.app.ui.screens.SearchScreen

private const val NAV_ANIM_DURATION = 280

@Composable
fun GalleryApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        // Tidak pakai contentWindowInsets default — tiap layar urus sendiri
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == item.screen.route } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick  = {
                            navController.navigate(item.screen.route) {
                                // Hindari stack menumpuk — kembali ke start destination
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
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
                navController    = navController,
                startDestination = Screen.Photos.route,
                // Transisi antar tab: fade singkat agar tidak berlebihan
                enterTransition  = {
                    fadeIn(tween(NAV_ANIM_DURATION)) +
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        tween(NAV_ANIM_DURATION),
                        initialOffset = { it / 10 }
                    )
                },
                exitTransition   = {
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
                composable(Screen.Photos.route)    { PhotosScreen() }
                composable(Screen.Albums.route)    { AlbumsScreen() }
                composable(Screen.Search.route)    { SearchScreen() }
                composable(Screen.Favorites.route) { FavoritesScreen() }
            }
        }
    }
}
