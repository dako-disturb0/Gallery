# TASK.md — Gallery App Overhaul
> Dibuat: 2026-07-12 | Status: IN PROGRESS

---

## Konteks & Tujuan
Refactor besar aplikasi Gallery Android (Kotlin/Jetpack Compose).
Screenshot referensi: `screenshot/ubah.png` (kondisi lama) → `screenshot/contoh.png` (target baru).
AppNavigation.kt sudah diupdate (3 tab: Foto, Koleksi, Peta).

---

## STATUS BLOK

| Blok | File Target | Status |
|------|-------------|--------|
| Pre | `ui/navigation/AppNavigation.kt` | ✅ Selesai |
| F | `ui/components/MediaThumbnail.kt` | ✅ Selesai |
| E | `ui/screens/MediaPreviewScreen.kt` | ✅ Selesai |
| D-VM | `viewmodel/GalleryViewModel.kt` | ✅ Selesai |
| C | `ui/screens/MapsScreen.kt` (NEW) | ✅ Selesai |
| B | `ui/screens/PhotosScreen.kt` | ✅ Selesai |
| D-Albums | `ui/screens/AlbumsScreen.kt` | ✅ Selesai |
| A | `ui/GalleryApp.kt` | 🔲 TERAKHIR |

---

## BLOK F — MediaThumbnail Optimized
File: `ui/components/MediaThumbnail.kt`
- diskCachePolicy + memoryCachePolicy ENABLED
- Smooth shimmer pulse loading
- Size hint dari density

---

## BLOK E — MediaPreview: Tombol Peta + Hapus Map dari Detail
File: `ui/screens/MediaPreviewScreen.kt`
- Tambah tombol "Peta" di bottom action row (hanya jika ada geotag)
- Callback: `onMapClick: (MediaItem) -> Unit`
- HAPUS render OpenStreetMap dari MediaDetailsContent
- Koordinat teks tetap ada, tapi MapView dihapus dari detail sheet

---

## BLOK D-VM — GalleryViewModel: Geotagged Items
File: `viewmodel/GalleryViewModel.kt`
- Tambah `_geotaggedItems: MutableStateFlow<List<MediaItem>>`
- Baca EXIF di background (Dispatchers.IO) setelah loadMedia
- Expose: `val geotaggedItems: StateFlow<List<MediaItem>>`
- Tambah method: `fun loadGeotaggedItems(items: List<MediaItem>)`

---

## BLOK C — MapsScreen (FILE BARU)
File: `ui/screens/MapsScreen.kt`
- OSMDroid map view
- Marker tiap foto bergeotag
- Panel bawah: thumbnail foto horizontal scroll
- Jika selectedItemId != null → animateTo lokasi itu
- Style switcher: Standard/Topo/Satelit
- Signature:
  fun MapsScreen(
      geotaggedItems: List<MediaItem>,
      selectedItemId: Long? = null,
      onMediaClick: (MediaItem) -> Unit,
  )

---

## BLOK B — PhotosScreen Dinamis
File: `ui/screens/PhotosScreen.kt`
- Hapus LargeTopAppBar
- Header compact: judul "Foto" + tanggal singkat (dd MMM) minimize saat scroll
- "Menyiapkan..." indicator (kecil, di kiri atas) saat isLoading
- LazyVerticalStaggeredGrid untuk layout masonry dinamis:
  - Landscape (w>h*1.3): span 2
  - Portrait (h>w*1.3): span 1 tapi lebih tinggi
  - Default: square 1:1
- Tanggal header format singkat: "12 Jul" bukan "12 Juli 2026"
- Skeleton shimmer tiles saat loading

Signature:
fun PhotosScreen(
    groupedMediaItems: List<Pair<String, List<MediaItem>>>,
    isLoading: Boolean,
    onMediaClick: (MediaItem) -> Unit,
)

---

## BLOK D-Albums — AlbumsScreen: Album Berlokasi
File: `ui/screens/AlbumsScreen.kt`
- Tampilkan album virtual "Berlokasi" di baris pertama (pinned)
- Ikon: LocationOn, warna hijau/biru
- onClick → callback `onLocationAlbumClick` yang buka MapsScreen
- Jika geotaggedCount == 0, sembunyikan album ini

Signature tambahan:
fun AlbumsScreen(
    albums: List<Album>,
    isLoading: Boolean,
    geotaggedCount: Int,
    onAlbumClick: (Album) -> Unit,
    onLocationAlbumClick: () -> Unit,
)

---

## BLOK A — GalleryApp: Floating Pill Navigation (TERAKHIR)
File: `ui/GalleryApp.kt`
- HAPUS NavigationBar lama
- Buat FloatingPill composable:
  - Box(fillMaxSize) → content di belakang, pill di depan align BottomCenter
  - Pill: RoundedCornerShape(50), background dark/light semi-transparan, shadow
  - Items: [ 📷 Foto ] [ ▦ Koleksi ] [ 🔍 ] [ 🗺 Peta ] [ ⋯ ]
  - Tab Cari (Search): HANYA ikon, tanpa label
  - Tab aktif: highlighted, label muncul
  - Tab non-aktif saat scroll: hanya ikon (animasi width collapse)
  - "..." dropdown: Sort (Hari/Minggu/Bulan/Tahun) + Buka Setelan + Favorit
- Layar yang ditampilkan (AnimatedContent):
  - 0 = PhotosScreen
  - 1 = AlbumsScreen (dengan geotaggedCount)
  - 2 = SearchScreen (inline, tanpa push nav)
  - 3 = MapsScreen
- Settings/Favorites → tetap push di NavHost
- MediaPreview: onMapClick → selectedMapItemId state → switch ke tab Peta
- Pill tidak mendorong konten (content fills screen, pill overlay)
- WindowInsets navigation bars padding di bawah pill

---

## Catatan Teknis
- osmdroid: pastikan `Configuration.getInstance().userAgentValue = context.packageName` sebelum map init
- Coil3 disk cache: `ImageRequest.Builder().diskCachePolicy(CachePolicy.ENABLED)`
- Staggered grid: `LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Fixed(3))`
- Floating pill z-order: `Box { Content(); Pill(align=BottomCenter) }`
- Geotagged read mahal → lakukan di background, update progressively

---

## Urutan Implementasi (no conflicts)
Wave 1 (paralel): F, E, D-VM, C
Wave 2 (setelah Wave 1): B, D-Albums
Wave 3 (terakhir, mandiri): A

---

## Commit Akhir
```
git add -A
git commit -m "feat: floating pill nav, dynamic masonry grid, maps tab, geotagged album, optimized thumbnails"
git push
```
