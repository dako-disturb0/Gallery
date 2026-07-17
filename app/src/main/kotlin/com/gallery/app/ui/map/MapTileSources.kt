package com.gallery.app.ui.map

import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

/**
 * Shared OSMDroid tile sources for the app.
 *
 * ArcGIS World_Imagery serves tiles at `.../tile/{z}/{row}/{col}` where
 * `row = y` and `col = x`. OSMDroid's default [XYTileSource.getTileURLString]
 * emits `{z}/{x}/{y}`, which transposes the axes and yields wrong tiles, so we
 * override the URL builder to emit `{z}/{y}/{x}`.
 */
private const val ESRI_IMAGERY_BASE =
    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"

val EsriSatelliteTileSource: XYTileSource =
    object : XYTileSource(
        "Satelit", 0, 18, 256, ".jpg",
        arrayOf(ESRI_IMAGERY_BASE)
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return baseUrl +
                MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex) +
                ".jpg"
        }
    }
