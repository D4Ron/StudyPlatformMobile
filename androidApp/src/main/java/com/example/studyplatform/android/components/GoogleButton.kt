package com.example.studyplatform.android.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.auth.GoogleSignIn
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.AuthApi
import kotlinx.coroutines.launch

/**
 * "Continue with Google" — drawn only when it can actually work.
 *
 * Both ends have to be configured: this build needs a client id, and the server needs
 * one too. Either missing produces a button that fails *after* the person has picked an
 * account, which is a worse experience than never offering it. So the button asks first
 * and stays hidden otherwise.
 *
 * @param onSignedIn called after the backend has exchanged the Google token for a
 *                   session; the caller navigates.
 */
@Composable
fun GoogleSignInButton(
    onSignedIn: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    groupInviteCode: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var available by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        available = GoogleSignIn.isConfigured &&
                AuthApi.providers()["google"] == true
    }

    AnimatedVisibility(
        visible = available,
        enter = Motion.expand(),
        exit = Motion.collapse(),
        modifier = modifier
    ) {
        Column(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val idToken = GoogleSignIn.requestIdToken(context)
                            AuthApi.google(idToken, groupInviteCode)
                            onSignedIn()
                        } catch (e: GoogleSignIn.Cancelled) {
                            // Closing the sheet is a decision, not a failure. Saying
                            // "sign-in failed" for it would be telling someone off for
                            // changing their mind.
                        } catch (e: Exception) {
                            onError(e.message ?: "Google sign-in didn't complete.")
                        }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, Border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Surface,
                    contentColor = TextPrimary
                )
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Primary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                } else {
                    // Google's own mark is a licensed asset with brand rules attached, so
                    // this stands in for it until the real one is added to res/drawable.
                    Box(
                        Modifier.size(20.dp).clip(MaterialTheme.shapes.extraSmall)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4285F4), Color(0xFF34A853))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    if (busy) stringResource(tg.edunova.app.R.string.auth_signing_in) else stringResource(tg.edunova.app.R.string.auth_continue_with_google),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = Border)
                Text(
                    stringResource(tg.edunova.app.R.string.auth_or),
                    color = TextMuted,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(Modifier.weight(1f), color = Border)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
