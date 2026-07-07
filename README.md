# Gallery — Android App

Aplikasi Android native dibangun dengan **Kotlin + Jetpack Compose + Material 3**.

## Struktur Project

```
Gallery/
├── app/
│   └── src/
│       ├── main/
│       │   ├── kotlin/com/gallery/app/
│       │   │   ├── MainActivity.kt
│       │   │   └── ui/
│       │   │       ├── GalleryApp.kt        ← Root composable
│       │   │       └── theme/
│       │   │           ├── Theme.kt
│       │   │           ├── Color.kt
│       │   │           └── Type.kt
│       │   ├── res/
│       │   └── AndroidManifest.xml
│       └── test/
├── gradle/
│   ├── libs.versions.toml                   ← Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
├── .github/
│   └── workflows/
│       └── android.yml                      ← CI/CD
├── build.gradle.kts
└── settings.gradle.kts
```

## CI/CD (GitHub Actions)

Pipeline otomatis berjalan saat:
- **Push ke `main` / `develop`**
- **Pull Request ke `main`**

| Job | Trigger | Output |
|-----|---------|--------|
| Lint & Unit Tests | Semua push/PR | Laporan lint + test |
| Build Debug APK | Setelah test lulus | `app-debug.apk` (7 hari) |
| Build Release APK | Push ke `main` saja | `app-release.apk` (30 hari) |

### Signing Release (opsional)

Tambahkan secrets di GitHub repo → Settings → Secrets:

```
KEYSTORE_BASE64   ← base64-encoded .jks file
KEY_ALIAS         ← alias kunci
KEY_PASSWORD      ← password kunci
STORE_PASSWORD    ← password keystore
```

Lalu uncomment bagian signing di `.github/workflows/android.yml`.

## Tech Stack

- **Kotlin** 2.1.0
- **Jetpack Compose** (BOM 2025.02.00)
- **Material 3**
- **Gradle** 8.10.2 (Kotlin DSL)
- **Min SDK** 26 (Android 8.0)
- **Target SDK** 35 (Android 15)
