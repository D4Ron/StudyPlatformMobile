package com.example.studyplatform.android.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.SearchApi
import com.example.studyplatform.model.SearchHit
import kotlinx.coroutines.delay
import tg.edunova.app.R

/** Below this the server refuses anyway, so there is no point asking. */
private const val MIN_QUERY = 2

/**
 * One box that looks across guides, notes, quizzes, courses and groups.
 *
 * Online-only and uncached, deliberately — a cached search would answer today's question
 * from yesterday's library while looking exactly like a fresh answer. When the request
 * fails the screen says the connection is the reason rather than showing an empty state,
 * because "found nothing" and "could not look" are different answers and only one of
 * them means stop typing.
 *
 * Results route by matching a closed set of destinations rather than following the
 * server's path verbatim, for the same reason a deep link does: it is data arriving over
 * the network, and an unrecognised route should do nothing rather than something
 * surprising.
 */
@Composable
fun SearchScreen(
    onOpenGuide: (String) -> Unit = {},
    onOpenQuiz: (String) -> Unit = {},
    onOpenGroup: (String) -> Unit = {},
    onOpenCourse: (String) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf<String?>(null) }
    var hits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    var counts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    // Debounced: a request per keystroke would make five calls for a five-letter word
    // and land them out of order. The delay is cancelled by the next keystroke because
    // LaunchedEffect restarts on a key change.
    LaunchedEffect(query, kind) {
        if (query.trim().length < MIN_QUERY) {
            hits = emptyList()
            counts = emptyMap()
            searched = false
            failed = false
            return@LaunchedEffect
        }
        delay(300)
        searching = true
        failed = false
        runCatching { SearchApi.search(query.trim(), listOfNotNull(kind)) }
            .onSuccess { hits = it.hits; counts = it.countsByKind }
            .onFailure { failed = true; hits = emptyList(); counts = emptyMap() }
        searching = false
        searched = true
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 640.dp).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.search_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextMuted) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, null, tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                KindFilters(
                    counts = counts,
                    selected = kind,
                    onSelect = { kind = it }
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        when {
            searching -> {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.5.dp)
                }
            }

            failed -> {
                CenteredContent {
                    Spacer(Modifier.height(20.dp))
                    StatusBanner(
                        message = stringResource(R.string.search_offline),
                        background = WarningLight,
                        contentColor = Warning
                    )
                }
            }

            query.trim().length < MIN_QUERY -> {
                CenteredContent {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            searched && hits.isEmpty() -> {
                CenteredContent {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.search_no_results, query.trim()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
                    ) {
                        items(hits, key = { it.kind + it.id }) { hit ->
                            ResultCard(hit) {
                                when (hit.kind) {
                                    "guide" -> onOpenGuide(hit.id)
                                    "quiz" -> onOpenQuiz(hit.id)
                                    "group" -> onOpenGroup(hit.id)
                                    // Courses open by slug, which is the tail of the
                                    // route rather than the id.
                                    "course" -> hit.route
                                        ?.substringAfterLast('/')
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let(onOpenCourse)
                                    // "note" and anything a later server adds: no
                                    // destination here yet, so the row does nothing
                                    // rather than navigating somewhere wrong.
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KindFilters(
    counts: Map<String, Int>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    // Fixed order so the chips do not reshuffle between searches. Only kinds this build
    // can label are offered; a kind a later server adds still appears in results, it
    // just has no chip.
    val kinds = listOf(
        "guide" to R.string.search_kind_guide,
        "note" to R.string.search_kind_note,
        "quiz" to R.string.search_kind_quiz,
        "course" to R.string.search_kind_course,
        "group" to R.string.search_kind_group
    )

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.search_all)) }
        )
        kinds.forEach { (value, label) ->
            val count = counts[value]
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(if (selected == value) null else value) },
                label = {
                    Text(
                        if (count != null && count > 0) {
                            stringResource(label) + " ($count)"
                        } else {
                            stringResource(label)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun ResultCard(hit: SearchHit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                hit.kind.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                hit.title.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            hit.snippet?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}
