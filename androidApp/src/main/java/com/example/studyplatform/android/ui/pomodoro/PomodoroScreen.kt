package com.example.studyplatform.android.ui.pomodoro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.data.AppData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import tg.edunova.app.R

private const val ROUNDS_BEFORE_LONG_BREAK = 4

private enum class Phase(val minutes: Int) {
    FOCUS(25), SHORT_BREAK(5), LONG_BREAK(15)
}

/**
 * A focus timer that writes what it measures.
 *
 * A completed focus round is logged through `StudyRepository.saveSession`, which is the
 * offline path — the row lands locally and the outbox carries it up when there is signal.
 * That matters more here than anywhere else in the app: someone using a timer to study is
 * often doing it precisely because they are somewhere without a connection, and a timer
 * whose record of your work evaporates is worse than no timer.
 *
 * Only focus rounds are logged. A break is not study time, and counting it would inflate
 * the very statistic the student is trying to read honestly.
 */
@Composable
fun PomodoroScreen() {
    var phase by remember { mutableStateOf(Phase.FOCUS) }
    var remaining by remember { mutableStateOf(Phase.FOCUS.minutes * 60) }
    var running by remember { mutableStateOf(false) }
    var completedFocusRounds by remember { mutableStateOf(0) }
    var startedAt by remember { mutableStateOf<String?>(null) }
    var justLogged by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reset(to: Phase) {
        phase = to
        remaining = to.minutes * 60
        running = false
        startedAt = null
    }

    // One tick per second while running. Reading the clock at both ends rather than
    // trusting the tick count keeps the logged duration honest if the coroutine is
    // descheduled — which on a cheap phone under memory pressure it will be.
    LaunchedEffect(running, phase) {
        if (!running) return@LaunchedEffect
        while (running && remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        if (remaining <= 0) {
            running = false
            if (phase == Phase.FOCUS) {
                val began = startedAt
                completedFocusRounds += 1
                if (began != null) {
                    scope.launch {
                        runCatching {
                            AppData.study.saveSession(
                                startTime = began,
                                endTime = Clock.System.now().toString(),
                                focusScore = 100,
                                activity = "POMODORO"
                            )
                        }
                    }
                    justLogged = true
                }
                reset(
                    if (completedFocusRounds % ROUNDS_BEFORE_LONG_BREAK == 0) Phase.LONG_BREAK
                    else Phase.SHORT_BREAK
                )
            } else {
                reset(Phase.FOCUS)
            }
        }
    }

    val total = phase.minutes * 60
    val fraction by animateFloatAsState(
        targetValue = if (total == 0) 0f else remaining.toFloat() / total,
        animationSpec = tween(400),
        label = "pomodoro"
    )

    val accent = when (phase) {
        Phase.FOCUS -> Primary
        Phase.SHORT_BREAK -> Success
        Phase.LONG_BREAK -> Secondary
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            AnimatedEntry {
                Text(
                    stringResource(R.string.pomodoro_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(28.dp))

            AnimatedEntry(index = 1) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(240.dp)) {
                        val stroke = 16.dp.toPx()
                        val inset = stroke / 2
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        drawArc(
                            color = Border,
                            startAngle = -90f, sweepAngle = 360f, useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = accent,
                            startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            formatClock(remaining),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            stringResource(
                                when (phase) {
                                    Phase.FOCUS -> R.string.pomodoro_focus
                                    Phase.SHORT_BREAK -> R.string.pomodoro_short_break
                                    Phase.LONG_BREAK -> R.string.pomodoro_long_break
                                }
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            color = accent
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(
                    R.string.pomodoro_rounds,
                    (completedFocusRounds % ROUNDS_BEFORE_LONG_BREAK) + 1,
                    ROUNDS_BEFORE_LONG_BREAK
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            if (justLogged) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.pomodoro_done),
                    style = MaterialTheme.typography.bodySmall,
                    color = Success
                )
            }

            Spacer(Modifier.height(28.dp))

            PrimaryButton(
                text = stringResource(
                    when {
                        running -> R.string.pomodoro_pause
                        remaining < total -> R.string.pomodoro_resume
                        else -> R.string.pomodoro_start
                    }
                ),
                onClick = {
                    if (!running) {
                        justLogged = false
                        // Only stamp the start on a fresh round, so pausing does not
                        // shorten the session that eventually gets logged.
                        if (startedAt == null) startedAt = Clock.System.now().toString()
                    }
                    running = !running
                }
            )

            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text = stringResource(R.string.pomodoro_reset),
                onClick = { justLogged = false; reset(phase) }
            )
        }
    }
}

private fun formatClock(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
