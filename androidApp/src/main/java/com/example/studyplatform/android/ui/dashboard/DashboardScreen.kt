package com.example.studyplatform.android.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.ApiClient
import com.example.studyplatform.data.AppData
import com.example.studyplatform.model.DashboardStats
import com.example.studyplatform.model.LevelResponse
import com.example.studyplatform.model.RecommendationResponse
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    onNavigateToGuides: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToExplanations: () -> Unit
) {
    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    var level by remember { mutableStateOf<LevelResponse?>(null) }
    var recommendations by remember { mutableStateOf<List<RecommendationResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val firstName = ApiClient.getFirstName() ?: "Student"

    // Each of these falls back to the last copy this device downloaded, independently:
    // a dashboard with stale recommendations and fresh stats is more useful than one
    // that shows nothing because a single call failed.
    var fromCache by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val statsResult = AppData.library.dashboard()
        val levelResult = AppData.library.level()
        val recResult = AppData.library.recommendations()

        stats = statsResult.value
        level = levelResult.value
        recommendations = recResult.value
        fromCache = statsResult.fromCache || levelResult.fromCache || recResult.fromCache
        error = if (stats == null && !fromCache) "Could not load your dashboard" else null
        loading = false
    }

    if (loading) { LoadingScreen("Loading dashboard..."); return }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        // Welcome header
        item {
            AnimatedEntry {
                Column {
                    Text("Welcome back, $firstName", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            error != null -> error!!
                            fromCache -> "Offline — showing what you downloaded earlier"
                            else -> "Keep up the great work"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (error != null || fromCache) Warning else TextMuted
                    )
                }
            }
        }

        // Level progress
        item {
            level?.let {
                AnimatedEntry(index = 1) {
                    XpProgressBar(it.currentXp, it.xpForNextLevel, it.level, it.title)
                }
            }
        }

        // Stats cards
        item {
            AnimatedEntry(index = 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Total XP", "${stats?.totalXp ?: 0}", Warning, Modifier.weight(1f))
                    StatCard("Guides", "${stats?.guidesCompleted ?: 0}", Primary, Modifier.weight(1f))
                }
            }
        }
        item {
            AnimatedEntry(index = 3) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Quizzes", "${stats?.quizzesTaken ?: 0}", Secondary, Modifier.weight(1f))
                    StatCard("Avg Score", "${stats?.averageQuizScore?.toInt() ?: 0}%", Success, Modifier.weight(1f))
                }
            }
        }

        // Quick actions
        item { SectionHeading("Quick Actions") }
        item {
            AnimatedEntry(index = 4) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard("Generate Guide", "Create an AI study guide", Primary, Modifier.weight(1f)) { onNavigateToGuides() }
                    QuickActionCard("Take Quiz", "Test your knowledge", Secondary, Modifier.weight(1f)) { onNavigateToQuizzes() }
                }
            }
        }
        item {
            AnimatedEntry(index = 5) {
                QuickActionCard(
                    "Explain a Concept", "Ask for a plain-language explanation",
                    Success, Modifier.fillMaxWidth()
                ) { onNavigateToExplanations() }
            }
        }

        // XP by topic
        val topics = stats?.xpByTopic ?: emptyList()
        if (topics.isNotEmpty()) {
            item { SectionHeading("XP by Topic") }
            topics.forEach { topicXp ->
                item {
                    TopicProgressRow(topicXp.topic, topicXp.xp.toInt(), (topics.maxOfOrNull { it.xp } ?: 1).toInt())
                }
            }
        }

        // Recommendations
        if (recommendations.isNotEmpty()) {
            item { SectionHeading("Suggestions for You") }
            recommendations.forEach { rec ->
                item { RecommendationCard(rec) }
            }
        }

        // Recent activity
        val activities = stats?.recentActivity ?: emptyList()
        if (activities.isNotEmpty()) {
            item { SectionHeading("Recent Activity") }
            activities.take(5).forEach { act ->
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(act.description, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("+${act.xpEarned} XP", style = MaterialTheme.typography.labelMedium, color = Warning)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
    }
}

/** One heading treatment across the dashboard, so the sections read as siblings. */
@Composable
private fun SectionHeading(text: String) {
    AnimatedEntry {
        Text(text, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
    }
}

@Composable
private fun QuickActionCard(title: String, subtitle: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = color)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@Composable
private fun TopicProgressRow(topic: String, xp: Int, maxXp: Int) {
    val progress = if (maxXp > 0) xp.toFloat() / maxXp else 0f
    // Grows into place, so the relative sizes register as a comparison rather than
    // arriving as a set of already-drawn bars.
    val animated by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.EaseOutCubic),
        label = "topic-xp"
    )
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(topic, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
            Text("$xp XP", style = MaterialTheme.typography.labelMedium, color = Primary)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.extraSmall),
            color = Primary, trackColor = PrimaryLight
        )
    }
}

@Composable
private fun RecommendationCard(rec: RecommendationResponse) {
    val color = when (rec.reason) {
        "WEAK_AREA" -> Accent; "NATURAL_PROGRESSION" -> Primary
        "COMPLEMENTARY" -> Secondary; else -> Success
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text(rec.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
                SuggestionChip(onClick = {}, label = { Text(rec.reason.replace("_", " "), style = MaterialTheme.typography.labelSmall) })
            }
            if (rec.description != null) {
                Spacer(Modifier.height(4.dp))
                Text(rec.description!!, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
