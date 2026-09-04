package com.example.studyplatform.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.AnimatedEntry
import com.example.studyplatform.android.sync.SyncWorker
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.ApiClient
import com.example.studyplatform.data.AppData
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val firstName = ApiClient.getFirstName() ?: "User"
    val accountType = ApiClient.getAccountType() ?: "STUDENT"
    val scope = rememberCoroutineScope()

    val syncFailedMessage = stringResource(tg.edunova.app.R.string.settings_sync_failed)
    val syncOkMessage = stringResource(tg.edunova.app.R.string.settings_sync_ok)

    var pending by remember { mutableStateOf(0L) }
    var syncing by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }

    suspend fun refreshPending() {
        pending = if (AppData.isReady) AppData.notes.pendingCount() else 0
    }
    LaunchedEffect(Unit) { refreshPending() }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(20.dp))
            AnimatedEntry {
                Text(stringResource(tg.edunova.app.R.string.settings_title), style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            }
            Spacer(Modifier.height(24.dp))

            AnimatedEntry(index = 1) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, Modifier.size(40.dp), tint = Primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(firstName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text(accountType, style = MaterialTheme.typography.labelMedium, color = Secondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Offline status. Worth its own row now that writes land locally first —
            // "is my work safe?" is the question this screen should be able to answer.
            AnimatedEntry(index = 2) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Sync, null, Modifier.size(32.dp),
                            tint = if (pending > 0) Warning else Success
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(tg.edunova.app.R.string.settings_sync), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                lastResult ?: when {
                                    pending > 0L -> "$pending change(s) saved here, waiting to upload"
                                    else -> stringResource(tg.edunova.app.R.string.settings_sync_clean)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (pending > 0) Warning else TextMuted
                            )
                        }
                        TextButton(
                            enabled = !syncing,
                            onClick = {
                                syncing = true
                                scope.launch {
                                    val outcome = AppData.syncEngine.sync()
                                    lastResult =
                                        if (outcome.failed) syncFailedMessage else syncOkMessage
                                    refreshPending()
                                    syncing = false
                                    SyncWorker.syncNow(context)
                                }
                            }
                        ) {
                            Text(
                                if (syncing) stringResource(tg.edunova.app.R.string.settings_syncing) else stringResource(tg.edunova.app.R.string.settings_sync_now),
                                color = Primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedEntry(index = 3) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, null, Modifier.size(32.dp), tint = Primary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(tg.edunova.app.R.string.settings_drive), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                stringResource(tg.edunova.app.R.string.settings_drive_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        // Deliberately inert until Phase 8: the OAuth client does not
                        // exist yet, so a working-looking button would only fail.
                        Text(stringResource(tg.edunova.app.R.string.settings_soon), style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            AnimatedEntry(index = 4) {
                Button(
                    onClick = {
                        scope.launch {
                            // Clears the local database as well as the tokens. The cache
                            // holds this person's guides, notes and results, and on a
                            // phone shared between siblings the next person would
                            // otherwise find all of it.
                            AppData.signOut()
                            onLogout()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentLight, contentColor = Accent),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(tg.edunova.app.R.string.settings_sign_out), style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}
