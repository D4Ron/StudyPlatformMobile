package com.example.studyplatform.android.ui.guest

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.Offline
import com.example.studyplatform.model.CourseSummary

/**
 * The catalogue, without an account.
 *
 * The point of guest mode is that someone can find out whether this platform is worth
 * registering for before handing over an email address — on a phone, in a country where
 * data costs money, and possibly at a school where signing up is not their decision to
 * make. So it reads the public catalogue, and it is cached like everything else: a
 * visitor evaluating the app is the least likely person to have a good connection.
 *
 * Nothing here writes. Everything that would need an account says so at the point of
 * use, rather than being hidden or failing silently.
 */
@Composable
fun GuestBrowseScreen(
    onOpenCourse: (String) -> Unit,
    onSignUp: () -> Unit,
    onBack: () -> Unit
) {
    var state by remember { mutableStateOf(Offline<List<CourseSummary>>(emptyList())) }
    var loading by remember { mutableStateOf(true) }
    var domain by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(domain) {
        loading = true
        state = AppData.library.publicCourses(domain)
        loading = false
    }

    val courses = state.value
    // Built from what actually came back rather than a fixed list, so the filters can
    // never offer a subject the catalogue does not have.
    val domains = remember(courses) {
        courses.mapNotNull { it.domain }.distinct().sorted()
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextMuted)
                }
                Text(
                    "Browse the library",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onSignUp) {
                    Text("Sign up", color = Primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
            ) {
                item {
                    AnimatedEntry {
                        Column {
                            Text(
                                "Open courses, free to read",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Openly licensed material from Siyavula, OpenStax and the "
                                        + "African Virtual University. No account needed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                            Spacer(Modifier.height(12.dp))
                            OfflineBanner(state)
                        }
                    }
                }

                if (domains.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                TopicChip("All", domain == null) { domain = null }
                            }
                            items(domains) { d ->
                                TopicChip(d, domain == d) { domain = if (domain == d) null else d }
                            }
                        }
                    }
                }

                if (loading) {
                    item { ShimmerList(count = 4) }
                }

                if (!loading && courses.isEmpty()) {
                    item {
                        AnimatedEntry(index = 1) {
                            Card(
                                Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = PrimaryLight),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(Modifier.padding(24.dp)) {
                                    Text(
                                        "Nothing to show yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    OfflineEmpty(
                                        state,
                                        "The library is still being put together. Check back soon.",
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(courses, key = { _, c -> c.slug }) { index, course ->
                    AnimatedEntry(index = index) {
                        CourseCard(course) { onOpenCourse(course.slug) }
                    }
                }

                if (!loading && courses.isNotEmpty()) {
                    item {
                        AnimatedEntry(index = courses.size) {
                            SignUpPrompt(onSignUp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: CourseSummary, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }

    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().pressScale(interaction),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(course.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            if (course.summary != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    course.summary!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 3
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                course.domain?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = Secondary)
                }
                Text(
                    "${course.chapterCount} chapters",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                // Named on the card, not only in the reader: this is someone else's work
                // and the credit travels with it.
                course.sourceName?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }
    }
}

/**
 * The one place guest mode asks for anything.
 *
 * Placed after the catalogue rather than in front of it: someone who has just read a
 * list of real courses has a reason to sign up, and someone who has not seen anything
 * yet does not.
 */
@Composable
fun SignUpPrompt(onSignUp: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrimaryLight),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Want the rest?",
                style = MaterialTheme.typography.titleMedium,
                color = Primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "An account adds AI study guides, quizzes, notes you can write offline, "
                        + "and study groups.",
                style = MaterialTheme.typography.bodyMedium,
                color = Primary
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(text = "Create a free account", onClick = onSignUp)
        }
    }
}
