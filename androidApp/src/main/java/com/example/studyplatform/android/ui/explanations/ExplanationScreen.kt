package com.example.studyplatform.android.ui.explanations

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.AnimatedEntry
import com.example.studyplatform.android.components.LoadingScreen
import com.example.studyplatform.android.components.PrimaryButton
import com.example.studyplatform.android.components.pressScale
import com.example.studyplatform.android.components.OfflineBanner
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.ExplanationApi
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.Offline
import com.example.studyplatform.model.ExplainConceptRequest
import com.example.studyplatform.model.ExplanationResponse
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
fun ExplanationScreen() {
    var concept by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("MEDIUM") }
    var loading by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<ExplanationResponse>>(emptyList()) }
    var currentExplanation by remember { mutableStateOf<ExplanationResponse?>(null) }
    var historyLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    var fromCache by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = AppData.library.explanations()
        history = result.value
        fromCache = result.fromCache
        historyLoading = false
    }

    if (loading) { LoadingScreen("Generating explanation\u2026"); return }

    // If viewing an explanation
    if (currentExplanation != null) {
        ExplanationDetail(currentExplanation!!) { currentExplanation = null }
        return
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
        Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp)
    ) {
        item {
            AnimatedEntry {
                Column {
                    Text(
                        "Explain a Concept",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ask about anything — the AI adapts to your level",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    if (fromCache) {
                        Spacer(Modifier.height(10.dp))
                        // Past explanations are readable offline; asking for a new one
                        // is not, because it needs the model.
                        OfflineBanner(Offline(Unit, fromCache = true))
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = concept, onValueChange = { concept = it },
                placeholder = { Text("e.g. Polymorphism in Java, Recursion, French Revolution") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Border, focusedContainerColor = Surface, unfocusedContainerColor = Surface)
            )
        }

        // Quick suggestions from history
        val suggestions = listOf("Binary Search", "Design Patterns", "Recursion", "SQL Joins", "Big O Notation", "Inheritance")
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.take(3).forEach { s ->
                    FilterChip(selected = false, onClick = { concept = s }, label = { Text(s, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(8.dp), colors = FilterChipDefaults.filterChipColors(containerColor = Surface, labelColor = TextSecondary))
                }
            }
        }

        item {
            Text("Detail level", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SHORT" to "Simple", "MEDIUM" to "Intermediate", "DETAILED" to "Advanced").forEach { (lvl, label) ->
                    FilterChip(
                        selected = selectedLevel == lvl, onClick = { selectedLevel = lvl },
                        label = { Text(label) }, modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryLight, selectedLabelColor = Primary)
                    )
                }
            }
        }

        item {
            PrimaryButton(text = "Get Explanation", enabled = concept.isNotBlank(), onClick = {
                loading = true
                scope.launch {
                    try {
                        val result = ExplanationApi.explain(ExplainConceptRequest(concept, selectedLevel))
                        currentExplanation = result
                        history = listOf(result) + history
                    } catch (_: Exception) {}
                    loading = false
                }
            })
        }

        // History
        if (history.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Previous Explanations", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            }
            itemsIndexed(history) { index, exp ->
                AnimatedEntry(index = index) {
                    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Card(
                        onClick = { currentExplanation = exp },
                        interactionSource = interaction,
                        modifier = Modifier.fillMaxWidth().pressScale(interaction),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(exp.concept, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(exp.detailLevel, style = MaterialTheme.typography.labelSmall, color = Secondary)
                            }
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
private fun ExplanationDetail(exp: ExplanationResponse, onBack: () -> Unit) {
    val content = try { exp.content?.let { Json { ignoreUnknownKeys = true }.parseToJsonElement(it.toString()).jsonObject } } catch (_: Exception) { null }
    val definition = content?.get("definition")?.jsonPrimitive?.contentOrNull ?: ""
    val analogy = content?.get("analogy")?.jsonPrimitive?.contentOrNull
    val explanation = content?.get("explanation")?.jsonPrimitive?.contentOrNull ?: ""
    val whenToUse = content?.get("whenToUse")?.jsonPrimitive?.contentOrNull

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
        Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AnimatedEntry {
                Column {
                    TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                        Text("\u2190 Back to search", color = Primary, style = MaterialTheme.typography.labelLarge)
                    }
                    Text(exp.concept, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                    Text(exp.detailLevel, style = MaterialTheme.typography.labelMedium, color = Secondary)
                }
            }
        }
        if (definition.isNotBlank()) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PrimaryLight), shape = RoundedCornerShape(12.dp)) {
                    Text(definition, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge, color = Primary)
                }
            }
        }
        if (analogy != null) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WarningLight), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Analogy", style = MaterialTheme.typography.labelSmall, color = Warning)
                        Spacer(Modifier.height(4.dp))
                        Text(analogy, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }
        if (explanation.isNotBlank()) {
            item { Text(explanation, style = MaterialTheme.typography.bodyLarge, color = TextSecondary) }
        }
        if (whenToUse != null) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SuccessLight), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("When to use it", style = MaterialTheme.typography.labelSmall, color = Success)
                        Spacer(Modifier.height(4.dp))
                        Text(whenToUse, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
    }
}
