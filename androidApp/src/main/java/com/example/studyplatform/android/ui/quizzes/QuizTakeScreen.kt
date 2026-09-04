package com.example.studyplatform.android.ui.quizzes

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.AnimatedEntry
import com.example.studyplatform.android.components.LoadingScreen
import com.example.studyplatform.android.components.OfflineBanner
import com.example.studyplatform.android.components.PrimaryButton
import com.example.studyplatform.android.sync.SyncWorker
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.ApiClient
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.Offline
import com.example.studyplatform.model.QuizAttemptResponse
import com.example.studyplatform.model.QuizResponse
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

/**
 * Taking a quiz, with or without a connection.
 *
 * The quiz payload carries the correct answer with every question — that is what lets
 * this screen mark answers as you go, and it is also what makes an offline attempt
 * possible: the device can score the whole thing itself. When submission fails the
 * attempt is stored and queued instead of being lost, and the student sees their result
 * immediately.
 *
 * The server re-scores every synced attempt against its own copy of the quiz, so the
 * local score is only ever what the student sees first, never what counts. XP comes from
 * the server's scoring either way.
 */
@Composable
fun QuizTakeScreen(
    quizId: String,
    onFinished: (QuizAttemptResponse) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(Offline<QuizResponse?>(null)) }
    var loading by remember { mutableStateOf(true) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    // Keyed by question index rather than appended to a list: going back to a question
    // used to add a second answer for it, and the old list was mutated in place inside a
    // `mutableStateOf`, which Compose cannot see.
    var answers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    var submitting by remember { mutableStateOf(false) }
    var startTime by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(quizId) {
        state = AppData.library.quiz(quizId)
        startTime = System.currentTimeMillis()
        loading = false
    }

    if (loading) { LoadingScreen(stringResource(tg.edunova.app.R.string.common_loading)); return }

    val q = state.value
    if (q == null) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(tg.edunova.app.R.string.quizzes_unavailable_offline),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(tg.edunova.app.R.string.quizzes_unavailable_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onBack) {
                    Text(stringResource(tg.edunova.app.R.string.common_back), color = Primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        return
    }

    val questions = remember(q) { parseQuestions(q) }

    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No questions found in this quiz",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )
        }
        return
    }

    val question = questions[currentIndex].jsonObject
    val questionText = question["questionText"]?.jsonPrimitive?.contentOrNull
        ?: "Question ${currentIndex + 1}"
    val options = question["options"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
    val correctAnswer = question["correctAnswer"]?.jsonPrimitive?.contentOrNull ?: ""
    val explanation = question["explanation"]?.jsonPrimitive?.contentOrNull

    val progress by animateFloatAsState(
        targetValue = (currentIndex + 1).toFloat() / questions.size,
        animationSpec = tween(320, easing = EaseOutCubic),
        label = "quiz-progress"
    )

    fun finish() {
        submitting = true
        scope.launch {
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            val submission = buildJsonArray {
                answers.toSortedMap().forEach { (index, answer) ->
                    add(buildJsonObject {
                        put("questionIndex", index)
                        put("selectedAnswer", answer)
                    })
                }
            }

            try {
                val result = com.example.studyplatform.api.QuizApi.submit(
                    com.example.studyplatform.model.SubmitQuizRequest(
                        quizId = quizId,
                        answers = answers.toSortedMap().map { (index, answer) ->
                            com.example.studyplatform.model.AnswerSubmission(index, answer)
                        },
                        timeTakenSeconds = elapsed
                    )
                )
                onFinished(result)
            } catch (e: Exception) {
                // Not a failure the student caused, and not one they should pay for by
                // losing the attempt. Score it here, queue it, and show the result.
                println("Submitting offline instead: ${e.message}")
                val correct = countCorrect(questions, answers)
                val local = AppData.attempts.record(
                    quizId = quizId,
                    quizTitle = q.title,
                    answers = submission,
                    correctCount = correct,
                    totalQuestions = questions.size,
                    timeTakenSeconds = elapsed
                )
                SyncWorker.syncNow(context)
                onFinished(
                    QuizAttemptResponse(
                        id = local.id,
                        quizId = quizId,
                        quizTitle = q.title,
                        // Points are the server's to award; showing the raw count is
                        // honest about what the device actually knows.
                        score = correct,
                        totalPoints = questions.size,
                        correctCount = correct,
                        totalQuestions = questions.size,
                        percentageScore = local.percentageScore,
                        timeTakenSeconds = elapsed
                    )
                )
            }
            submitting = false
        }
    }

    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxSize().padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, "Close", tint = TextMuted)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Question ${currentIndex + 1} of ${questions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                            .clip(MaterialTheme.shapes.extraSmall),
                        color = Primary, trackColor = PrimaryLight
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OfflineBanner(state)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(16.dp))
                Text(
                    questionText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Spacer(Modifier.height(20.dp))

                options.forEachIndexed { idx, option ->
                    val letter = ('A' + idx).toString()
                    val isSelected = selectedAnswer == letter
                    val isCorrect = letter == correctAnswer
                    val borderColor = when {
                        !submitted && isSelected -> Primary
                        submitted && isCorrect -> Success
                        submitted && isSelected && !isCorrect -> Accent
                        else -> Border
                    }
                    val bgColor = when {
                        !submitted && isSelected -> PrimaryLight
                        submitted && isCorrect -> SuccessLight
                        submitted && isSelected && !isCorrect -> AccentLight
                        else -> Surface
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                            .then(
                                if (!submitted) Modifier.clickable { selectedAnswer = letter }
                                else Modifier
                            ),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = MaterialTheme.shapes.medium,
                        border = CardDefaults.outlinedCardBorder()
                            .copy(width = if (isSelected || (submitted && isCorrect)) 2.dp else 1.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(32.dp).clip(CircleShape)
                                    .background(if (isSelected) borderColor else Color.Transparent)
                                    .border(1.5.dp, borderColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    letter,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) Color.White else borderColor
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(
                                option.removePrefix("$letter) ").removePrefix("$letter. "),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (submitted && isCorrect) {
                                Icon(Icons.Default.CheckCircle, null, tint = Success)
                            }
                        }
                    }
                }

                if (submitted && explanation != null) {
                    Spacer(Modifier.height(16.dp))
                    AnimatedEntry {
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PrimaryLight),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                explanation,
                                Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Primary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (!submitted) {
                PrimaryButton(
                    text = stringResource(tg.edunova.app.R.string.quizzes_submit_answer),
                    enabled = selectedAnswer != null,
                    onClick = {
                        submitted = true
                        answers = answers + (currentIndex to (selectedAnswer ?: ""))
                    }
                )
            } else {
                val isLast = currentIndex >= questions.size - 1
                PrimaryButton(
                    text = if (isLast) stringResource(tg.edunova.app.R.string.quizzes_see_results) else stringResource(tg.edunova.app.R.string.quizzes_next_question),
                    loading = submitting,
                    onClick = {
                        if (isLast) finish()
                        else {
                            currentIndex++
                            selectedAnswer = null
                            submitted = false
                        }
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Both shapes the generator emits: a bare array, or an object wrapping one. */
private fun parseQuestions(quiz: QuizResponse): JsonArray = try {
    val root = ApiClient.json.parseToJsonElement(quiz.questions.toString())
    when {
        root is JsonObject && root.containsKey("questions") -> root["questions"]!!.jsonArray
        root is JsonArray -> root
        else -> JsonArray(emptyList())
    }
} catch (_: Exception) {
    JsonArray(emptyList())
}

/**
 * Scores the attempt on the device.
 *
 * Case-insensitive, matching the server's comparison — a mismatch here would show the
 * student one score and record another.
 */
private fun countCorrect(questions: JsonArray, answers: Map<Int, String>): Int =
    answers.count { (index, given) ->
        if (index !in questions.indices) return@count false
        val correct = questions[index].jsonObject["correctAnswer"]
            ?.jsonPrimitive?.contentOrNull ?: return@count false
        correct.equals(given, ignoreCase = true)
    }
