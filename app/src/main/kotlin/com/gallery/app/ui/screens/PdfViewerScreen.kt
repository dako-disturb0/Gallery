package com.gallery.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gallery.app.data.ImageFormat
import com.gallery.app.data.PdfExport
import com.gallery.app.ui.pdf.PdfRendererState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Status pemuatan PDF untuk membedakan loading / siap / gagal. */
private sealed interface PdfLoadState {
    data object Loading : PdfLoadState
    data object Error : PdfLoadState
    data class Ready(val renderer: PdfRendererState) : PdfLoadState
}

/** Resolusi render untuk ekspor Save As (lebih tinggi dari tampilan layar). */
private const val EXPORT_WIDTH = 2200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uri: Uri,
    title: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Buka renderer; tutup otomatis saat layar ditinggalkan (awaitDispose).
    val loadState by produceState<PdfLoadState>(PdfLoadState.Loading, uri) {
        val renderer = withContext(Dispatchers.IO) { PdfRendererState.open(context, uri) }
        value = if (renderer == null) PdfLoadState.Error else PdfLoadState.Ready(renderer)
        awaitDispose { renderer?.close() }
    }

    var uiVisible by remember { mutableStateOf(true) }
    var vertical by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }

    // Mode fullscreen: sembunyikan/ tampilkan system bars mengikuti uiVisible.
    val view = LocalView.current
    DisposableEffect(uiVisible) {
        val window = context.findActivity()?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (uiVisible) controller.show(WindowInsetsCompat.Type.systemBars())
            else controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {}
    }
    // Pulihkan system bars saat keluar dari layar.
    DisposableEffect(Unit) {
        onDispose {
            context.findActivity()?.window?.let { window ->
                WindowInsetsControllerCompat(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    fun saveCurrentPage(format: ImageFormat) {
        val renderer = (loadState as? PdfLoadState.Ready)?.renderer ?: return
        val page = currentPage
        scope.launch {
            val bitmap = renderer.renderPage(page, EXPORT_WIDTH)
            if (bitmap == null) {
                Toast.makeText(context, "Gagal merender halaman", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val saved = PdfExport.saveImage(context, bitmap, "${title}_hal${page + 1}", format)
            Toast.makeText(
                context,
                if (saved != null) "Tersimpan: $saved" else "Gagal menyimpan",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF2B2B2B)) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = loadState) {
                is PdfLoadState.Loading -> {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is PdfLoadState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Tidak dapat membuka PDF",
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                is PdfLoadState.Ready -> {
                    val renderer = state.renderer
                    val pageCount = renderer.pageCount
                    if (vertical) {
                        VerticalPdfContent(
                            renderer = renderer,
                            pageCount = pageCount,
                            onTap = { uiVisible = !uiVisible },
                            onFirstVisiblePageChange = { currentPage = it }
                        )
                    } else {
                        HorizontalPdfContent(
                            renderer = renderer,
                            pageCount = pageCount,
                            onTap = { uiVisible = !uiVisible },
                            onPageChange = { currentPage = it }
                        )
                    }
                }
            }

            // Top bar — hanya tampil saat UI terlihat (bukan fullscreen).
            val ready = loadState as? PdfLoadState.Ready
            if (uiVisible) {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // Ganti arah scroll halaman.
                        IconButton(onClick = { vertical = !vertical }) {
                            Icon(
                                Icons.Outlined.SwapVert,
                                contentDescription = if (vertical) "Ganti ke horizontal" else "Ganti ke vertikal",
                                tint = Color.White
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Menu", tint = Color.White)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                ImageFormat.entries.forEach { format ->
                                    DropdownMenuItem(
                                        text = { Text("Simpan halaman sebagai ${format.label}") },
                                        leadingIcon = { Icon(Icons.Outlined.SaveAlt, contentDescription = null) },
                                        enabled = ready != null,
                                        onClick = {
                                            showMenu = false
                                            saveCurrentPage(format)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Bagikan PDF") },
                                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                                    onClick = { showMenu = false; sharePdf(context, uri) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Buka dengan") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
                                    onClick = { showMenu = false; openPdfWith(context, uri) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.55f)
                    )
                )
            }

            // Indikator halaman.
            if (ready != null && ready.renderer.pageCount > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = "${(currentPage + 1).coerceIn(1, ready.renderer.pageCount)} / ${ready.renderer.pageCount}",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalPdfContent(
    renderer: PdfRendererState,
    pageCount: Int,
    onTap: () -> Unit,
    onFirstVisiblePageChange: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val firstVisible by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    LaunchedEffect(firstVisible) { onFirstVisiblePageChange(firstVisible) }

    val zoom = remember { ZoomState() }
    var viewportWidth by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportWidth = it.width }
            .twoFingerZoom(zoom)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { if (zoom.scale > 1.01f) zoom.reset() else zoom.scale = 2.5f }
                )
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offsetX
                    translationY = zoom.offsetY
                },
            contentPadding = PaddingValues(top = 72.dp, bottom = 40.dp, start = 8.dp, end = 8.dp)
        ) {
            items(count = pageCount, key = { it }) { index ->
                PdfPageImage(
                    renderer = renderer,
                    index = index,
                    widthPx = viewportWidth,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HorizontalPdfContent(
    renderer: PdfRendererState,
    pageCount: Int,
    onTap: () -> Unit,
    onPageChange: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pageCount })
    LaunchedEffect(pagerState.currentPage) { onPageChange(pagerState.currentPage) }

    // Zoom per-halaman; pager swipe dimatikan saat halaman aktif sedang di-zoom.
    val zoomStates = remember { mutableStateMapOf<Int, ZoomState>() }
    fun zoomFor(page: Int) = zoomStates.getOrPut(page) { ZoomState() }
    val currentZoomed = (zoomStates[pagerState.currentPage]?.scale ?: 1f) > 1.01f

    var viewportWidth by remember { mutableIntStateOf(0) }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = !currentZoomed,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportWidth = it.width }
    ) { page ->
        val zoom = zoomFor(page)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .twoFingerZoom(zoom)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { if (zoom.scale > 1.01f) zoom.reset() else zoom.scale = 2.5f }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            PdfPageImage(
                renderer = renderer,
                index = page,
                widthPx = viewportWidth,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom.scale
                        scaleY = zoom.scale
                        translationX = zoom.offsetX
                        translationY = zoom.offsetY
                    }
            )
        }
    }
}

@Composable
private fun PdfPageImage(
    renderer: PdfRendererState,
    index: Int,
    widthPx: Int,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(index, widthPx) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(index, widthPx) {
        if (widthPx > 0) bitmap = renderer.renderPage(index, widthPx)
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Halaman ${index + 1}",
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .then(if (contentScale == ContentScale.FillWidth) Modifier.aspectRatio(1f / 1.414f) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.5f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** State zoom & pan bersama untuk satu area tampilan. */
private class ZoomState {
    var scale by mutableFloatStateOf(1f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var viewportW by mutableIntStateOf(0)
    var viewportH by mutableIntStateOf(0)

    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
}

/**
 * Zoom & pan hanya saat 2 jari — sehingga gestur 1 jari (scroll LazyColumn atau
 * swipe pager) tetap diteruskan ke komponen di bawahnya.
 */
private fun Modifier.twoFingerZoom(state: ZoomState): Modifier = this
    .onSizeChanged {
        state.viewportW = it.width
        state.viewportH = it.height
    }
    .pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } >= 2) {
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    state.scale = (state.scale * zoom).coerceIn(1f, 5f)
                    val maxX = ((state.scale - 1f) * state.viewportW / 2f).coerceAtLeast(0f)
                    val maxY = ((state.scale - 1f) * state.viewportH / 2f).coerceAtLeast(0f)
                    state.offsetX = (state.offsetX + pan.x).coerceIn(-maxX, maxX)
                    state.offsetY = (state.offsetY + pan.y).coerceIn(-maxY, maxY)
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
            if (state.scale <= 1.01f) state.reset()
        }
    }

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun sharePdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Bagikan PDF")) }
}

private fun openPdfWith(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Buka dengan")) }
}
