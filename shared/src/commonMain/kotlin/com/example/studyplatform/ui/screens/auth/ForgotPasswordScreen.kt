package com.example.studyplatform.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.studyplatform.api.AuthApi
import com.example.studyplatform.i18n.Msg
import com.example.studyplatform.ui.components.PrimaryButton
import com.example.studyplatform.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Two steps on one screen: ask for a code, then use it.
 *
 * The first step says the same thing whether or not the address has an account — the
 * server deliberately does not tell, and this screen must not guess on its behalf.
 * A successful reset signs the person in, so they land where a login would have taken
 * them rather than being sent back to type the password they just chose.
 */
@Composable
fun ForgotPasswordScreen(
    initialEmail: String = "",
    onSignedIn: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary, unfocusedBorderColor = Border,
        focusedContainerColor = Surface, unfocusedContainerColor = Surface
    )

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryLight.copy(alpha = 0.3f), Background))),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier.fillMaxSize().widthIn(max = 420.dp)
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))
            Text(
                Msg.t("Reset your password", "Réinitialiser le mot de passe"),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (!codeSent) Msg.t(
                    "Enter your account's email address. We'll send you a 6-digit code.",
                    "Saisissez l'adresse e-mail de votre compte. Nous vous enverrons un code à 6 chiffres."
                ) else Msg.t(
                    "If an account exists for $email, a code is on its way. Enter it with your new password.",
                    "Si un compte existe pour $email, un code vient d'être envoyé. Saisissez-le avec votre nouveau mot de passe."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            error?.let {
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(it, Modifier.padding(14.dp), color = Accent, style = MaterialTheme.typography.bodyMedium)
                }
            }

            OutlinedTextField(
                value = email, onValueChange = { email = it; error = null },
                label = { Text(Msg.t("Email", "E-mail")) },
                leadingIcon = { Icon(Icons.Outlined.Email, null, tint = TextMuted) },
                enabled = !codeSent,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp), singleLine = true, colors = fieldColors
            )

            if (codeSent) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { v -> code = v.filter { it.isDigit() }.take(6); error = null },
                    label = { Text(Msg.t("6-digit code", "Code à 6 chiffres")) },
                    leadingIcon = { Icon(Icons.Outlined.Pin, null, tint = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp), singleLine = true, colors = fieldColors
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it; error = null },
                    label = { Text(Msg.t("New password (8 characters min.)", "Nouveau mot de passe (8 caractères min.)")) },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = TextMuted) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp), singleLine = true, colors = fieldColors
                )
            }

            Spacer(Modifier.height(24.dp))

            if (!codeSent) {
                PrimaryButton(
                    text = Msg.t("Send the code", "Envoyer le code"),
                    loading = loading,
                    enabled = email.contains('@'),
                    onClick = {
                        loading = true; error = null
                        scope.launch {
                            runCatching { AuthApi.requestPasswordReset(email.trim()) }
                                .onSuccess { codeSent = true }
                                .onFailure { error = it.message }
                            loading = false
                        }
                    }
                )
            } else {
                PrimaryButton(
                    text = Msg.t("Set the new password", "Définir le nouveau mot de passe"),
                    loading = loading,
                    enabled = code.length == 6 && password.length >= 8,
                    onClick = {
                        loading = true; error = null
                        scope.launch {
                            runCatching { AuthApi.confirmPasswordReset(email.trim(), code, password) }
                                .onSuccess { onSignedIn() }
                                .onFailure { error = it.message }
                            loading = false
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { codeSent = false; code = ""; error = null }) {
                    Text(Msg.t("Use another address or resend", "Changer d'adresse ou renvoyer"), color = Primary)
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack) {
                Text(Msg.t("Back to sign in", "Retour à la connexion"), color = TextMuted)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
