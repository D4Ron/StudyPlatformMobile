package com.example.studyplatform.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.*
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.api.AuthApi
import kotlinx.coroutines.launch
import tg.edunova.app.R

/**
 * The first screen someone who is not signed in sees.
 *
 * A login form asks for a decision from a person who has not been given a reason to
 * make it. This screen makes the case first — what the app does, and specifically that
 * it works offline, which is the part that matters most to the students it is for and
 * the part a form cannot say.
 *
 * Signing in stays one tap away rather than being buried: someone who already has an
 * account is not the audience for the pitch, and making them scroll past it would be
 * charging returning users for the benefit of new ones.
 */
@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onBrowse: () -> Unit,
    onDemoSignedIn: () -> Unit
) {
    var demoAvailable by remember { mutableStateOf(false) }
    var demoLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // The server decides whether this build can offer a demo. Asked once, and a failure
    // simply leaves the button hidden — the email path is always there.
    LaunchedEffect(Unit) {
        demoAvailable = AuthApi.providers()["demo"] == true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(PrimaryLight.copy(alpha = 0.45f), Background))
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            AnimatedEntry {
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Primary, Secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("E", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(Modifier.height(22.dp))

            AnimatedEntry(index = 1) {
                Text(
                    stringResource(R.string.welcome_headline),
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(10.dp))

            AnimatedEntry(index = 2) {
                Text(
                    stringResource(R.string.welcome_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(30.dp))

            SellingPoint(
                index = 3,
                icon = Icons.Filled.AutoAwesome,
                tint = Primary,
                title = stringResource(R.string.welcome_point_ai_title),
                body = stringResource(R.string.welcome_point_ai_body)
            )
            SellingPoint(
                index = 4,
                icon = Icons.Filled.CloudOff,
                tint = Success,
                title = stringResource(R.string.welcome_point_offline_title),
                body = stringResource(R.string.welcome_point_offline_body)
            )
            SellingPoint(
                index = 5,
                icon = Icons.Filled.Groups,
                tint = Secondary,
                title = stringResource(R.string.welcome_point_groups_title),
                body = stringResource(R.string.welcome_point_groups_body)
            )

            Spacer(Modifier.height(26.dp))

            error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Accent,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
            }

            AnimatedEntry(index = 6) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PrimaryButton(
                        text = stringResource(R.string.welcome_get_started),
                        onClick = onCreateAccount
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(
                        text = stringResource(R.string.welcome_sign_in),
                        onClick = onSignIn
                    )

                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onBrowse) {
                        Text(stringResource(R.string.welcome_browse), color = Primary)
                    }

                    if (demoAvailable) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                demoLoading = true
                                error = null
                                scope.launch {
                                    runCatching { AuthApi.demo() }
                                        .onSuccess { onDemoSignedIn() }
                                        .onFailure {
                                            // The server is the authority. If it says no,
                                            // stop offering the button this session.
                                            demoAvailable = false
                                            error = it.message
                                                ?: "Demo mode is not available right now."
                                        }
                                    demoLoading = false
                                }
                            },
                            enabled = !demoLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (demoLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(R.string.welcome_demo))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.welcome_demo_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SellingPoint(
    index: Int,
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String
) {
    AnimatedEntry(index = index) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 9.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}
