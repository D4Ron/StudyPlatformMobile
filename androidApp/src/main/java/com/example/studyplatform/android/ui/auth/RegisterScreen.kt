package com.example.studyplatform.android.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.GoogleSignInButton
import com.example.studyplatform.android.components.PrimaryButton
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.AuthApi
import com.example.studyplatform.model.RegisterRequest
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    /** Registration no longer signs the user in — it hands off to email verification. */
    onRegisterSuccess: (email: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    /** Google signs the user straight in - there is no code to confirm. */
    onGoogleSignUp: () -> Unit = {}
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val markScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "mark"
    )

    val strength = when {
        password.length < 6 -> 0
        password.length < 8 -> 1
        password.any { it.isUpperCase() } && password.any { it.isDigit() } -> 3
        password.length >= 8 -> 2
        else -> 1
    }
    val strengthColor = when(strength) { 0 -> Accent; 1 -> Warning; 2 -> Warning; else -> Success }
    val strengthLabel = when(strength) { 0 -> "Too short"; 1 -> "Fair"; 2 -> "Good"; else -> "Strong" }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SecondaryLight.copy(alpha = 0.3f), Background)))) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 40 }) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 420.dp)
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(70.dp))
                Box(
                    Modifier.size(76.dp).scale(markScale).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Secondary, Primary))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", style = MaterialTheme.typography.displayMedium, color = androidx.compose.ui.graphics.Color.White)
                }
                Spacer(Modifier.height(20.dp))
                Text(stringResource(tg.edunova.app.R.string.auth_create_account), style = MaterialTheme.typography.headlineLarge, color = TextPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(tg.edunova.app.R.string.auth_start_journey), style = MaterialTheme.typography.bodyLarge, color = TextMuted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))

                AnimatedVisibility(visible = error != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Card(Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = AccentLight), shape = RoundedCornerShape(12.dp)) {
                        Text(error ?: "", Modifier.padding(14.dp), color = Accent, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Google here skips the whole OTP round trip: Google has already
                // proved the person controls the address, so there is no code to wait
                // for and no email that might not arrive.
                GoogleSignInButton(
                    onSignedIn = onGoogleSignUp,
                    onError = { error = it }
                )

                val tfColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Border, focusedContainerColor = Surface, unfocusedContainerColor = Surface)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text(stringResource(tg.edunova.app.R.string.auth_first_name)) },
                        leadingIcon = { Icon(Icons.Outlined.Person, null, tint = TextMuted) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }), colors = tfColors)
                    OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text(stringResource(tg.edunova.app.R.string.auth_last_name)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), colors = tfColors)
                }
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(value = email, onValueChange = { email = it; error = null }, label = { Text(stringResource(tg.edunova.app.R.string.auth_email)) },
                    leadingIcon = { Icon(Icons.Outlined.Email, null, tint = TextMuted) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), colors = tfColors)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(stringResource(tg.edunova.app.R.string.auth_password)) },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = TextMuted) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }), colors = tfColors)

                if (password.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        // The bars fill and recolour as the password improves, so the
                        // rating is something you watch happen rather than a verdict.
                        repeat(4) { i ->
                            val segment by animateColorAsState(
                                targetValue = if (i <= strength) strengthColor else Border,
                                animationSpec = tween(220),
                                label = "strength$i"
                            )
                            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(segment))
                        }
                        Spacer(Modifier.width(8.dp))
                        AnimatedContent(
                            targetState = strengthLabel,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                            label = "strength-label"
                        ) { label ->
                            Text(label, style = MaterialTheme.typography.labelMedium, color = strengthColor)
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
                PrimaryButton(text = if (loading) stringResource(tg.edunova.app.R.string.auth_creating_account) else stringResource(tg.edunova.app.R.string.auth_create_account), loading = loading,
                    enabled = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() && password.length >= 6,
                    onClick = {
                        loading = true; error = null
                        scope.launch {
                            try {
                                val registration = AuthApi.register(
                                    RegisterRequest(firstName.trim(), lastName.trim(), email.trim(), password))
                                onRegisterSuccess(registration.email)
                            }
                            catch (e: Exception) { error = e.message ?: "Registration failed. Please try again." }
                            finally { loading = false }
                        }
                    })
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onNavigateToLogin) {
                    Text(stringResource(tg.edunova.app.R.string.auth_have_account), color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    Text("Sign in", color = Primary, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
