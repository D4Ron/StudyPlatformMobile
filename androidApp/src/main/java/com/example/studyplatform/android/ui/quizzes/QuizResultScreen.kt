package com.example.studyplatform.android.ui.quizzes

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.PrimaryButton
import com.example.studyplatform.android.components.SecondaryButton
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.model.QuizAttemptResponse

@Composable
fun QuizResultScreen(attempt: QuizAttemptResponse, onRetake: () -> Unit, onDashboard: () -> Unit) {
    val pct = attempt.percentageScore
    val scoreColor = when { pct >= 80 -> Success; pct >= 60 -> Warning; else -> Accent }
    val emoji = when { pct >= 90 -> "Excellent!"; pct >= 75 -> "Great job!"; pct >= 60 -> "Good effort!"; else -> "Keep practicing!" }

    val animatedPct by animateFloatAsState(targetValue = pct.toFloat(), animationSpec = tween(1500, easing = EaseOutCubic), label = "score")
    val pulse by rememberInfiniteTransition(label = "trophy").animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutCubic), RepeatMode.Reverse), label = "p"
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // Trophy
        if (pct >= 75) {
            Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(72.dp).scale(pulse), tint = Warning)
        }

        Spacer(Modifier.height(20.dp))

        // Score circle
        Box(
            modifier = Modifier.size(160.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(scoreColor.copy(alpha = 0.1f), scoreColor.copy(alpha = 0.2f)))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${animatedPct.toInt()}%", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                Text("${attempt.correctCount}/${attempt.totalQuestions}", fontSize = 16.sp, color = TextMuted)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(emoji, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(attempt.quizTitle ?: "Quiz Complete", fontSize = 15.sp, color = TextMuted, textAlign = TextAlign.Center)

        Spacer(Modifier.height(32.dp))

        // Stats row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ResultStat("Correct", "${attempt.correctCount}", Success)
            ResultStat("Wrong", "${attempt.totalQuestions - attempt.correctCount}", Accent)
            ResultStat("Time", "${attempt.timeTakenSeconds / 60}m ${attempt.timeTakenSeconds % 60}s", Primary)
            ResultStat("Points", "${attempt.score}", Warning)
        }

        Spacer(Modifier.height(32.dp))

        // Certificate banner
        if (pct >= 75) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SuccessLight),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Certificate Earned!", fontWeight = FontWeight.Bold, color = Success, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Score 75% or above — well done!", fontSize = 14.sp, color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // XP earned
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WarningLight),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("+30 XP", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Warning)
                Spacer(Modifier.width(12.dp))
                Text("earned for completing this quiz", fontSize = 14.sp, color = TextSecondary)
            }
        }

        Spacer(Modifier.height(32.dp))
        PrimaryButton(text = "Back to Dashboard", onClick = onDashboard)
        Spacer(Modifier.height(12.dp))
        SecondaryButton(text = "Retake Quiz", onClick = onRetake)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ResultStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = TextMuted)
    }
}
