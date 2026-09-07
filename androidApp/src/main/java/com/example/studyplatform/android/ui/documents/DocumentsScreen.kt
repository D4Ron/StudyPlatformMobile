package com.example.studyplatform.android.ui.documents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.Offline
import com.example.studyplatform.model.DocumentResponse
import tg.edunova.app.R

/**
 * The document list, metadata only.
 *
 * The files themselves are deliberately not downloaded here — see
 * `LibraryRepository.documents`. What this screen can honestly show offline is which
 * documents exist and what the server already summarised about them.
 */
@Composable
fun DocumentsScreen() {
    var state by remember { mutableStateOf<Offline<List<DocumentResponse>>?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        state = AppData.library.documents()
        loading = false
    }

    if (loading) { LoadingScreen(stringResource(R.string.common_loading)); return }

    val docs = state?.value.orEmpty()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp)
        ) {
            item {
                AnimatedEntry {
                    Column {
                        Text(
                            stringResource(R.string.documents_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                        state?.let { if (it.fromCache) { Spacer(Modifier.height(10.dp)); OfflineBanner(it) } }
                    }
                }
            }

            if (docs.isEmpty()) {
                item {
                    AnimatedEntry(index = 1) {
                        CenteredContent {
                            Text(
                                stringResource(R.string.documents_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            items(docs, key = { it.id }) { doc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconFor(doc.contentType), null, tint = Primary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(doc.filename, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                humanSize(doc.fileSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            doc.groupName?.let {
                                Text(
                                    stringResource(R.string.documents_group, it),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            doc.summary?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(6.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun iconFor(contentType: String): ImageVector = when {
    contentType.contains("pdf") -> Icons.Filled.PictureAsPdf
    contentType.startsWith("image/") -> Icons.Filled.Image
    contentType.contains("sheet") || contentType.contains("csv") -> Icons.Filled.TableChart
    else -> Icons.Filled.Description
}

/** Bytes, in the units a person reads. Decimal units, matching what file managers show. */
private fun humanSize(bytes: Long): String = when {
    bytes <= 0L -> "—"
    bytes < 1_000L -> "$bytes B"
    bytes < 1_000_000L -> "${bytes / 1_000} KB"
    bytes < 1_000_000_000L -> "${bytes / 1_000_000} MB"
    else -> "${bytes / 1_000_000_000} GB"
}
