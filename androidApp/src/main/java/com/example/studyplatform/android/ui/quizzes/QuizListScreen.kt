package com.example.studyplatform.android.ui.quizzes

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
import com.example.studyplatform.model.QuizListItem

@Composable
fun QuizListScreen(
    onCreateQuiz: () -> Unit,
    onTakeQuiz: (String) -> Unit,
    onFlashcards: (String) -> Unit = {}
) {
    var state by remember { mutableStateOf(Offline<List<QuizListItem>>(emptyList())) }
    var loading by remember { mutableStateOf(true) }
    val quizzes = state.value

    LaunchedEffect(Unit) {
        state = AppData.library.quizzes()
        loading = false
    }

    Scaffold(
        containerColor = Background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateQuiz,
                containerColor = Secondary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(tg.edunova.app.R.string.quizzes_new), style = MaterialTheme.typography.labelLarge) }
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
                                stringResource(tg.edunova.app.R.string.quizzes_title),
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${quizzes.size} quizzes",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(Modifier.height(10.dp))
                            OfflineBanner(state)
                        }
                    }
                }

                if (loading) {
                    item { ShimmerList(count = 4) }
                }

                if (!loading && quizzes.isEmpty()) {
                    item {
                        AnimatedEntry(index = 1) {
                            Card(
                                Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SecondaryLight),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(Modifier.padding(24.dp)) {
                                    Text(
                                        stringResource(tg.edunova.app.R.string.quizzes_none),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Secondary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    OfflineEmpty(
                                        state,
                                        stringResource(tg.edunova.app.R.string.quizzes_none_hint),
                                        Modifier,
                                        color = Secondary
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(quizzes, key = { _, q -> q.id }) { index, quiz ->
                    AnimatedEntry(index = index) {
                        val interaction = remember { MutableInteractionSource() }
                        Card(
                            onClick = { onTakeQuiz(quiz.id) },
                            interactionSource = interaction,
                            modifier = Modifier.fillMaxWidth().pressScale(interaction),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    quiz.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        quiz.difficulty,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (quiz.difficulty) {
                                            "EASY" -> Success
                                            "MEDIUM" -> Warning
                                            else -> Accent
                                        }
                                    )
                                    Text(
                                        "${quiz.questionCount} questions",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                                // A second way into the same content. Inside the card
                                // rather than on a separate screen, because the choice
                                // is "how do I want to study this quiz", and that is a
                                // decision made while looking at the quiz.
                                Spacer(Modifier.height(4.dp))
                                TextButton(
                                    onClick = { onFlashcards(quiz.id) },
                                    contentPadding = PaddingValues(horizontal = 0.dp)
                                ) {
                                    Text(
                                        stringResource(tg.edunova.app.R.string.flashcards_open),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Primary
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
