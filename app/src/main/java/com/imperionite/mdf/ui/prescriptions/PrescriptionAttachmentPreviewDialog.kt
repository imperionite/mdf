package com.imperionite.mdf.ui.prescriptions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AttachmentPreviewResult(
    val bitmap: Bitmap?,
    val pageCount: Int = 0,
    val error: String? = null
)

@Composable
fun PrescriptionAttachmentPreviewDialog(
    uri: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var isLoading by remember(uri) {
        mutableStateOf(true)
    }
    var previewResult by remember(uri) {
        mutableStateOf<AttachmentPreviewResult?>(null)
    }

    LaunchedEffect(uri) {
        isLoading = true
        previewResult = null

        previewResult = withContext(Dispatchers.IO) {
            loadAttachmentPreview(context, Uri.parse(uri))
        }

        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Doctor's Prescription")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    previewResult?.bitmap != null -> {
                        Column {
                            Image(
                                bitmap = previewResult!!.bitmap!!.asImageBitmap(),
                                contentDescription =
                                    "Doctor's prescription preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 460.dp),
                                contentScale = ContentScale.Fit
                            )

                            if (previewResult!!.pageCount > 0) {
                                Text(
                                    text = "PDF preview: page 1 of " +
                                            previewResult!!.pageCount,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = previewResult?.error
                                ?: "The attachment could not be previewed.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun loadAttachmentPreview(
    context: Context,
    uri: Uri
): AttachmentPreviewResult {
    return try {
        val mimeType = context.contentResolver.getType(uri)
        val isPdf = mimeType.equals(
            "application/pdf",
            ignoreCase = true
        ) || uri.lastPathSegment
            ?.substringAfterLast('.', "")
            ?.equals("pdf", ignoreCase = true) == true

        if (isPdf) {
            loadPdfFirstPage(context, uri)
        } else {
            val bitmap = context.contentResolver
                .openInputStream(uri)
                ?.use { input ->
                    BitmapFactory.decodeStream(input)
                }

            if (bitmap != null) {
                AttachmentPreviewResult(bitmap = bitmap)
            } else {
                AttachmentPreviewResult(
                    bitmap = null,
                    error = "This image could not be displayed. " +
                            "The file may be unavailable or unsupported."
                )
            }
        }
    } catch (exception: Exception) {
        AttachmentPreviewResult(
            bitmap = null,
            error = "Unable to open the attachment. " +
                    "The original file may have been moved, deleted, " +
                    "or become inaccessible."
        )
    }
}

private fun loadPdfFirstPage(
    context: Context,
    uri: Uri
): AttachmentPreviewResult {
    var fileDescriptor: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null

    return try {
        fileDescriptor = context.contentResolver
            .openFileDescriptor(uri, "r")
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
            val scale = 2

            Bitmap.createBitmap(
                (page.width * scale).coerceAtLeast(1),
                (page.height * scale).coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            ).also { pageBitmap ->
                page.render(
                    pageBitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
            }
        }

        AttachmentPreviewResult(
            bitmap = bitmap,
            pageCount = pageCount
        )
    } catch (exception: Exception) {
        AttachmentPreviewResult(
            bitmap = null,
            error = "Unable to preview this PDF. " +
                    "Please check that the file is accessible."
        )
    } finally {
        renderer?.close()
        fileDescriptor?.close()
    }
}
