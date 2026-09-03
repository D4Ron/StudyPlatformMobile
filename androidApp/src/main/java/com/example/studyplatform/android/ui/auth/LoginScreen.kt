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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.GoogleSignInButton
import com.example.studyplatform.android.components.PrimaryButton
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.AuthApi
import com.example.studyplatform.api.EmailNotVerifiedException
import com.example.studyplatform.model.LoginRequest
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onEmailNotVerified: (String) -> Unit = {},
    onBrowseAsGuest: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // The mark settles into place rather than appearing at full size — the one piece of
    // motion on this screen, so it reads as arrival and not as decoration.
    val markScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "mark"
    )

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PrimaryLight.copy(alpha = 0.3f), Background)))) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 40 }) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // The form stays a readable column and stays centred, on a small
                    // phone and on a tablet alike.
                    .widthIn(max = 420.dp)
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(96.dp))
                Box(
                    Modifier.size(76.dp).scale(markScale).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Primary, Secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", style = MaterialTheme.typography.displayMedium, color = androidx.compose.ui.graphics.Color.White)
                }
                Spacer(Modifier.height(20.dp))
                Text("Welcome back", style = MaterialTheme.typography.headlineLarge, color = TextPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("Sign in to continue learning", style = MaterialTheme.typography.bodyLarge, color = TextMuted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(40.dp))

                AnimatedVisibility(visible = error != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Card(Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = AccentLight), shape = RoundedCornerShape(12.dp)) {
                        Text(error ?: "", Modifier.padding(14.dp), color = Accent, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Above the fields, not below them: someone who has an account and would
                // rather not type a password on a phone keyboard should see this first.
                GoogleSignInButton(
                    onSignedIn = onLoginSuccess,
                    onError = { error = it }
                )

                OutlinedTextField(
                    value = email, onValueChange = { email = it; error = null },
                    label = { Text("Email address") },
                    leadingIcon = { Icon(Icons.Outlined.Email, null, tint = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(14.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Border, focusedContainerColor = Surface, unfocusedContainerColor = Surface)
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = password, onValueChange = { password = it; error = null },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = TextMuted) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = TextMuted)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Border, focusedContainerColor = Surface, unfocusedContainerColor = Surface)
                )
                Spacer(Modifier.height(28.dp))

                PrimaryButton(text = if (loading) "Signing in..." else "Sign In", loading = loading, enabled = email.isNotBlank() && password.isNotBlank(), onClick = {
                    loading = true; error = null
                    scope.launch {
                        try { AuthApi.login(LoginRequest(email.trim(), password)); onLoginSuccess() }
                        catch (e: EmailNotVerifiedException) {
                            // Credentials were correct — the address just isn't confirmed.
                            // The backend has already re-sent a code.
                            onEmailNotVerified(e.email)
                        }
                        catch (e: Exception) { error = "Invalid email or password. Please try again." }
                        finally { loading = false }
                    }
                })
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = Border)
                    Text("  or  ", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                    HorizontalDivider(Modifier.weight(1f), color = Border)
                }
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onNavigateToRegister) {
                    Text("Don't have an account? ", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    Text("Sign up", color = Primary, style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.height(4.dp))

                // Deliberately on the sign-in screen rather than behind it. Someone
                // deciding whether this is worth an email address should be able to see
                // the library first — and at a school, signing up may not be their
                // decision to make.
                TextButton(onClick = onBrowseAsGuest) {
                    Text(
                        "Browse the library without an account",
                        color = Secondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
