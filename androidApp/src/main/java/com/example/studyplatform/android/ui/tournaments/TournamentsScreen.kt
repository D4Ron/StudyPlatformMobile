package com.example.studyplatform.android.ui.tournaments

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.TournamentApi
import com.example.studyplatform.model.TournamentResponse
import kotlinx.coroutines.launch
import tg.edunova.app.R

/**
 * The tournaments a student can see.
 *
 * Online-only by design — see `TournamentApi`. On failure this says so plainly rather
 * than showing an empty list, because "no tournaments exist" and "we could not ask"
 * are very different answers and only one of them means stop waiting.
 */
@Composable
fun TournamentsScreen(onOpen: (String) -> Unit = {}) {
    var items by remember { mutableStateOf<List<TournamentResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        runCatching { TournamentApi.listVisible() }
            .onSuccess { items = it; error = null }
            .onFailure { error = it.message ?: "…" }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    if (loading) { LoadingScreen(stringResource(R.string.common_loading)); return }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp)
        ) {
            item {
                AnimatedEntry {
                    Text(
                        stringResource(R.string.tournaments_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                }
            }

            error?.let { message ->
                item {
                    AnimatedEntry(index = 1) {
                        Column {
                            StatusBanner(
                                message = stringResource(R.string.tournaments_online_only),
                                background = WarningLight,
                                contentColor = Warning
                            )
                            Spacer(Modifier.height(10.dp))
                            ErrorMessage(message) { scope.launch { loading = true; load() } }
                        }
                    }
                }
            }

            if (error == null && items.isEmpty()) {
                item {
                    AnimatedEntry(index = 1) {
                        CenteredContent {
                            Text(
                                stringResource(R.string.tournaments_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            items(items, key = { it.id }) { t ->
                val interaction = remember { MutableInteractionSource() }
                Card(
                    onClick = { onOpen(t.id) },
                    interactionSource = interaction,
                    modifier = Modifier.fillMaxWidth().pressScale(interaction),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EmojiEvents, null, tint = Warning)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                t.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (t.joined) {
                                Text(
                                    stringResource(R.string.tournaments_joined),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Success,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SuccessLight)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        t.description?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.tournaments_questions, t.questionCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Text(" · ", color = TextMuted)
                            Text(
                                stringResource(R.string.tournaments_participants, t.participantCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            if (t.teamsAllowed) {
                                Spacer(Modifier.width(10.dp))
                                Icon(Icons.Filled.Group, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
