package com.example.studyplatform.android.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.PrimaryButton
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.AuthApi
import com.example.studyplatform.model.VerifyOtpRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CODE_LENGTH = 6
private const val RESEND_COOLDOWN_SECONDS = 60

/**
 * Confirms the code emailed at registration. This is the step that actually signs a
 * new user in — registration alone creates an unverified account with no session.
 */
@Composable
fun VerifyOtpScreen(
    email: String,
    groupInviteCode: String? = null,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var secondsUntilResend by remember { mutableIntStateOf(RESEND_COOLDOWN_SECONDS) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var arrived by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { arrived = true }
    val markScale by animateFloatAsState(
        targetValue = if (arrived) 1f else 0.7f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "mark"
    )

    // The backend rate-limits resends; mirroring that as a visible countdown is
    // friendlier than letting the user tap into an error.
    LaunchedEffect(secondsUntilResend) {
        if (secondsUntilResend > 0) {
            delay(1_000)
            secondsUntilResend--
        }
    }

    fun submit() {
        loading = true
        error = null
        notice = null
        scope.launch {
            try {
                AuthApi.verifyOtp(VerifyOtpRequest(email.trim(), code, groupInviteCode))
                onVerified()
            } catch (e: Exception) {
                error = e.message ?: "That code is invalid or has expired."
                code = ""
            } finally {
                loading = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryLight.copy(alpha = 0.3f), Background)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 420.dp)
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(96.dp))

            Box(
                Modifier
                    .size(76.dp)
                    .scale(markScale)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Primary, Secondary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MarkEmailRead, null, tint = Color.White, modifier = Modifier.size(34.dp))
            }

            Spacer(Modifier.height(20.dp))
            Text("Check your email", style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("We sent a $CODE_LENGTH-digit code to", style = MaterialTheme.typography.bodyLarge,
                color = TextMuted, textAlign = TextAlign.Center)
            Text(email, style = MaterialTheme.typography.titleMedium,
                color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(36.dp))

            AnimatedVisibility(error != null, enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()) {
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(error ?: "", Modifier.padding(14.dp), color = Accent, style = MaterialTheme.typography.bodyMedium)
                }
            }

            AnimatedVisibility(notice != null, enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()) {
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(notice ?: "", Modifier.padding(14.dp), color = Primary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            OutlinedTextField(
                value = code,
                onValueChange = { entered ->
                    val digits = entered.filter { it.isDigit() }.take(CODE_LENGTH)
                    code = digits
                    error = null
                    // Six digits is unambiguous — submit without making the user
                    // hunt for a button.
                    if (digits.length == CODE_LENGTH && !loading) {
                        focusManager.clearFocus()
                        submit()
                    }
                },
                label = { Text("Verification code") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { if (code.length == CODE_LENGTH) submit() }),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    textAlign = TextAlign.Center, letterSpacing = 12.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary, unfocusedBorderColor = Border,
                    focusedContainerColor = Surface, unfocusedContainerColor = Surface
                )
            )

            Spacer(Modifier.height(28.dp))

            PrimaryButton(
                text = if (loading) "Verifying\u2026" else "Verify",
                loading = loading,
                enabled = code.length == CODE_LENGTH,
                onClick = { submit() }
            )

            Spacer(Modifier.height(20.dp))

            TextButton(
                enabled = secondsUntilResend == 0 && !loading,
                onClick = {
                    scope.launch {
                        try {
                            AuthApi.resendOtp(email.trim())
                            notice = "A new code is on its way."
                            error = null
                            secondsUntilResend = RESEND_COOLDOWN_SECONDS
                        } catch (e: Exception) {
                            error = e.message ?: "Could not resend the code."
                        }
                    }
                }
            ) {
                Text(
                    if (secondsUntilResend > 0) "Resend code in ${secondsUntilResend}s"
                    else "Didn't get it? Resend code",
                    color = if (secondsUntilResend > 0) TextMuted else Primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            TextButton(onClick = onBack) {
                Text("Use a different email", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
