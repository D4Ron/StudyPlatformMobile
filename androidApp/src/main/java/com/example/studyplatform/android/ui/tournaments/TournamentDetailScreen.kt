package com.example.studyplatform.android.ui.tournaments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.studyplatform.model.LeaderboardEntryResponse
import com.example.studyplatform.model.TournamentResponse
import kotlinx.coroutines.launch
import tg.edunova.app.R

/**
 * One tournament: what it is, whether you are in it, and who is winning.
 *
 * Joining re-reads the tournament rather than flipping `joined` locally — the server
 * decides eligibility (group membership, whether it has closed), and a button that
 * lies about having worked is the failure people remember.
 */
@Composable
fun TournamentDetailScreen(tournamentId: String, onBack: () -> Unit) {
    var tournament by remember { mutableStateOf<TournamentResponse?>(null) }
    var board by remember { mutableStateOf<List<LeaderboardEntryResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var joining by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        runCatching {
            tournament = TournamentApi.getById(tournamentId)
            board = TournamentApi.leaderboard(tournamentId)
            error = null
        }.onFailure { error = it.message ?: "…" }
        loading = false
    }

    LaunchedEffect(tournamentId) { load() }

    if (loading) { LoadingScreen(stringResource(R.string.common_loading)); return }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.common_back), tint = TextPrimary)
                    }
                    Text(
                        tournament?.title.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                }
            }

            error?.let { message ->
                item { ErrorMessage(message) { scope.launch { loading = true; load() } } }
            }

            tournament?.let { t ->
                item {
                    AnimatedEntry(index = 1) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                t.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Spacer(Modifier.height(12.dp))
                                }
                                Row {
                                    Text(
                                        stringResource(R.string.tournaments_questions, t.questionCount),
                                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                                    )
                                    Text(" · ", color = TextMuted)
                                    Text(
                                        stringResource(R.string.tournaments_participants, t.participantCount),
                                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                                    )
                                }
                                if (t.teamsAllowed) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        stringResource(R.string.tournaments_teams_allowed),
                                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                // Says "Joined" and stops there rather than "Compete":
                                // the answering screen does not exist yet, and a button
                                // that goes nowhere is worse than one that plainly says
                                // you are already in.
                                PrimaryButton(
                                    text = stringResource(
                                        if (t.joined) R.string.tournaments_joined
                                        else R.string.tournaments_join
                                    ),
                                    loading = joining,
                                    enabled = !t.joined && !joining,
                                    onClick = {
                                        joining = true
                                        scope.launch {
                                            runCatching { TournamentApi.joinSolo(t.id) }
                                                .onFailure { error = it.message ?: "…" }
                                            load()
                                            joining = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.tournaments_leaderboard),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            if (board.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.tournaments_leaderboard_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            items(board, key = { it.participantId ?: "${it.rank}-${it.displayName}" }) { row ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape)
                                .background(if (row.rank <= 3) WarningLight else Background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${row.rank}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (row.rank <= 3) Warning else TextMuted
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                row.teamName ?: row.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                stringResource(R.string.tournaments_answered, row.answered),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Text(
                            "${row.score}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }
        }
    }
}
