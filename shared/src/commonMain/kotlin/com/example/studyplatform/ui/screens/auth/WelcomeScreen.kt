package com.example.studyplatform.ui.screens.auth

import com.example.studyplatform.i18n.Msg

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
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.ui.components.*
import com.example.studyplatform.ui.theme.*
import com.example.studyplatform.api.AuthApi
import com.example.studyplatform.api.ApiClient
import kotlinx.coroutines.launch
import studyplatform.shared.generated.resources.*

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
    // Re-asked whenever the server changes: demo mode is that server's decision.
    var serverVersion by remember { mutableStateOf(0) }
    LaunchedEffect(serverVersion) {
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
                // The real mark. This was a gradient circle with a letter E in it — a
                // stand-in from before there was a logo, and the first thing anyone
                // opening the app saw.
                Image(
                    painter = painterResource(Res.drawable.logo_mark),
                    contentDescription = null,
                    modifier = Modifier.size(76.dp)
                )
            }

            Spacer(Modifier.height(22.dp))

            AnimatedEntry(index = 1) {
                Text(
                    stringResource(Res.string.welcome_headline),
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(10.dp))

            AnimatedEntry(index = 2) {
                Text(
                    stringResource(Res.string.welcome_sub),
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
                title = stringResource(Res.string.welcome_point_ai_title),
                body = stringResource(Res.string.welcome_point_ai_body)
            )
            SellingPoint(
                index = 4,
                icon = Icons.Filled.CloudOff,
                tint = Success,
                title = stringResource(Res.string.welcome_point_offline_title),
                body = stringResource(Res.string.welcome_point_offline_body)
            )
            SellingPoint(
                index = 5,
                icon = Icons.Filled.Groups,
                tint = Secondary,
                title = stringResource(Res.string.welcome_point_groups_title),
                body = stringResource(Res.string.welcome_point_groups_body)
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
                        text = stringResource(Res.string.welcome_get_started),
                        onClick = onCreateAccount
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(
                        text = stringResource(Res.string.welcome_sign_in),
                        onClick = onSignIn
                    )

                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onBrowse) {
                        Text(stringResource(Res.string.welcome_browse), color = Primary)
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
                                                ?: Msg.t("Demo mode is not available right now.", "Le mode démo n'est pas disponible pour le moment.")
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
                                Text(stringResource(Res.string.welcome_demo))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(Res.string.welcome_demo_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            ServerChooser(onChanged = { serverVersion++ })
            Spacer(Modifier.height(28.dp))
        }
    }
}

/**
 * Which server this phone talks to, and a way to change it.
 *
 * For showing the app: the backend runs on a laptop whose address depends on the
 * network of the day, so the address is typed here, tested, and kept on the phone.
 */
@Composable
private fun ServerChooser(onChanged: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf(ApiClient.baseUrl) }
    var draft by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TextButton(onClick = { draft = if (ApiClient.isServerChosenOnDevice()) current else ""; problem = null; open = true }) {
        Text(
            Msg.t("Server: ", "Serveur : ") + current.removePrefix("https://").removePrefix("http://"),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { if (!checking) open = false },
            title = { Text(Msg.t("Server address", "Adresse du serveur")) },
            text = {
                Column {
                    Text(
                        Msg.t(
                            "For a demo on a laptop: its address on this Wi-Fi, e.g. 192.168.1.20:8080.",
                            "Pour une démo sur un ordinateur : son adresse sur ce Wi-Fi, par ex. 192.168.1.20:8080."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it; problem = null },
                        singleLine = true,
                        placeholder = { Text("192.168.1.20:8080") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    problem?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Accent)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !checking && draft.isNotBlank(),
                    onClick = {
                        checking = true
                        scope.launch {
                            if (ApiClient.isReachable(draft)) {
                                ApiClient.chooseServer(draft)
                                current = ApiClient.baseUrl
                                open = false
                                onChanged()
                            } else {
                                problem = Msg.t(
                                    "No EduNova server answers at that address. Check the address and that both devices are on the same network.",
                                    "Aucun serveur EduNova ne répond à cette adresse. Vérifiez l'adresse et que les deux appareils sont sur le même réseau."
                                )
                            }
                            checking = false
                        }
                    }
                ) { Text(if (checking) Msg.t("Checking…", "Vérification…") else Msg.t("Test and use", "Tester et utiliser")) }
            },
            dismissButton = {
                TextButton(
                    enabled = !checking,
                    onClick = {
                        ApiClient.chooseServer(null)
                        current = ApiClient.baseUrl
                        open = false
                        onChanged()
                    }
                ) { Text(Msg.t("Default server", "Serveur par défaut")) }
            }
        )
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
