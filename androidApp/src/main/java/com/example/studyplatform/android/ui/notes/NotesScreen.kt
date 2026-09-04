package com.example.studyplatform.android.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.sync.SyncWorker
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.NoteRepository
import kotlinx.coroutines.launch

/**
 * Notes, offline-first.
 *
 * Every read and write here goes to the device database. Nothing on this screen waits on
 * the network, and nothing on it can fail because the network is absent — a note written
 * with no signal is saved, listed, and editable immediately, and reaches the server
 * whenever the phone next has a connection.
 *
 * Rows still waiting to reach the server carry a mark, so the state is visible rather
 * than something the user has to trust.
 */
@Composable
fun NotesScreen() {
    val context = LocalContext.current
    val repository = remember { AppData.notes }
    val notes by repository.observe().collectAsState(initial = emptyList())

    var showCreate by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var syncing by remember { mutableStateOf(false) }
    var syncNote by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val pending = notes.count { it.pending }

    fun closeForm() {
        showCreate = false; editingId = null; title = ""; content = ""
    }

    fun runSync() {
        syncing = true
        scope.launch {
            val outcome = AppData.syncEngine.sync()
            syncNote = when {
                outcome.failed -> "Saved on this device. It will upload when you're back online."
                outcome.conflicts > 0 -> "${outcome.conflicts} note(s) were updated elsewhere and that version was kept."
                outcome.pushed > 0 -> "${outcome.pushed} change(s) uploaded."
                else -> null
            }
            syncing = false
        }
    }

    // A pull on opening, so the list is current when there is a connection and simply
    // unchanged when there is not.
    LaunchedEffect(Unit) { runSync() }

    val spin by animateFloatAsState(
        targetValue = if (syncing) 360f else 0f,
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "sync-spin"
    )

    Scaffold(
        containerColor = Background,
        floatingActionButton = {
            AnimatedVisibility(
                visible = !showCreate,
                enter = Motion.enter(),
                exit = Motion.exit()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { closeForm(); showCreate = true },
                    containerColor = Primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(stringResource(tg.edunova.app.R.string.notes_new), style = MaterialTheme.typography.labelLarge) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp)
            ) {
                item {
                    AnimatedEntry {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(tg.edunova.app.R.string.notes_title),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    if (pending > 0) "${notes.size} notes · $pending waiting to upload"
                                    else "${notes.size} notes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            IconButton(onClick = {
                                runSync()
                                SyncWorker.syncNow(context)
                            }) {
                                Icon(
                                    if (pending > 0) Icons.Default.CloudOff else Icons.Default.Sync,
                                    "Sync now",
                                    tint = if (pending > 0) Warning else TextMuted,
                                    modifier = Modifier.rotate(spin)
                                )
                            }
                        }
                    }
                }

                item {
                    StatusBanner(
                        message = syncNote,
                        background = PrimaryLight,
                        contentColor = Primary
                    )
                }

                item {
                    AnimatedVisibility(
                        visible = showCreate,
                        enter = Motion.expand(),
                        exit = Motion.collapse()
                    ) {
                        NoteForm(
                            title = title,
                            content = content,
                            onTitle = { title = it },
                            onContent = { content = it },
                            isEdit = editingId != null,
                            onSave = {
                                val id = editingId
                                val savedTitle = title
                                val savedContent = content
                                // The form closes immediately — the write is local and
                                // the upload is the sync engine's problem, not the
                                // user's, so there is nothing here worth waiting on.
                                closeForm()
                                scope.launch {
                                    if (id != null) {
                                        repository.save(
                                            id = id,
                                            title = savedTitle,
                                            content = savedContent
                                        )
                                    } else {
                                        repository.save(
                                            title = savedTitle,
                                            content = savedContent
                                        )
                                    }
                                    SyncWorker.syncNow(context)
                                }
                            },
                            onCancel = { closeForm() }
                        )
                    }
                }

                if (notes.isEmpty() && !showCreate) {
                    item {
                        AnimatedEntry(index = 1) {
                            Card(
                                Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = PrimaryLight),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(Modifier.padding(24.dp)) {
                                    Text(
                                        stringResource(tg.edunova.app.R.string.notes_none),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        stringResource(tg.edunova.app.R.string.notes_none_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
                    AnimatedEntry(index = index) {
                        NoteCard(
                            note = note,
                            onEdit = {
                                editingId = note.id
                                title = note.title
                                content = note.content
                                showCreate = true
                            },
                            onDelete = {
                                scope.launch {
                                    repository.delete(note.id)
                                    SyncWorker.syncNow(context)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteForm(
    title: String,
    content: String,
    onTitle: (String) -> Unit,
    onContent: (String) -> Unit,
    isEdit: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                if (isEdit) stringResource(tg.edunova.app.R.string.notes_edit) else stringResource(tg.edunova.app.R.string.notes_new),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitle,
                label = { Text(stringResource(tg.edunova.app.R.string.notes_note_title)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border
                )
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = content,
                onValueChange = onContent,
                label = { Text(stringResource(tg.edunova.app.R.string.notes_content)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border
                )
            )
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryButton(
                    text = stringResource(tg.edunova.app.R.string.common_save),
                    enabled = title.isNotBlank() && content.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    onClick = onSave
                )
                TextButton(onClick = onCancel) {
                    Text(stringResource(tg.edunova.app.R.string.common_cancel), color = TextMuted, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteRepository.LocalNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }

    Card(
        onClick = onEdit,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().pressScale(interaction),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    note.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete, "Delete",
                        tint = TextMuted, modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                note.content.take(140) + if (note.content.length > 140) "…" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Row(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.sharedWithGroup) {
                    Text(
                        stringResource(tg.edunova.app.R.string.notes_shared_with_group),
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary
                    )
                }
                // Not an error state — just the truth about where this note currently
                // lives, so nobody wonders whether their work is safe.
                AnimatedVisibility(
                    visible = note.pending,
                    enter = Motion.enter(),
                    exit = Motion.exit()
                ) {
                    Text(
                        stringResource(tg.edunova.app.R.string.notes_saved_locally),
                        style = MaterialTheme.typography.labelSmall,
                        color = Warning
                    )
                }
            }
        }
    }
}
