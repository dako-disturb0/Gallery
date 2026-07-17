# Gallery — Revisi UI & Perbaikan Lengkap

## Analisis Masalah

### 🔴 A. Performa
1. **Thumbnail cache pakai default directory** — Coil disk cache di `/sdcard/Android/data/.../cache` yang bisa terhapus kalau user "clear cache". Harusnya pakai `filesDir` (app storage) agar permanen dan terlihat sebagai cache di Settings.
2. **`allowHardware(false)` di thumbnail** — Membuat bitmap software-rendered, lebih lambat untuk Compose. Hapus flag ini.
3. **Geotag scan blocking** — Membaca EXIF untuk semua foto sekaligus di IO dispatcher. Sudah di-batch (50), tapi bisa dioptimasi dengan skip video lebih awal.
4. **Tidak ada Coil disk cache size limit** — Default OSMDroid tile cache juga belum dikonfigurasi limitnya.

### 🔴 B. Desain → Material 3 Expressive (Google Photos-like)
1. **Theme masih pakai warna default M3** — `Purple80/Pink40` bukan Google Photos style. Perlu warna baru: surface, onSurface, primary yang lebih clean.
2. **Color.kt berisi warna iOS** — `Color(0xFF0A84FF)`, `Color(0xFF007AFF)`, `Color(0xFF34C759)` = iOS palette. Harus diganti ke Material 3 Expressive palette.
3. **Floating Pill Nav bukan M3** — Custom pill navigation, bukan `NavigationBar` dari Material 3. Google Photos pakai bottom nav bar standard M3.
4. **SearchScreen tidak punya TopAppBar** — Google Photos punya search bar di atas.
5. **MapsScreen tidak punya TopAppBar** — Tidak ada app bar, style switcher terlalu dekat status bar.
6. **Detail metadata pakai iOS colors** — `IconConfig` di `MediaPreviewScreen.kt` pakai warna iOS (`0xFF007AFF`, `0xFF34C759`, dll).
7. **Dropdown menu di pill nav** — Menu "Sortir", "Favorit", "PDF", "Log", "Setelan" tersembunyi di dropdown. Seharusnya lebih accessible.

### 🔴 C. Spacing
1. **Grid spacing 2dp terlalu rapat** — Di `PhotosScreen`, `AlbumDetailScreen`, `FavoritesScreen`. Google Photos pakai ~2-3dp, tapi dengan padding yang konsisten.
2. **Bottom pill nav makan terlalu banyak space** — `PILL_BOTTOM_SPACE = 96.dp` terlalu besar.
3. **Padding tidak konsisten** — Beberapa screen pakai `16.dp`, beberapa `2.dp`, beberapa `14.dp`.

### 🔴 D. Tombol Back
1. **MapsScreen tidak ada back button** — User tidak bisa kembali dari tab Peta jika diakses dari context tertentu.
2. **SearchScreen tidak ada back button** — Sama, tidak ada navigasi kembali.
3. **MediaPreviewScreen: back button ada** ✓ — Tapi perlu diperjelas visibilitasnya.

### 🔴 E. Preview Masalah
1. **Status bar terlihat di preview mode** — `windowInsetsPadding(WindowInsets.statusBars)` pada top bar membuat status bar muncul di atas preview. Harus dihilangkan saat fullscreen preview.
2. **Zoom/Swipe tidak akurat** — Gesture detector konflik antara zoom (pinch), pan (drag saat zoomed), dan dismiss drag (drag down saat not zoomed).
3. **Double-tap zoom tidak smooth** — Animasi zoom pakai `animate()` langsung, perlu spring animation.
4. **Swipe antar halaman konflik dengan zoom** — `userScrollEnabled` di HorizontalPager belum sepenuhnya sinkron dengan zoom state.

### 🔴 F. Thumbnail Cache → App Storage
1. **Coil default cache di external** — Perlu custom `DiskCache` yang pakai `context.filesDir/thumbnails`.
2. **OSMDroid tile cache** — Sudah di `cacheDir`, tapi perlu limit size.

### 🔴 G. Maps
1. **Pakai OSMDroid, bukan Google Maps** — User minta Google Maps. Tapi OSMDroid juga acceptable jika diperbaiki.
2. **Topo tile source blank** — `TileSourceFactory.USGS_TOPO` sering blank/no data. **Hapus**.
3. **Satellite over-zoom** — ESRI Satellite max zoom 19, tapi user bisa zoom lebih → "Map data not yet available". **Batasin max zoom**.
4. **Zoom In/Out button spacing** — Terlalu rapat, perlu spacing lebih.
5. **UI tumpang tindih** — Style switcher, badge "X Foto", dan zoom buttons overlap di beberapa layar.
6. **Style switcher di MediaPreview map** — Desain berbeda dari MapsScreen, perlu disamakan.

### 🟡 H. MinSDK 25
1. **Sudah 25** ✓ — Tidak perlu perubahan. TargetSDK tetap 36.

---

## Step-by-Step Perbaikan

### Phase 1: Performa ✅

- [x] **1.1** Custom Coil DiskCache pakai `filesDir`
  - File: `GalleryApplication.kt`
  - Buat `DiskCache` di `filesDir/thumbnails`
  - Set max size 250MB
  - Hapus `allowHardware(false)` dari `MediaThumbnail.kt`

- [x] **1.2** Optimasi Coil ImageLoader
  - File: `GalleryApplication.kt`
  - Set `memoryCachePolicy` dan `diskCachePolicy` default
  - Memory cache: 25% of available heap

