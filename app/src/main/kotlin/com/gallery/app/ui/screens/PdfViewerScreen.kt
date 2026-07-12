package com.gallery.app.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gallery.app.ui.pdf.PdfRendererState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Status pemuatan PDF untuk membedakan loading / siap / gagal. */
private sealed interface PdfLoadState {
    data object Loading : PdfLoadState
    data object Error : PdfLoadState
    data class Ready(val renderer: PdfRendererState) : PdfLoadState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uri: Uri,
    title: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current

    val loadState by produceState<PdfLoadState>(PdfLoadState.Loading, uri) {
        value = withContext(Dispatchers.IO) {
            when (val renderer = PdfRendererState.open(context, uri)) {
                null -> PdfLoadState.Error
                else -> PdfLoadState.Ready(renderer)
            }
        }
    }

    // Tutup renderer saat state berganti / layar ditinggalkan.
    DisposableEffect(loadState) {
        onDispose {
            (loadState as? PdfLoadState.Ready)?.renderer?.close()
        }
    }

    val listState = rememberLazyListState()
    val currentPage by remember {
        androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex }
    }

    // Zoom & pan bersama untuk seluruh kolom halaman.
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var viewportWidth by remember { mutableStateOf(0) }
    var viewportHeight by remember { mutableStateOf(0) }

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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.layout.Column(
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
                }

                is PdfLoadState.Ready -> {
                    val renderer = state.renderer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged {
                                viewportWidth = it.width
                                viewportHeight = it.height
                            }
                            // Zoom hanya saat 2 jari agar scroll vertikal 1 jari tetap jalan.
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        if (event.changes.count { it.pressed } >= 2) {
                                            val zoom = event.calculateZoom()
                                            val pan = event.calculatePan()
                                            scale = (scale * zoom).coerceIn(1f, 5f)
                                            val maxX = ((scale - 1f) * viewportWidth / 2f).coerceAtLeast(0f)
                                            val maxY = ((scale - 1f) * viewportHeight / 2f).coerceAtLeast(0f)
                                            offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                            offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                                            event.changes.forEach { it.consume() }
                                        }
                                    } while (event.changes.any { it.pressed })
                                    if (scale <= 1.01f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1.01f) {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            scale = 2.5f
                                        }
                                    }
                                )
                            }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                },
                            contentPadding = PaddingValues(
                                top = 72.dp,
                                bottom = 24.dp,
                                start = 8.dp,
                                end = 8.dp
                            )
                        ) {
                            items(
                                count = renderer.pageCount,
                                key = { it }
                            ) { index ->
                                PdfPage(
                                    renderer = renderer,
                                    index = index,
                                    widthPx = viewportWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Top bar mengambang di atas konten.
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
                    IconButton(onClick = { sharePdf(context, uri) }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Bagikan", tint = Color.White)
                    }
                    IconButton(onClick = { openPdfWith(context, uri) }) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = "Buka dengan", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.55f)
                )
            )

            // Indikator halaman.
            val ready = loadState as? PdfLoadState.Ready
            if (ready != null && ready.renderer.pageCount > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = "${(currentPage + 1).coerceAtMost(ready.renderer.pageCount)} / ${ready.renderer.pageCount}",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfPage(
    renderer: PdfRendererState,
    index: Int,
    widthPx: Int,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(index, widthPx) { mutableStateOf<Bitmap?>(null) }

    androidx.compose.runtime.LaunchedEffect(index, widthPx) {
        if (widthPx > 0) {
            bitmap = renderer.renderPage(index, widthPx)
        }
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Halaman ${index + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = modifier
        )
    } else {
        // Placeholder rasio A4 sambil menunggu render.
        Box(
            modifier = modifier
                .aspectRatio(1f / 1.414f)
                .background(Color.White.copy(alpha = 0.06f)),
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

private fun sharePdf(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Bagikan PDF")) }
}

private fun openPdfWith(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Buka dengan")) }
}
