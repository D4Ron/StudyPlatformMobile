package com.example.studyplatform.android.ui.tournaments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.studyplatform.model.SubmissionResponse
import com.example.studyplatform.model.TournamentQuestionResponse
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import tg.edunova.app.R

/**
 * Answering a tournament's questions.
 *
 * <p>The payload is author-authored JSON whose shape depends on `type`, and the server
 * strips the answer fields before sending it. It is read defensively here — a missing
 * key renders as absent rather than throwing, and a `type` this build does not know
 * says so instead of showing an empty card, because a tournament can contain a question
 * type added after this version shipped.
 *
 * <p>Nothing is queued offline. A submission is scored against a live contest, so an
 * answer uploaded an hour later is either wrong to score or wrong to drop — the screen
 * reports the failure and keeps the answer on screen so it can be sent again.
 */
@Composable
fun TournamentCompeteScreen(tournamentId: String, onBack: () -> Unit) {
    var questions by remember { mutableStateOf<List<TournamentQuestionResponse>>(emptyList()) }
    var index by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    /** Answers typed or chosen this session, by question id. */
    val answers = remember { mutableStateMapOf<String, String>() }

    /** What the server said about each submission this session. */
    val results = remember { mutableStateMapOf<String, SubmissionResponse>() }

    val scope = rememberCoroutineScope()

    suspend fun load() {
        runCatching { TournamentApi.questions(tournamentId) }
            .onSuccess { questions = it.sortedBy { q -> q.position }; error = null }
            .onFailure { error = it.message ?: "…" }
        loading = false
    }

    LaunchedEffect(tournamentId) { load() }

    if (loading) { LoadingScreen(stringResource(R.string.common_loading)); return }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(top = 12.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, stringResource(R.string.common_back), tint = TextPrimary)
            }
            if (questions.isNotEmpty()) {
                Text(
                    stringResource(R.string.tournaments_question_of, index + 1, questions.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextMuted
                )
            }
        }

        error?.let {
            ErrorMessage(it) { scope.launch { loading = true; load() } }
        }

        if (questions.isEmpty()) {
            CenteredContent {
                Spacer(Modifier.height(40.dp))
                Text(
                    stringResource(R.string.tournaments_no_questions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
            return@Column
        }

        val question = questions[index]
        val body = question.payload as? JsonObject ?: JsonObject(emptyMap())
        val locked = question.answered || results.containsKey(question.id)

        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    stringResource(R.string.tournaments_points, question.points),
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                // MCQ uses `question`, TRUE_FALSE uses `statement`. Falling back across
                // both means a payload that names it either way still renders.
                Text(
                    body.text("question") ?: body.text("statement") ?: body.text("prompt").orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                body.text("code")?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Background)
                            .padding(12.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                when (question.type) {
                    "MCQ" -> {
                        val options = body["options"] as? JsonArray ?: JsonArray(emptyList())
                        options.forEachIndexed { i, element ->
                            // Options are objects with a `text` field in the authoring
                            // format, but a plain string list is cheap to also accept.
                            val label = (element as? JsonObject)?.text("text")
                                ?: (element as? JsonPrimitive)?.contentOrNull
                                ?: return@forEachIndexed
                            ChoiceRow(
                                label = label,
                                letter = "ABCDEFGH".getOrNull(i)?.toString() ?: "${i + 1}",
                                selected = answers[question.id] == label,
                                enabled = !locked,
                                onClick = { answers[question.id] = label }
                            )
                        }
                    }

                    "TRUE_FALSE" -> {
                        listOf(
                            stringResource(R.string.tournaments_true) to "true",
                            stringResource(R.string.tournaments_false) to "false"
                        ).forEach { (label, value) ->
                            ChoiceRow(
                                label = label,
                                letter = if (value == "true") "V" else "F",
                                selected = answers[question.id] == value,
                                enabled = !locked,
                                onClick = { answers[question.id] = value }
                            )
                        }
                    }

                    "ESSAY", "CODE_IMAGE" -> {
                        OutlinedTextField(
                            value = answers[question.id].orEmpty(),
                            onValueChange = { answers[question.id] = it },
                            label = { Text(stringResource(R.string.tournaments_your_answer)) },
                            enabled = !locked,
                            minLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {
                        // CODE_CHALLENGE needs an editor and a runner. Saying so beats
                        // rendering an answer box that cannot produce a valid answer.
                        StatusBanner(
                            message = stringResource(R.string.tournaments_unsupported),
                            background = WarningLight,
                            contentColor = Warning
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                results[question.id]?.let { r ->
                    Text(
                        if (r.status.equals("PENDING", ignoreCase = true))
                            stringResource(R.string.tournaments_awaiting_review)
                        else stringResource(R.string.tournaments_scored, r.score),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Success
                    )
                } ?: if (question.answered) {
                    Text(
                        stringResource(R.string.tournaments_already_answered),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                } else Unit

                Spacer(Modifier.height(20.dp))

                val answerable = question.type in setOf("MCQ", "TRUE_FALSE", "ESSAY", "CODE_IMAGE")
                if (answerable && !locked) {
                    PrimaryButton(
                        text = stringResource(R.string.tournaments_submit),
                        loading = submitting,
                        enabled = !submitting && !answers[question.id].isNullOrBlank(),
                        onClick = {
                            submitting = true
                            error = null
                            scope.launch {
                                runCatching {
                                    TournamentApi.submit(
                                        tournamentId, question.id, answers.getValue(question.id)
                                    )
                                }
                                    .onSuccess { results[question.id] = it }
                                    // The answer stays on screen: a failed send is a
                                    // reason to try again, not to lose the work.
                                    .onFailure { error = it.message ?: "…" }
                                submitting = false
                            }
                        }
                    )
                }

                Spacer(Modifier.height(30.dp))
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { if (index > 0) index-- }, enabled = index > 0) {
                Text(stringResource(R.string.tournaments_previous))
            }
            TextButton(
                onClick = { if (index < questions.lastIndex) index++ },
                enabled = index < questions.lastIndex
            ) {
                Text(stringResource(R.string.tournaments_next))
            }
        }
    }
}

/** A tappable option row, used for both MCQ choices and true/false. */
@Composable
private fun ChoiceRow(
    label: String,
    letter: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PrimaryLight else Surface)
            .border(
                width = 1.dp,
                color = if (selected) Primary else Border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            letter,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Primary else TextMuted
        )
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) TextPrimary else TextMuted
        )
    }
}

/** A string field, or null when absent or not a string. Never throws on a shape. */
private fun JsonObject.text(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.takeIf { it.isNotBlank() }
