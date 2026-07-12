package com.gallery.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.gallery.app.data.CrashLogger
import org.osmdroid.config.Configuration

class GalleryApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        // Pasang penangkap crash sedini mungkin agar crash startup pun tercatat.
        CrashLogger.install(this)
        // Inisialisasi OSMDroid sekali di awal: user agent + cache path.
        // Tanpa ini tile bisa gagal diunduh (peta kosong).
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = java.io.File(cacheDir, "osmdroid-tiles")
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
