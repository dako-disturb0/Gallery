package com.gallery.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Daftar rute navigasi utama aplikasi.
 */
sealed class Screen(val route: String) {
    data object Photos   : Screen("photos")
    data object Albums   : Screen("albums")
    data object Search   : Screen("search")
    data object Favorites: Screen("favorites")
}

/**
 * Item tab di NavigationBar bawah.
 */
data class NavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    NavItem(
        screen        = Screen.Photos,
        label         = "Foto",
        selectedIcon  = Icons.Rounded.GridView,
        unselectedIcon= Icons.Outlined.GridView,
    ),
    NavItem(
        screen        = Screen.Albums,
        label         = "Album",
        selectedIcon  = Icons.Rounded.PhotoAlbum,
        unselectedIcon= Icons.Outlined.PhotoAlbum,
    ),
    NavItem(
        screen        = Screen.Search,
        label         = "Cari",
        selectedIcon  = Icons.Rounded.Search,
        unselectedIcon= Icons.Outlined.Search,
    ),
    NavItem(
        screen        = Screen.Favorites,
        label         = "Favorit",
        selectedIcon  = Icons.Rounded.Favorite,
        unselectedIcon= Icons.Outlined.FavoriteBorder,
    ),
)
