# Audit Codebase — Gallery App

> Static analysis (baca kode saja, tanpa build/gradle). Scope: seluruh module `:app`.
> Tanggal audit: 2026-07-08 · Branch: `dev`
>
> **Catatan:** Temuan dikelompokkan per severity. Belum ada kode yang diubah — menunggu review.

---

## Ringkasan Versi Library (terpasang vs terbaru)

| Library | Terpasang | Terbaru (Jul 2026) | Catatan |
|---|---|---|---|
| `compileSdk` | **37** | 36 (Android 16, stabil) | API 37 belum rilis stabil → lihat #5 |
| AGP | 9.2.0 | 9.x | OK |
| Kotlin | 2.4.0 | 2.x | OK |
| Coil | **3.1.0** | 3.5.0 | outdated + artifact `coil-video` hilang → lihat #1 |
| Media3 ExoPlayer/UI | **1.4.1** | 1.10.1 | outdated, banyak bugfix → lihat #8 |
| navigation-compose | 2.9.8 | ~2.9.x | OK |
| lifecycle | 2.11.0 | ~2.11.x | OK |

---

## CRITICAL / HIGH

### #1 — Thumbnail & cover video tidak akan pernah muncul (Coil 3 tanpa `coil-video`) — HIGH
- **File:** `gradle/libs.versions.toml:35`, `app/build.gradle.kts:73`, `app/src/main/kotlin/com/gallery/app/ui/components/MediaThumbnail.kt:30-41` & `61-72`
- **Masalah:** `MediaThumbnail`/`AlbumCoverThumbnail` memuat `item.uri` (termasuk URI **video** `content://.../Video/Media/...`) langsung ke Coil. Project hanya menyertakan `io.coil-kt.coil3:coil-compose`, tanpa `io.coil-kt.coil3:coil-video`. Coil 3 **tidak** bisa men-decode frame video tanpa mendaftarkan `VideoFrameDecoder.Factory()`.
- **Efek:** Semua item video di grid Foto/Album/Favorit/Search gagal decode → jatuh ke branch `error = { ShimmerBox(...) }`. Cover album yang item terbarunya video juga blank. (Lihat juga #2 yang membuat kegagalan ini terlihat seperti loading tak berujung.)
- **Sumber:** [Coil — Video Frames](https://coil-kt.github.io/coil/videos/) · [coil-video README](https://github.com/coil-kt/coil/blob/main/coil-video/README.md)
- **Saran fix:** Tambah dependency `io.coil-kt.coil3:coil-video`, buat custom `ImageLoader` (via `SingletonImageLoader.setSafe { ... }` atau Application) yang `.components { add(VideoFrameDecoder.Factory()) }`. Sekalian naikkan Coil ke 3.5.0.

### #2 — Placeholder `error` memakai `ShimmerBox` → kegagalan load terlihat seperti loading selamanya — HIGH (UX)
- **File:** `app/src/main/kotlin/com/gallery/app/ui/components/MediaThumbnail.kt:39-40` & `70-71`
- **Masalah:** `loading` dan `error` sama-sama render `ShimmerBox`. Item yang **gagal** dimuat (mis. semua video karena #1, file korup, URI dicabut) akan berkedip shimmer tanpa henti, tak bisa dibedakan dari yang sedang loading.
- **Efek:** User mengira app hang; tidak ada indikasi error/retry.
- **Saran fix:** Gunakan visual berbeda untuk `error` (mis. ikon broken-image di atas background solid), pisahkan dari state `loading`.

### #3 — App terkunci permanen di layar izin pada Android 14+ (partial access) & bila `ACCESS_MEDIA_LOCATION` ditolak — HIGH
- **File:** `app/src/main/kotlin/com/gallery/app/ui/components/PermissionScreen.kt:31-41`, `app/src/main/kotlin/com/gallery/app/ui/GalleryApp.kt:57-78` (khususnya `results.values.all { it }` di baris 68 dan `requiredPermissions.all { ... }` di baris 59)
- **Masalah:**
  1. `hasPermission` = **semua** izin harus granted. `ACCESS_MEDIA_LOCATION` ikut dimasukkan ke array `requestPermissions` (baris 38-40). Bila user menolak izin lokasi (yang bukan syarat menampilkan media), `all { it }` = false → app menganggap belum punya izin sama sekali dan mentok di `PermissionScreen`.
  2. Android 14+ punya **partial access**: user bisa memilih "Foto terpilih" yang hanya memberikan `READ_MEDIA_VISUAL_USER_SELECTED`, sedangkan `READ_MEDIA_IMAGES/VIDEO` = denied → `all { it }` = false → terkunci walau sebenarnya sebagian media bisa diakses. Permission `READ_MEDIA_VISUAL_USER_SELECTED` belum dideklarasikan/ditangani sama sekali.
- **Sumber:** [Grant partial access to photos and videos (Android 14)](https://developer.android.com/about/versions/14/changes/partial-photo-video-access) · [Access media files from shared storage](https://developer.android.com/training/data-storage/shared/media)
- **Saran fix:**
  - Hitung `hasPermission` dari izin **media inti saja** (`READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO`, atau `READ_MEDIA_VISUAL_USER_SELECTED` untuk 14+, atau `READ_EXTERNAL_STORAGE` untuk <33). Jangan gate berdasarkan `ACCESS_MEDIA_LOCATION`.
  - Tambahkan `READ_MEDIA_VISUAL_USER_SELECTED` (manifest + request array) untuk API 34+, dan dukung mode partial.
  - `ACCESS_MEDIA_LOCATION` cukup dideklarasikan di manifest & di-request terpisah bila benar-benar butuh EXIF unredacted; jangan menjadi syarat masuk.

---

## MEDIUM

### #4 — Risiko crash `IndexOutOfBoundsException` saat list berubah di preview — MEDIUM
- **File:** `app/src/main/kotlin/com/gallery/app/ui/screens/MediaPreviewScreen.kt:154-156` (`displayList[pagerState.currentPage]`), juga `:176` (`displayList[page]`)
- **Masalah:** `currentItem` dan pager membaca `displayList[currentPage]` secara langsung. Bila `displayList` mengecil saat preview terbuka (mis. `mediaItems`/`favorites` di-refresh, item di-unfavorite, atau MediaStore berubah) sementara `pagerState.currentPage` masih menunjuk index lama, akses index bisa out-of-bounds.
- **Efek:** Potensi crash. Probabilitas kini rendah karena `loadMedia()` hanya dipanggil sekali, tapi menjadi nyata begitu ada refresh/observer.
- **Saran fix:** `displayList.getOrNull(pagerState.currentPage)` + guard null, atau `coerceIn(0, lastIndex)` sebelum indexing.

### #5 — ~~`compileSdk = 37` belum tersedia sebagai rilis stabil~~ (DIKOREKSI) — targetSdk dinaikkan ke 36
- **File:** `app/build.gradle.kts:8,13`
- **Koreksi:** Dugaan awal bahwa `compileSdk 37` tidak valid **SALAH**. CI (`checkDebugAarMetadata`) membuktikan dependency `androidx.core:core:1.19.0` dan `androidx.lifecycle:*:2.11.0` **mewajibkan** `compileSdk 37`. Menurunkan ke 36 justru mem-break build. Jadi `compileSdk = 37` **dipertahankan**.
- **Yang tetap dikerjakan:** `targetSdk 35 → 36` (Play mewajibkan target 36 mulai 31 Agustus 2026). Ini valid karena `compileSdk (37) >= targetSdk (36)`.
- **Pelajaran:** Info web (US-only) soal ketersediaan SDK bisa tertinggal; constraint nyata dari graph dependency yang menentukan.

### #6 — Nama album tidak di-encode di path rute navigasi → mismatch/crash — MEDIUM
- **File:** `app/src/main/kotlin/com/gallery/app/ui/navigation/AppNavigation.kt:26-28` (`"album_detail/{albumId}/{albumName}"` + `createRoute`)
- **Masalah:** `albumName` (dari `bucketName` MediaStore, bisa mengandung spasi, `/`, `?`, `#`, `&`) disisipkan mentah ke **path segment**. Karakter `/` memecah segment; karakter khusus lain merusak pola rute.
- **Efek:** Navigasi ke album dengan nama berisi karakter tsb gagal match → layar kosong / crash `IllegalArgumentException`.
- **Saran fix:** URL-encode `albumName` saat `createRoute` (`Uri.encode(...)`) dan decode di sisi baca, atau jangan lewatkan nama via route — cukup lewatkan `albumId` lalu resolve nama dari data. (`albumId` numerik relatif aman, tapi sebaiknya diencode juga.)

### #7 — Grouping tanggal seluruh media dijalankan di Main thread — MEDIUM (performa)
- **File:** `app/src/main/kotlin/com/gallery/app/viewmodel/GalleryViewModel.kt:36-40` (`combine(...).stateIn(viewModelScope, ...)`) & `95-118` (`groupMediaItems`)
- **Masalah:** Transform `combine {}` berjalan di dispatcher `viewModelScope` (Main). `groupMediaItems` melakukan konversi `Instant`→`LocalDateTime` + format string untuk **setiap** item, dan membuat 3+ `DateTimeFormatter` tiap invokasi. Dipicu ulang setiap `_mediaItems`/`_dateGrouping` berubah.
- **Efek:** Jank/ANR pada galeri besar (ribuan foto), terutama saat pertama load atau ganti grouping.
- **Saran fix:** Pindahkan komputasi ke `Dispatchers.Default` (mis. `.flowOn(Dispatchers.Default)` sebelum `stateIn`, atau map di dalam coroutine IO). Angkat `DateTimeFormatter` menjadi konstanta (thread-safe) alih-alih dibuat ulang.

### #8 — Media3 1.4.1 tertinggal jauh (terbaru 1.10.1) — MEDIUM
- **File:** `gradle/libs.versions.toml:13`
- **Masalah:** Selisih 6 minor version; banyak perbaikan bug playback, kompatibilitas device, dan keamanan sejak 1.4.1.
- **Sumber:** [Media3 releases](https://github.com/androidx/media/releases) · [Media3 Jetpack page](https://developer.android.com/jetpack/androidx/releases/media3)
- **Saran fix:** Naikkan `media3 = "1.10.1"` (uji regresi `ExoPlayer`/`PlayerView`).

### #9 — Release APK ditandatangani dengan debug key — MEDIUM
- **File:** `app/build.gradle.kts:28` (`signingConfig = signingConfigs.getByName("debug")` di block `release`)
- **Masalah:** Build release memakai keystore debug.
- **Efek:** Tidak bisa diupload ke Play Store; APK release tidak aman/tidak konsisten identitasnya. Bila ini disengaja untuk CI internal, sebaiknya eksplisit.
- **Saran fix:** Buat signing config release sesungguhnya (keystore via secret CI), atau dokumentasikan bahwa artifact ini bukan untuk distribusi.

### #10 — Izin `INTERNET` & `ACCESS_NETWORK_STATE` untuk galeri lokal — MEDIUM (privasi/Play policy)
- **File:** `app/src/main/AndroidManifest.xml:10-11`; dipakai kosmetik di `SettingsScreen.kt:206-225`
- **Masalah:** App galeri lokal mendeklarasikan akses jaringan hanya untuk menampilkan status di Setelan. Menambah permukaan izin & bisa memicu pertanyaan pada review Play.
- **Saran fix:** Hapus bila tidak ada kebutuhan jaringan nyata (Coil memuat URI lokal, tidak perlu INTERNET). Jika status koneksi hanya dekoratif, pertimbangkan hilangkan fitur tsb.

---

## LOW

### #11 — Crash bila kolom MediaStore tak ada di OEM tertentu — LOW
- **File:** `app/src/main/kotlin/com/gallery/app/data/MediaRepository.kt:45-51`
- **Masalah:** `getColumnIndexOrThrow` untuk kolom standar umumnya aman, tapi tanpa try/catch di sekeliling query, error I/O atau kolom tak terduga (custom ROM) langsung meng-crash proses tanpa fallback.
- **Saran fix:** Bungkus query dengan try/catch, log & return list parsial/empty daripada crash.

### #12 — API deprecated: `mediumTopAppBarColors` / `largeTopAppBarColors` — LOW
- **File:** `app/src/main/kotlin/com/gallery/app/ui/screens/AlbumsScreen.kt:62`, `FavoritesScreen.kt:57`, `PhotosScreen.kt:61` (dan pemakaian TopAppBar colors terkait)
- **Sumber:** Warning dari `release_job.log`: *"'fun mediumTopAppBarColors(...)' is deprecated. Use topAppBarColors instead."*
- **Saran fix:** Ganti ke `TopAppBarDefaults.topAppBarColors(...)`.

### #13 — API deprecated: `window.statusBarColor` — LOW
- **File:** `app/src/main/kotlin/com/gallery/app/ui/theme/Theme.kt:51`
- **Masalah:** `statusBarColor` deprecated (SDK 35+); dengan `enableEdgeToEdge()` pengaturan warna status bar sebaiknya lewat mekanisme edge-to-edge. Cast `view.context as Activity` juga rapuh (aman karena dijaga `isInEditMode`, tapi bukan idiom yang disarankan).
- **Saran fix:** Andalkan `enableEdgeToEdge()` + `WindowCompat`/`SystemBarStyle`, hapus set `statusBarColor` manual.

### #14 — `hasPermission` disimpan di `rememberSaveable` — LOW
- **File:** `app/src/main/kotlin/com/gallery/app/ui/GalleryApp.kt:57-63`
- **Masalah:** Dokumentasi menyarankan **tidak** menyimpan state izin (bisa out-of-sync setelah reset izin/hibernasi). Nilai awal memang dihitung ulang saat komposisi baru, tapi menyimpan hasil di saveable berisiko basi setelah process death + pencabutan izin.
- **Sumber:** [Access media files — best practices](https://developer.android.com/training/data-storage/shared/media)
- **Saran fix:** Cek ulang izin via `ContextCompat.checkSelfPermission` pada `ON_RESUME` (lifecycle observer) alih-alih menyimpan boolean.

### #15 — Tidak ada refresh saat MediaStore berubah — LOW
- **File:** `app/src/main/kotlin/com/gallery/app/viewmodel/GalleryViewModel.kt:62-70` (`loadMedia`)
- **Masalah:** `loadMedia()` hanya dipanggil sekali saat izin didapat. Foto/video baru atau perubahan favorit di app lain tidak tercermin sampai app di-restart.
- **Saran fix:** Daftarkan `ContentObserver` pada URI MediaStore dan re-query, atau refresh di `ON_RESUME`. (Catatan: ini juga membuka jalan #4 sehingga guard indexing perlu lebih dulu.)

### #16 — Toggle UI single-tap tertunda ~300ms di preview — LOW (UX)
- **File:** `app/src/main/kotlin/com/gallery/app/ui/screens/MediaPreviewScreen.kt:446-453`
- **Masalah:** Karena deteksi double-tap manual, `onTap()` (toggle top/bottom bar) baru dijalankan setelah `delay(doubleTapTimeout)` untuk memastikan bukan double-tap → single tap terasa lambat.
- **Saran fix:** Wajar untuk membedakan single/double tap, tapi bisa pakai `detectTapGestures(onTap, onDoubleTap)` bawaan yang sudah dioptimasi, atau perpendek timeout.

---

## Informational (bukan bug aktif)

### #17 — Kegagalan R8 di `release_job.log` sudah usang
- **File:** `release_job.log:235` — *"Supplied proguard configuration does not exist: .../app/proguard-rules.pro"*
- **Status:** File `app/proguard-rules.pro` **sekarang sudah ada** (dibuat 07-07 18:43, tepat setelah run gagal 18:43:02). Kegagalan build pada log itu sudah teratasi. Tidak perlu aksi, kecuali memverifikasi keep-rules cukup untuk minify (`isMinifyEnabled=true`). Coil3 & Media3 membawa consumer-rules sendiri, jadi kemungkinan aman.

### #18 — Coil 3.1.0 tertinggal (terbaru 3.5.0)
- **File:** `gradle/libs.versions.toml:12`
- Sudah tercakup solusinya di #1 (naikkan versi sekaligus tambah `coil-video`).