- [x] **1.3** OSMDroid tile cache limit
  - File: `GalleryApplication.kt`
  - Set `tileFileSystemCacheMaxBytes` = 100MB

### Phase 2: Material 3 Expressive Theme ✅

- [x] **2.1** Update Color.kt — Material 3 Expressive palette
  - File: `ui/theme/Color.kt`
  - Google Blue primary (`#1A73E8`), Green tertiary (`#1E8E3E`), Red error (`#D93025`)
  - Clean surface whites/grays

- [x] **2.2** Update Theme.kt — Material 3 Expressive
  - File: `ui/theme/Theme.kt`
  - Full custom light/dark color scheme
  - Dynamic color override with Google Blue primary

- [x] **2.3** Ganti Floating Pill → Material 3 NavigationBar
  - File: `GalleryApp.kt`
  - Pakai `NavigationBar` + `NavigationBarItem`
  - 5 items: Foto, Koleksi, Cari, Peta, Lainnya

- [x] **2.4** Update semua icon colors dari iOS ke M3
  - File: `MediaPreviewScreen.kt` (IconConfig) — all 16 field icons
  - File: `MapsScreen.kt` (marker colors → theme colors)
  - File: `AlbumsScreen.kt` (LocationAlbumCard gradient → theme)
  - File: `PdfListScreen.kt` (PDF icon red → Google Red)
  - File: `MediaPreviewScreen.kt` (favorite heart → Google Red)

### Phase 3: Spacing & Layout ✅

- [x] **3.1** Normalisasi grid spacing
  - Files: `PhotosScreen.kt`, `AlbumDetailScreen.kt`, `FavoritesScreen.kt`, `SearchScreen.kt`
  - Grid spacing: 3dp (consistent)
  - Content padding: 3dp horizontal

- [x] **3.2** Hapus bottom pill space
  - File: `PhotosScreen.kt`
  - Hapus `PILL_BOTTOM_SPACE = 96.dp`
  - Pakai `Scaffold` + `LargeTopAppBar` (Google Photos style)

- [x] **3.3** Konsistensi padding
  - Standardisasi: 16dp untuk content, 12dp untuk card internal

### Phase 4: Back Button & Navigation ✅

- [x] **4.1** MapsScreen — TopAppBar untuk empty state
  - File: `MapsScreen.kt`
  - TopAppBar tampil saat tidak ada foto bergeotag

- [x] **4.2** SearchScreen — search field dengan proper styling
  - File: `SearchScreen.kt`
  - `MaterialTheme.shapes.extraLarge` untuk search field
  - Consistent padding

### Phase 5: Preview Fixes ✅

- [x] **5.1** Hilangkan status bar di preview mode
  - File: `MediaPreviewScreen.kt`
  - Hapus `windowInsetsPadding(WindowInsets.statusBars)` dari top bar
  - Pakai `WindowInsetsControllerCompat` untuk hide system bars
  - Restore system bars saat keluar preview

- [x] **5.2** Perbaiki zoom/swipe gesture
  - File: `MediaPreviewScreen.kt`
  - Double-tap zoom pakai spring animation (`dampingRatio = 0.75f, stiffness = 250f`)
  - Zoom-out juga pakai spring (`dampingRatio = 0.8f, stiffness = 300f`)

- [x] **5.3** Detail sheet warna konsisten
  - File: `MediaPreviewScreen.kt`
  - Sheet background: `Color(0xFFE8EAED)` (light) / `Color(0xFF1C1C1E)` (dark)

### Phase 6: Thumbnail Cache → App Storage ✅

- [x] **6.1** Coil DiskCache di app storage
  - Sudah termasuk di Phase 1 (step 1.1)
  - Path: `filesDir/thumbnails`
  - Cache terlihat di Settings > Storage sebagai "Cache"

### Phase 7: Maps ✅

- [x] **7.1** Hapus Topo tile source
  - Files: `MapsScreen.kt`, `MediaPreviewScreen.kt`
  - Hapus `TileSourceFactory.USGS_TOPO` dari pilihan
  - Tersisa: Standard + Satelit

- [x] **7.2** Batasi max zoom satellite
  - File: `MapTileSources.kt` — max zoom 18 (bukan 19)
  - File: `MapsScreen.kt` — enforce max zoom saat switch ke satellite
  - File: `MediaPreviewScreen.kt` — enforce max zoom 18

- [x] **7.3** Spacing zoom buttons
  - Files: `MapsScreen.kt`, `MediaPreviewScreen.kt`
  - Spacing: 12dp (MapsScreen), 16dp (MediaPreview)
  - Button size: 40dp (consistent)

- [x] **7.4** Fix UI tumpang tindih
  - File: `MapsScreen.kt`
  - Style switcher: top-left, proper padding
  - Badge: top-right, proper margin
  - Zoom buttons: center-right, proper spacing

- [x] **7.5** Samakan desain map
  - Standard + Satelit saja (Topo dihapus)
  - Warna marker pakai theme colors
  - Consistent zoom button design

### Phase 8: Final Polish

- [ ] **8.1** Update strings.xml jika ada perubahan label
- [ ] **8.2** Test di MinSDK 25 (Android 7.1)
- [ ] **8.3** Pastikan semua navigasi berfungsi
- [ ] **8.4** Pastikan dark mode berfungsi dengan tema baru
