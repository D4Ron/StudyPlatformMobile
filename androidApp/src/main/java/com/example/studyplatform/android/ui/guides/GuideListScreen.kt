package com.example.studyplatform.android.ui.guides

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.Offline
import com.example.studyplatform.model.GuideListItem

@Composable
fun GuideListScreen(onCreateGuide: () -> Unit, onViewGuide: (String) -> Unit) {
    // Network first, last downloaded copy second. A guide list is small and changes
    // on the server, so showing the cache first would mean showing yesterday's library
    // to someone who has signal.
    var state by remember { mutableStateOf(Offline<List<GuideListItem>>(emptyList())) }
    var loading by remember { mutableStateOf(true) }
    val guides = state.value

    LaunchedEffect(Unit) {
        state = AppData.library.guides()
        loading = false
    }

    Scaffold(
        containerColor = Background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateGuide,
                containerColor = Primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(tg.edunova.app.R.string.guides_new), style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp)
            ) {
                item {
                    AnimatedEntry {
                        Column {
                            Text(
                                stringResource(tg.edunova.app.R.string.guides_title),
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${guides.size} guides generated",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(Modifier.height(10.dp))
                            OfflineBanner(state)
                        }
                    }
                }

                // Placeholders in the shape of the result, rather than a spinner: this
                // says "guides are coming" instead of "something is happening", which on
                // a slow connection is the difference between progress and a stall.
                if (loading) {
                    item { ShimmerList(count = 4) }
                }

                if (!loading && guides.isEmpty()) {
                    item {
                        AnimatedEntry(index = 1) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = PrimaryLight),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(Modifier.padding(24.dp)) {
                                    Text(
                                        stringResource(tg.edunova.app.R.string.guides_none),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    OfflineEmpty(
                                        state,
                                        stringResource(tg.edunova.app.R.string.guides_none_hint),
                                        Modifier,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(guides, key = { _, g -> g.id }) { index, guide ->
                    AnimatedEntry(index = index) {
                        val interaction = remember { MutableInteractionSource() }
                        Card(
                            onClick = { onViewGuide(guide.id) },
                            interactionSource = interaction,
                            modifier = Modifier.fillMaxWidth().pressScale(interaction),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    guide.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        guide.expertiseLevel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Secondary
                                    )
                                    Text(
                                        "${guide.moduleCount} modules",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        "~${guide.totalEstimatedMinutes} min",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
