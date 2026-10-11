package com.imperionite.mdf.ui.prescriptions

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import androidx.compose.runtime.Composable as Composable1
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap

private data class AttachmentPreviewResult(
    val bitmap: Bitmap?,
    val pageCount: Int = 0,
    val error: String? = null
)

@SuppressLint("AutoboxingStateCreation")
@Suppress("FunctionName")
@Composable1
fun PrescriptionAttachmentPreviewDialog(
    uri: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember(uri) { mutableStateOf(true) }
    var previewResult by remember(uri) {
        mutableStateOf<AttachmentPreviewResult?>(null)
    }

    // Keep zoom and pan state tied to this attachment, so a newly opened
    // attachment always starts at its fitted, unzoomed size.
    var scale by remember(uri) { mutableStateOf(1f) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale
        offset = if (newScale <= 1f) {
            Offset.Zero
        } else {
            offset + panChange
        }
    }

    LaunchedEffect(uri) {
        isLoading = true
        previewResult = null
        previewResult = withContext(Dispatchers.IO) {
            loadAttachmentPreview(context, uri.toUri())
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Doctor's Prescription",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text("Close") }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                previewResult?.bitmap != null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clipToBounds()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = previewResult!!.bitmap!!.asImageBitmap(),
                            contentDescription = "Doctor's prescription preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .pointerInput(uri) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1f) {
                                                scale = 1f
                                                offset = Offset.Zero
                                            } else {
                                                scale = 2.5f
                                            }
                                        }
                                    )
                                }
                                .transformable(state = transformState),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Text(
                        text = buildString {
                            append("Pinch to zoom • Drag to move • Double-tap to zoom")
                            if (previewResult!!.pageCount > 0) {
                                append(" • PDF page 1 of ${previewResult!!.pageCount}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            enabled = scale != 1f || offset != Offset.Zero
                        ) {
                            Text("Reset zoom")
                        }
                        TextButton(onClick = onDismiss) { Text("Done") }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = previewResult?.error
                                ?: "The attachment could not be previewed.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                }
            }
        }
    }
}

private fun loadAttachmentPreview(
    context: Context,
    uri: Uri
): AttachmentPreviewResult {
    return try {
        val mimeType = context.contentResolver.getType(uri)
        val isPdf = mimeType.equals("application/pdf", ignoreCase = true) ||
                uri.lastPathSegment
                    ?.substringAfterLast('.', "")
                    ?.equals("pdf", ignoreCase = true) == true

        if (isPdf) {
            loadPdfFirstPage(context, uri)
        } else {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }

            if (bitmap != null) {
                AttachmentPreviewResult(bitmap = bitmap)
            } else {
                AttachmentPreviewResult(
                    bitmap = null,
                    error = "This image could not be displayed. The file may be unavailable or unsupported."
                )
            }
        }
    } catch (_: Exception) {
        AttachmentPreviewResult(
            bitmap = null,
            error = "Unable to open the attachment. The original file may have been moved, deleted, or become inaccessible."
        )
    }
}

@SuppressLint("UseKt", "UseKt", "UseKt")
private fun loadPdfFirstPage(
    context: Context,
    uri: Uri
): AttachmentPreviewResult {
    var fileDescriptor: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null

    return try {
        fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return AttachmentPreviewResult(
                bitmap = null,
                error = "The PDF file could not be opened."
            )

        renderer = PdfRenderer(fileDescriptor)
        if (renderer.pageCount == 0) {
            return AttachmentPreviewResult(
                bitmap = null,
                error = "This PDF does not contain any pages."
            )
        }

        val pageCount = renderer.pageCount
        val bitmap = renderer.openPage(0).use { page ->
            // Render larger than the original preview to improve readability
            // when users zoom in. The bitmap is still limited to page one.
            val scale = 3
            createBitmap(max(1, page.width * scale), max(1, page.height * scale)).also { pageBitmap ->
                page.render(
                    pageBitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
            }
        }

        AttachmentPreviewResult(bitmap = bitmap, pageCount = pageCount)
    } catch (_: Exception) {
        AttachmentPreviewResult(
            bitmap = null,
            error = "Unable to preview this PDF. Please check that the file is accessible."
        )
    } finally {
        renderer?.close()
        fileDescriptor?.close()
    }
}
