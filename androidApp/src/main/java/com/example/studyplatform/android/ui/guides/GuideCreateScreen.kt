package com.example.studyplatform.android.ui.guides

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.PrimaryButton
import com.example.studyplatform.android.components.TopicChip
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.GuideApi
import com.example.studyplatform.model.GenerateGuideRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GuideCreateScreen(onGuideCreated: (String) -> Unit, onBack: () -> Unit) {
    var topic by remember { mutableStateOf("") }
    var specificConcept by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("INTERMEDIATE") }
    var loading by remember { mutableStateOf(false) }
    // What the server says the job is doing, so a long wait is explained rather than
    // merely slow.
    var jobStatus by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val subjects = listOf("Computer Science", "Mathematics", "Physics", "Biology", "Chemistry", "Engineering", "Literature", "Economics", "Law", "Business")

    // Show snackbar for errors
    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(error!!, duration = SnackbarDuration.Long)
            error = null
        }
    }

    if (loading) {
        GeneratingLoadingScreen(jobStatus)
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = WarningLight,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(12.dp)
            )
        }},
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Text(stringResource(tg.edunova.app.R.string.guides_generate_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
        },
        containerColor = Background
    ) { padding ->
        // Capped and centred: a form stretched across a tablet leaves the eye hunting
        // for the next field.
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxSize()
                .padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // Topic input
            Text(stringResource(tg.edunova.app.R.string.guides_what_learn), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = topic, onValueChange = { topic = it },
                placeholder = { Text("e.g. Java OOP, Calculus, French Revolution") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Border, focusedContainerColor = Surface, unfocusedContainerColor = Surface)
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                subjects.forEach { subj -> TopicChip(subj, topic == subj) { topic = subj } }
            }

            Spacer(Modifier.height(24.dp))

            // Specific concept
            Text(stringResource(tg.edunova.app.R.string.guides_focus_area), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = specificConcept, onValueChange = { specificConcept = it },
                placeholder = { Text("e.g. Inheritance and Polymorphism") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Border, focusedContainerColor = Surface, unfocusedContainerColor = Surface)
            )

            Spacer(Modifier.height(24.dp))

            // Expertise level
            Text(stringResource(tg.edunova.app.R.string.guides_your_level), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            listOf(
                Triple("BEGINNER", "Beginner", "Starting from scratch — I'll explain everything"),
                Triple("INTERMEDIATE", "Intermediate", "I know the basics — go deeper"),
                Triple("PROFESSIONAL", "Advanced", "I'm experienced — show me edge cases")
            ).forEach { (level, title, desc) ->
                Card(
                    onClick = { selectedLevel = level },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = if (selectedLevel == level) PrimaryLight else Surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(if (selectedLevel == level) 2.dp else 0.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedLevel == level, onClick = { selectedLevel = level }, colors = RadioButtonDefaults.colors(selectedColor = Primary))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleMedium, color = if (selectedLevel == level) Primary else TextPrimary)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            PrimaryButton(
                text = stringResource(tg.edunova.app.R.string.guides_generate_action),
                enabled = topic.isNotBlank(),
                onClick = {
                    loading = true
                    scope.launch {
                        try {
                            val guide = GuideApi.generate(
                                GenerateGuideRequest(
                                    topic = topic, expertiseLevel = selectedLevel,
                                    specificConcept = specificConcept.ifBlank { null }
                                ),
                                onStatus = { jobStatus = it }
                            )
                            onGuideCreated(guide.id)
                        } catch (e: Exception) {
                            error = e.message ?: "Something went wrong. Please try again."
                            loading = false
                        }
                    }
                }
            )
            Spacer(Modifier.height(32.dp))
        }
        }
    }
}

@Composable
private fun GeneratingLoadingScreen(jobStatus: String? = null) {
    val infiniteTransition = rememberInfiniteTransition(label = "gen")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutCubic), RepeatMode.Reverse), label = "p"
    )

    val tips = listOf(
        "AI is analyzing your topic...",
        "Structuring learning modules...",
        "Adding examples and exercises...",
        "Almost there..."
    )
    var tipIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(48.dp)) {
            Icon(
                Icons.Default.AutoAwesome, contentDescription = null,
                modifier = Modifier.size(64.dp).scale(pulse), tint = Primary
            )
            Spacer(Modifier.height(24.dp))
            Text(stringResource(tg.edunova.app.R.string.guides_generating), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                when (jobStatus) {
                    "PENDING" -> "Queued \u2014 starting shortly"
                    "RUNNING" -> "Working on it now"
                    else -> "This usually takes 15-30 seconds"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Primary, trackColor = PrimaryLight
            )
            Spacer(Modifier.height(20.dp))
            AnimatedContent(targetState = tipIndex, transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) }, label = "tip") { idx ->
                Text(tips[idx], style = MaterialTheme.typography.labelLarge, color = Secondary, textAlign = TextAlign.Center)
            }
        }
    }
}
