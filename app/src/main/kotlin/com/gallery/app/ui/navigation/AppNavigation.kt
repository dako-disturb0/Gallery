package com.gallery.app.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Daftar rute navigasi utama aplikasi.
 */
sealed class Screen(val route: String) {
    data object Photos   : Screen("photos")
    data object Albums   : Screen("albums")
    data object Maps     : Screen("maps")
    data object Search   : Screen("search")
    data object Favorites: Screen("favorites")
    data object Settings : Screen("settings")

    data object AlbumDetail : Screen("album_detail/{albumId}/{albumName}") {
        fun createRoute(albumId: String, albumName: String) =
            "album_detail/${Uri.encode(albumId)}/${Uri.encode(albumName)}"
    }

    data object MediaPreview : Screen("media_preview/{itemId}?albumId={albumId}&isFavorite={isFavorite}&fromMaps={fromMaps}") {
        fun createRoute(itemId: Long, albumId: String? = null, isFavorite: Boolean = false, fromMaps: Boolean = false): String {
            return buildString {
                append("media_preview/$itemId")
                val params = mutableListOf<String>()
                if (albumId != null) params.add("albumId=${Uri.encode(albumId)}")
                if (isFavorite) params.add("isFavorite=true")
                if (fromMaps) params.add("fromMaps=true")
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.joinToString("&"))
                }
            }
        }
    }

    data object PdfViewer : Screen("pdf_viewer?uri={uri}&name={name}") {
        fun createRoute(uri: String, name: String) =
            "pdf_viewer?uri=${Uri.encode(uri)}&name=${Uri.encode(name)}"
    }
}

/**
 * Item tab di NavigationBar bawah (floating pill).
 * Hanya 3 tab utama: Foto, Koleksi, Peta
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
        selectedIcon  = Icons.Rounded.PhotoLibrary,
        unselectedIcon= Icons.Outlined.PhotoLibrary,
    ),
    NavItem(
        screen        = Screen.Albums,
        label         = "Koleksi",
        selectedIcon  = Icons.Rounded.Collections,
        unselectedIcon= Icons.Outlined.Collections,
    ),
    NavItem(
        screen        = Screen.Maps,
        label         = "Peta",
        selectedIcon  = Icons.Rounded.Map,
        unselectedIcon= Icons.Outlined.Map,
    ),
)
