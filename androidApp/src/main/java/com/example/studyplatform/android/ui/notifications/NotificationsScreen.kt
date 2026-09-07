package com.example.studyplatform.android.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.NotificationApi
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.Offline
import com.example.studyplatform.model.NotificationResponse
import kotlinx.coroutines.launch
import tg.edunova.app.R

/**
 * What happened while the student was away.
 *
 * Read state is written straight through to the server rather than queued: an unread
 * count that clears offline and then comes back on the next sync teaches people to
 * distrust the badge. Offline, the rows still render from cache and simply do not
 * respond to a tap.
 */
@Composable
fun NotificationsScreen(onOpenGroup: (String) -> Unit = {}) {
    var items by remember { mutableStateOf<List<NotificationResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var state by remember { mutableStateOf<Offline<List<NotificationResponse>>?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        val result = AppData.library.notifications()
        state = result
        items = result.value.sortedByDescending { it.createdAt ?: "" }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    if (loading) { LoadingScreen(stringResource(R.string.common_loading)); return }

    val unread = items.count { !it.read }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp)
        ) {
            item {
                AnimatedEntry {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.notifications_title),
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            // Offline, marking everything read would be a promise the
                            // app cannot keep, so the action is simply absent.
                            if (unread > 0 && state?.fromCache != true) {
                                TextButton(onClick = {
                                    scope.launch {
                                        runCatching { NotificationApi.markAllRead() }
                                            .onSuccess { load() }
                                    }
                                }) {
                                    Icon(Icons.Filled.DoneAll, null, tint = Primary)
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.notifications_mark_all), color = Primary)
                                }
                            }
                        }
                        state?.let { if (it.fromCache) { Spacer(Modifier.height(10.dp)); OfflineBanner(it) } }
                    }
                }
            }

            if (items.isEmpty()) {
                item {
                    AnimatedEntry(index = 1) {
                        CenteredContent {
                            Text(
                                stringResource(R.string.notifications_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            items(items, key = { it.id }) { n ->
                val interaction = remember { MutableInteractionSource() }
                Card(
                    onClick = {
                        scope.launch {
                            if (!n.read) {
                                runCatching { NotificationApi.markRead(n.id) }.onSuccess { load() }
                            }
                            // linkPath comes from the server. Rather than navigate to it
                            // verbatim, only the destination the payload can justify is
                            // offered — a group it names.
                            n.groupId?.let(onOpenGroup)
                        }
                    },
                    interactionSource = interaction,
                    modifier = Modifier.fillMaxWidth().pressScale(interaction),
                    colors = CardDefaults.cardColors(
                        containerColor = if (n.read) Surface else PrimaryLight
                    ),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape)
                                .background(if (n.read) Color.Transparent else Primary)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                n.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = if (n.read) FontWeight.Normal else FontWeight.SemiBold
                            )
                            if (n.message.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(n.message, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
