package com.example.studyplatform.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.UserApi
import com.example.studyplatform.model.UpdateProfileRequest
import kotlinx.coroutines.launch
import tg.edunova.app.R

/**
 * The two questions registration does not ask.
 *
 * Three of the four ways into this app skip the long form — Google, demo mode, and a
 * mobile sign-up that only collects a name and a password — so without this an account
 * carries defaults forever and recommendations have nothing to work from.
 *
 * Skippable, and it means it. Someone who wants to look around first should not be held
 * at a form, and the same questions are reachable later from Settings. A failure to save
 * does not trap them either: the screen says the answers can be set later and moves on,
 * because blocking entry to the app over a preferences write would be the wrong ranking
 * of what matters.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var level by remember { mutableStateOf("UNIVERSITY") }
    val domains = remember { mutableStateListOf<String>() }
    var objectives by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val levels = listOf(
        "MIDDLE_SCHOOL" to stringResource(R.string.onboarding_level_middle),
        "HIGH_SCHOOL" to stringResource(R.string.onboarding_level_high),
        "UNIVERSITY" to stringResource(R.string.onboarding_level_university)
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            AnimatedEntry {
                Text(
                    stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(8.dp))
            AnimatedEntry(index = 1) {
                Text(
                    stringResource(R.string.onboarding_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }

            Spacer(Modifier.height(28.dp))

            SectionLabel(stringResource(R.string.onboarding_level))
            Spacer(Modifier.height(10.dp))
            levels.forEach { (value, label) ->
                SelectRow(
                    label = label,
                    selected = level == value,
                    onClick = { level = value }
                )
            }

            Spacer(Modifier.height(24.dp))

            SectionLabel(stringResource(R.string.onboarding_domains))
            Text(
                stringResource(R.string.onboarding_domains_hint),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            FlowChips(
                options = COURSE_DOMAINS,
                selected = domains,
                onToggle = { if (it in domains) domains.remove(it) else domains.add(it) }
            )

            Spacer(Modifier.height(24.dp))

            SectionLabel(stringResource(R.string.onboarding_objectives))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = objectives,
                onValueChange = { objectives = it },
                placeholder = { Text(stringResource(R.string.onboarding_objectives_hint)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (failed) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.onboarding_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = Warning
                )
            }

            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = stringResource(R.string.onboarding_finish),
                loading = saving,
                enabled = !saving,
                onClick = {
                    saving = true
                    scope.launch {
                        runCatching {
                            UserApi.updateProfile(
                                UpdateProfileRequest(
                                    educationLevel = level,
                                    // Only sent when something was chosen. An empty list
                                    // means "none of these", which is a different answer
                                    // from not having reached the question.
                                    preferenceDomains = domains.toList().takeIf { it.isNotEmpty() },
                                    objectives = objectives.trim().takeIf { it.isNotBlank() }
                                )
                            )
                        }
                            .onSuccess { onDone() }
                            .onFailure {
                                // Say so, then let them in anyway.
                                failed = true
                                onDone()
                            }
                        saving = false
                    }
                }
            )

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.onboarding_skip), color = TextMuted)
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

/**
 * The subject list, matching the web's `COURSE_DOMAINS`.
 *
 * These are the same strings a course's `domain` carries, so a preference lines up with
 * a course without a mapping table. They are French in both locales on purpose: they are
 * data the server stores and compares, not UI copy, and translating them would stop
 * preferences matching courses.
 */
private val COURSE_DOMAINS = listOf(
    "Informatique", "Mathématiques", "Physique", "Biologie", "Chimie",
    "Langues", "Littérature", "Économie", "Droit", "Médecine", "Commerce"
)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SelectRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PrimaryLight else Surface)
            .border(1.dp, if (selected) Primary else Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

/** Wrapping chips. FlowRow keeps long subject lists from running off a narrow phone. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val isOn = option in selected
            FilterChip(
                selected = isOn,
                onClick = { onToggle(option) },
                label = { Text(option) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryLight,
                    selectedLabelColor = Primary
                )
            )
        }
    }
}
