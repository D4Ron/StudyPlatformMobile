package com.example.studyplatform.android.ui.quizzes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.PrimaryButton
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.QuizApi
import com.example.studyplatform.model.GenerateQuizRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCreateScreen(onQuizCreated: (String) -> Unit, onBack: () -> Unit) {
    var topic by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("MEDIUM") }
    var questionCount by remember { mutableStateOf(10) }
    var loading by remember { mutableStateOf(false) }
    var jobStatus by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(error!!, duration = SnackbarDuration.Long)
            error = null
        }
    }

    if (loading) {
        QuizLoadingScreen(jobStatus)
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data ->
            Snackbar(snackbarData = data, containerColor = WarningLight, contentColor = TextPrimary, shape = RoundedCornerShape(12.dp))
        }},
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Text(stringResource(tg.edunova.app.R.string.quizzes_generate_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
        },
        containerColor = Background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxSize()
                .padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            Text(stringResource(tg.edunova.app.R.string.quizzes_what_topic), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = topic, onValueChange = { topic = it },
                placeholder = { Text("e.g. Java OOP, Algorithms") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Secondary, unfocusedBorderColor = Border, focusedContainerColor = Surface, unfocusedContainerColor = Surface)
            )

            Spacer(Modifier.height(24.dp))
            Text(stringResource(tg.edunova.app.R.string.quizzes_difficulty), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("EASY", "Easy", Success),
                    Triple("MEDIUM", "Medium", Warning),
                    Triple("HARD", "Hard", Accent)
                ).forEach { (d, label, color) ->
                    FilterChip(
                        selected = difficulty == d, onClick = { difficulty = d },
                        label = { Text(label, fontWeight = if (difficulty == d) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.15f),
                            selectedLabelColor = color, labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(tg.edunova.app.R.string.quizzes_questions_label), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                // Animated so dragging the slider reads as one number changing rather
                // than a series of unrelated ones.
                AnimatedContent(
                    targetState = questionCount,
                    transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                    label = "count"
                ) { n ->
                    Text("$n", style = MaterialTheme.typography.headlineSmall, color = Secondary)
                }
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = questionCount.toFloat(), onValueChange = { questionCount = it.toInt() },
                valueRange = 5f..50f, steps = 8,
                colors = SliderDefaults.colors(thumbColor = Secondary, activeTrackColor = Secondary, activeTickColor = Secondary)
            )

            Spacer(Modifier.height(32.dp))

            PrimaryButton(
                text = stringResource(tg.edunova.app.R.string.quizzes_generate_title),
                enabled = topic.isNotBlank(),
                onClick = {
                    loading = true
                    scope.launch {
                        try {
                            val quiz = QuizApi.generate(
                                GenerateQuizRequest(topic, difficulty, "STANDARD", questionCount),
                                onStatus = { jobStatus = it }
                            )
                            onQuizCreated(quiz.id)
                        } catch (e: Exception) {
                            error = e.message ?: "Something went wrong. Please try again."
                            loading = false
                        }
                    }
                }
            )
            Spacer(Modifier.height(32.dp))
        }
        }
    }
}

@Composable
private fun QuizLoadingScreen(jobStatus: String? = null) {
    val infiniteTransition = rememberInfiniteTransition(label = "quiz")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutCubic), RepeatMode.Reverse), label = "p"
    )

    val tips = listOf("Crafting questions for your topic...", "Balancing difficulty levels...", "Writing detailed explanations...", "Almost ready...")
    var tipIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { kotlinx.coroutines.delay(3500); tipIndex = (tipIndex + 1) % tips.size }
    }

    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(48.dp)) {
            Icon(Icons.Default.Psychology, null, modifier = Modifier.size(64.dp).scale(pulse), tint = Secondary)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(tg.edunova.app.R.string.quizzes_generating), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                when (jobStatus) {
                    "PENDING" -> "Queued \u2014 starting shortly"
                    "RUNNING" -> "Working on it now"
                    else -> "This usually takes 10-20 seconds"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth().height(6.dp), color = Secondary, trackColor = SecondaryLight)
            Spacer(Modifier.height(20.dp))
            AnimatedContent(targetState = tipIndex, transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) }, label = "tip") { idx ->
                Text(tips[idx], style = MaterialTheme.typography.labelLarge, color = Primary, textAlign = TextAlign.Center)
            }
        }
    }
}
