package com.example.studyplatform.ui.screens.quizzes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.ui.components.*
import com.example.studyplatform.ui.theme.*
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.Offline
import com.example.studyplatform.model.QuizResponse
import studyplatform.shared.generated.resources.*

/**
 * A quiz, read the other way round.
 *
 * Built from a quiz the student already has rather than from a new generation: it costs
 * no API call, works from the same cache the quiz list uses, and turns content they have
 * already paid for into a second way to study it. The offline-first point matters most
 * here — this is the screen someone opens on a bus.
 *
 * Self-rated, and only two buttons. Grading yourself out of five invites deliberation
 * about the rating instead of about the answer; "I knew it" or "review again" is the
 * only distinction that changes what happens next.
 *
 * Cards marked for review come back in the next round, so a session ends when the deck
 * is empty rather than after a fixed number of cards. That is the whole of the spacing
 * logic, deliberately: a real scheduler needs per-card history that would have to sync,
 * and this is worth having before that exists.
 */
@Composable
fun FlashcardScreen(quizId: String, onBack: () -> Unit) {
    var state by remember { mutableStateOf<Offline<QuizResponse?>?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(quizId) {
        state = AppData.library.quiz(quizId)
        loading = false
    }

    if (loading) { LoadingScreen(stringResource(Res.string.common_loading)); return }

    val quiz = state?.value
    val cards = remember(quiz) { quiz?.let(::quizToCards).orEmpty() }

    /** Indices still in this round. */
    var deck by remember(cards) { mutableStateOf(cards.indices.toList()) }
    var position by remember(cards) { mutableStateOf(0) }
    var revealed by remember(cards) { mutableStateOf(false) }
    var toReview by remember(cards) { mutableStateOf<List<Int>>(emptyList()) }
    var knownThisRound by remember(cards) { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(top = 10.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, stringResource(Res.string.common_back), tint = TextPrimary)
            }
            Column {
                Text(
                    stringResource(Res.string.flashcards_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                quiz?.title?.let {
                    Text(
                        stringResource(Res.string.flashcards_from_quiz, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        state?.let { if (it.fromCache) { Spacer(Modifier.height(6.dp)); OfflineBanner(it) } }

        if (cards.isEmpty()) {
            CenteredContent {
                Spacer(Modifier.height(40.dp))
                Text(
                    stringResource(Res.string.flashcards_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            return@Column
        }

        // Round over: every card was seen once.
        if (position >= deck.size) {
            RoundSummary(
                toReview = toReview.size,
                onRepeat = {
                    deck = toReview
                    toReview = emptyList()
                    position = 0
                    revealed = false
                    knownThisRound = 0
                },
                onRestart = {
                    deck = cards.indices.toList()
                    toReview = emptyList()
                    position = 0
                    revealed = false
                    knownThisRound = 0
                }
            )
            return@Column
        }

        val card = cards[deck[position]]

        val progress by animateFloatAsState(
            targetValue = (position + 1).toFloat() / deck.size,
            animationSpec = tween(250),
            label = "flashcard-progress"
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            color = Primary,
            trackColor = Border
        )
        Text(
            stringResource(Res.string.flashcards_progress, position + 1, deck.size),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { revealed = !revealed },
                colors = CardDefaults.cardColors(
                    containerColor = if (revealed) SuccessLight else Surface
                ),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        card.front,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    if (revealed) {
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Success.copy(alpha = 0.3f))
                        Spacer(Modifier.height(20.dp))
                        Text(
                            card.back,
                            style = MaterialTheme.typography.titleSmall,
                            color = Success,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        card.note?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            stringResource(Res.string.flashcards_tap_to_reveal),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // The rating buttons appear only once the answer is visible. Rating a card you
        // have not looked at is not self-assessment, it is just advancing.
        if (revealed) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        toReview = toReview + deck[position]
                        position += 1
                        revealed = false
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text(stringResource(Res.string.flashcards_review))
                }
                Button(
                    onClick = {
                        knownThisRound += 1
                        position += 1
                        revealed = false
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success)
                ) {
                    Text(stringResource(Res.string.flashcards_knew_it))
                }
            }
        } else {
            Spacer(Modifier.height(84.dp))
        }
    }
}

@Composable
private fun RoundSummary(toReview: Int, onRepeat: () -> Unit, onRestart: () -> Unit) {
    CenteredContent(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(60.dp))
        Text(
            if (toReview == 0) stringResource(Res.string.flashcards_all_known)
            else stringResource(Res.string.flashcards_round_done, toReview),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(24.dp))
        Column(Modifier.padding(horizontal = 24.dp)) {
            if (toReview > 0) {
                PrimaryButton(
                    text = stringResource(Res.string.flashcards_repeat, toReview),
                    onClick = onRepeat
                )
                Spacer(Modifier.height(10.dp))
            }
            SecondaryButton(
                text = stringResource(Res.string.flashcards_restart),
                onClick = onRestart
            )
        }
    }
}
