package com.example.studyplatform.android.ui.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.AnimatedEntry
import com.example.studyplatform.android.components.LoadingScreen
import com.example.studyplatform.android.components.OfflineBanner
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.Offline
import com.example.studyplatform.model.Course
import kotlinx.serialization.json.*

/**
 * Reading an open course, with or without an account.
 *
 * Cached on first read, so a course opened once at school is readable that evening with
 * no signal — which for the students this is built for is the difference between the
 * library being usable and being theoretical.
 */
@Composable
fun CourseReaderScreen(
    slug: String,
    onBack: () -> Unit,
    onSignUp: () -> Unit
) {
    var state by remember { mutableStateOf(Offline<Course?>(null)) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(slug) {
        state = AppData.library.publicCourse(slug)
        loading = false
    }

    if (loading) { LoadingScreen("Opening the course…"); return }

    val course = state.value
    if (course == null) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "This course isn't available offline",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Open it once while you have a connection and you can read it anywhere.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onBack) {
                    Text("Back", color = Primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        return
    }

    val chapters = remember(course) {
        try {
            course.content?.jsonObject?.get("chapters")?.jsonArray ?: JsonArray(emptyList())
        } catch (_: Exception) {
            JsonArray(emptyList())
        }
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
                    course.domain ?: "Course",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                // Wider than a form but still capped: this is long-form reading, and text
                // running the full width of a tablet loses the eye on the way back.
                Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
            ) {
                item {
                    AnimatedEntry {
                        Column {
                            Text(
                                course.title,
                                style = MaterialTheme.typography.displayMedium,
                                color = TextPrimary
                            )
                            if (course.summary != null) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    course.summary!!,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            OfflineBanner(state)
                        }
                    }
                }

                chapters.forEachIndexed { chapterIndex, chapterElement ->
                    val chapter = chapterElement.jsonObject
                    val chapterTitle = chapter["title"]?.jsonPrimitive?.contentOrNull
                        ?: "Chapter ${chapterIndex + 1}"
                    val blocks = chapter["blocks"]?.jsonArray ?: JsonArray(emptyList())

                    item {
                        AnimatedEntry(index = chapterIndex) {
                            Column(Modifier.padding(top = 12.dp)) {
                                Text(
                                    chapterTitle,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    blocks.forEach { blockElement ->
                        item { ContentBlock(blockElement.jsonObject) }
                    }
                }

                if (chapters.isEmpty()) {
                    item {
                        Text(
                            "This course has no chapters yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                item { Attribution(course) }

                item {
                    Spacer(Modifier.height(8.dp))
                    SignUpPrompt(onSignUp)
                }
            }
        }
    }
}

/** One content block. Unknown types are skipped rather than rendered as raw JSON. */
@Composable
private fun ContentBlock(block: JsonObject) {
    val uriHandler = LocalUriHandler.current
    val type = block["type"]?.jsonPrimitive?.contentOrNull ?: return
    val text = block["text"]?.jsonPrimitive?.contentOrNull

    when (type) {
        "paragraph" -> if (text != null) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        }

        "heading" -> if (text != null) {
            Text(
                text,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        "list" -> {
            val items = block["items"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.contentOrNull
            } ?: emptyList()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { entry ->
                    Row {
                        Text("•", style = MaterialTheme.typography.bodyLarge, color = Primary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            entry,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        "code" -> if (text != null) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TextPrimary),
                shape = MaterialTheme.shapes.small
            ) {
                Column(Modifier.padding(14.dp)) {
                    block["language"]?.jsonPrimitive?.contentOrNull?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = Border
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium
                            .copy(fontFamily = FontFamily.Monospace),
                        color = Background
                    )
                }
            }
        }

        "link" -> {
            val url = block["url"]?.jsonPrimitive?.contentOrNull ?: return
            val caption = block["caption"]?.jsonPrimitive?.contentOrNull
            Card(
                onClick = { uriHandler.openUri(url) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = MaterialTheme.shapes.small,
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        text ?: url,
                        style = MaterialTheme.typography.titleSmall,
                        color = Primary
                    )
                    if (caption != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Who wrote this and under what terms.
 *
 * Not a footer nicety. Every seeded course is CC BY material, and CC BY makes naming
 * the source and its licence a condition of using it at all — a reader that shows the
 * text without this is in breach, not merely impolite. The source link is tappable
 * because sending readers to the original is the point of an open library.
 */
@Composable
private fun Attribution(course: Course) {
    val uriHandler = LocalUriHandler.current
    val source = course.sourceName ?: return

    Card(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Source",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(Modifier.height(6.dp))
            Text(source, style = MaterialTheme.typography.titleSmall, color = TextPrimary)

            course.licence?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }

            course.sourceUrl?.let { url ->
                Spacer(Modifier.height(10.dp))
                Card(
                    onClick = { uriHandler.openUri(url) },
                    colors = CardDefaults.cardColors(containerColor = PrimaryLight),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "Open the original",
                        style = MaterialTheme.typography.labelLarge,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "This summary was written for EduNova+ to introduce the subject and "
                        + "point you at the source. It is not a copy of the original text.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}
