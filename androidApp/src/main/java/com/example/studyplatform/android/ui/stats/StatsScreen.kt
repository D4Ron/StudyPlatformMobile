package com.example.studyplatform.android.ui.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.data.Offline
import com.example.studyplatform.data.AppData
import com.example.studyplatform.model.BadgeResponse
import com.example.studyplatform.model.DashboardStats
import com.example.studyplatform.model.LevelResponse

@Composable
fun StatsScreen() {
    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    var level by remember { mutableStateOf<LevelResponse?>(null) }
    var badges by remember { mutableStateOf<List<BadgeResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var fromCache by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val statsResult = AppData.library.dashboard()
        val levelResult = AppData.library.level()
        val badgeResult = AppData.library.badges()

        stats = statsResult.value
        level = levelResult.value
        badges = badgeResult.value
        fromCache = statsResult.fromCache || levelResult.fromCache || badgeResult.fromCache
        loading = false
    }

    if (loading) { LoadingScreen("Loading your stats\u2026"); return }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp)
    ) {
        item {
            AnimatedEntry {
                Column {
                    Text(stringResource(tg.edunova.app.R.string.stats_title), style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                    if (fromCache) {
                        Spacer(Modifier.height(10.dp))
                        OfflineBanner(Offline(Unit, fromCache = true))
                    }
                }
            }
        }

        // Level card
        item {
            level?.let {
                AnimatedEntry(index = 1) {
                    XpProgressBar(it.currentXp, it.xpForNextLevel, it.level, it.title)
                }
            }
        }

        // Stats grid
        item {
            AnimatedEntry(index = 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(stringResource(tg.edunova.app.R.string.stats_total_xp), "${stats?.totalXp ?: 0}", Warning, Modifier.weight(1f))
                    StatCard(stringResource(tg.edunova.app.R.string.stats_study_hours), "${(stats?.totalStudyMinutes ?: 0) / 60}", Primary, Modifier.weight(1f))
                }
            }
        }
        item {
            AnimatedEntry(index = 3) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(stringResource(tg.edunova.app.R.string.stats_guides), "${stats?.guidesCompleted ?: 0}", Success, Modifier.weight(1f))
                    StatCard(stringResource(tg.edunova.app.R.string.stats_quizzes), "${stats?.quizzesTaken ?: 0}", Secondary, Modifier.weight(1f))
                }
            }
        }
        item {
            AnimatedEntry(index = 4) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(stringResource(tg.edunova.app.R.string.stats_avg_score), "${stats?.averageQuizScore?.toInt() ?: 0}%", Accent, Modifier.weight(1f))
                    StatCard(stringResource(tg.edunova.app.R.string.stats_badges), "${badges.count { it.earned }}", Warning, Modifier.weight(1f))
                }
            }
        }

        // XP by topic
        val topics = stats?.xpByTopic ?: emptyList()
        if (topics.isNotEmpty()) {
            item { AnimatedEntry { Text(stringResource(tg.edunova.app.R.string.dash_xp_by_topic), style = MaterialTheme.typography.headlineSmall, color = TextPrimary) } }
            val maxXp = (topics.maxOfOrNull { it.xp } ?: 1).toInt()
            val topicColors = listOf(Primary, Secondary, Success, Warning, Accent)
            topics.forEachIndexed { idx, t ->
                item {
                    val color = topicColors[idx % topicColors.size]
                    val progress by animateFloatAsState(
                        targetValue = if (maxXp > 0) t.xp.toFloat() / maxXp else 0f,
                        animationSpec = tween(1000, delayMillis = idx * 150), label = "xp$idx"
                    )
                    Column {
                        Row {
                            Text(t.topic, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                            Text("${t.xp} XP", style = MaterialTheme.typography.labelLarge, color = color)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = color, trackColor = color.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }

        // Badges
        if (badges.isNotEmpty()) {
            item { AnimatedEntry { Text(stringResource(tg.edunova.app.R.string.stats_achievements), style = MaterialTheme.typography.headlineSmall, color = TextPrimary) } }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(badges) { badge ->
                        BadgeCard(badge)
                    }
                }
            }
        }

        // Recent activity
        val activities = stats?.recentActivity ?: emptyList()
        if (activities.isNotEmpty()) {
            item { AnimatedEntry { Text(stringResource(tg.edunova.app.R.string.dash_recent_activity), style = MaterialTheme.typography.headlineSmall, color = TextPrimary) } }
            activities.take(10).forEach { act ->
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            val iconColor = when {
                                act.type.contains("GUIDE") -> Primary
                                act.type.contains("QUIZ") -> Secondary
                                act.type.contains("EXPLANATION") -> Success
                                else -> Warning
                            }
                            Box(Modifier.size(10.dp).clip(CircleShape).background(iconColor))
                            Spacer(Modifier.width(12.dp))
                            Text(act.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
                            Text("+${act.xpEarned} XP", style = MaterialTheme.typography.labelMedium, color = Warning)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
    }
}

@Composable
private fun BadgeCard(badge: BadgeResponse) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = if (badge.earned) Surface else Surface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(if (badge.earned) 2.dp else 0.dp)
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(if (badge.earned) Brush.linearGradient(listOf(Warning, Accent)) else Brush.linearGradient(listOf(Border, Border))),
                contentAlignment = Alignment.Center
            ) {
                Text(if (badge.earned) "★" else "?", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                badge.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (badge.earned) TextPrimary else TextMuted,
                maxLines = 1
            )
            if (!badge.earned && badge.progress != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (badge.progress!! / 100f).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Primary, trackColor = PrimaryLight
                )
            }
        }
    }
}
