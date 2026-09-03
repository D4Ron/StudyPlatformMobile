package com.example.studyplatform.android.ui.guides

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.AnimatedEntry
import com.example.studyplatform.android.components.LoadingScreen
import com.example.studyplatform.android.components.OfflineBanner
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.Offline
import com.example.studyplatform.model.GuideResponse
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reading a guide.
 *
 * The most important screen in the app to have offline. A study guide is something you
 * sit with for half an hour, and that half hour is exactly when a student is least
 * likely to be somewhere with a connection — on a bus, in a room without signal, on a
 * phone with no data left for the month. Opening one you have read before must work
 * whether or not the network does.
 */
@Composable
fun GuideViewScreen(guideId: String, onBack: () -> Unit) {
    var state by remember { mutableStateOf(Offline<GuideResponse?>(null)) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(guideId) {
        state = AppData.library.guide(guideId)
        loading = false
    }

    if (loading) { LoadingScreen("Loading guide…"); return }

    val g = state.value
    if (g == null) {
        // Never downloaded and not reachable now — a different situation from an empty
        // guide, and worth saying so plainly.
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "This guide isn't available offline",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Open it once while you have a connection and it will be here next time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onBack) {
                    Text("Back to guides", color = Primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        return
    }

    val modules = try {
        g.content?.jsonObject?.get("modules")?.jsonArray ?: JsonArray(emptyList())
    } catch (_: Exception) { JsonArray(emptyList()) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedEntry {
                    Column {
                        Text(
                            g.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                g.expertiseLevel,
                                style = MaterialTheme.typography.labelMedium,
                                color = Secondary
                            )
                            Text(
                                "~${g.totalEstimatedMinutes} min",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                            Text(
                                "${modules.size} modules",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OfflineBanner(state)
                    }
                }
            }

            modules.forEachIndexed { index, moduleElement ->
                item {
                    val module = moduleElement.jsonObject
                    val title = module["title"]?.jsonPrimitive?.contentOrNull
                        ?: "Module ${index + 1}"
                    val type = module["type"]?.jsonPrimitive?.contentOrNull ?: ""
                    val content = module["content"]?.jsonPrimitive?.contentOrNull ?: ""
                    val keyBox = module["keyConceptBox"]?.jsonPrimitive?.contentOrNull

                    AnimatedEntry(index = index) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Row {
                                    Text(
                                        "Module ${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary
                                )

                                if (keyBox != null) {
                                    Spacer(Modifier.height(10.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = WarningLight),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(
                                                "Key idea",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Warning
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                keyBox,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Warning
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                // bodyLarge, not bodyMedium: this is the long-form reading
                                // the whole screen exists for.
                                Text(
                                    content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            item {
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to guides", color = Primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
