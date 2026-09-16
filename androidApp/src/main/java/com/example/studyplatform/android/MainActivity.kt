package com.example.studyplatform.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.example.studyplatform.android.sync.SyncWorker
import com.example.studyplatform.api.ApiClient
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.DatabaseDriverFactory
import com.example.studyplatform.data.SyncTrigger
import com.example.studyplatform.ui.EduNovaApp
import com.example.studyplatform.ui.components.GoogleSignInButton
import com.example.studyplatform.ui.theme.StudyPlatformTheme
import tg.edunova.app.BuildConfig

/**
 * Android's half of the app.
 *
 * The screens, the navigation graph and the bottom bar all live in `commonMain` so iOS
 * renders the same thing — see `EduNovaApp`. What is left here is genuinely Android:
 * process startup, WorkManager, Credential Manager, and turning an `Intent`'s `Uri`
 * into a route.
 */
class MainActivity : ComponentActivity() {

    /**
     * The route a link asked for, if any.
     *
     * Held as state rather than read once, because `singleTask` means a second link
     * arrives at `onNewIntent` on the running instance instead of starting a new one.
     */
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Before anything can make a request. Blank in a normal build, so this is a
        // no-op unless someone has pointed it at a local backend.
        ApiClient.useBaseUrl(BuildConfig.API_BASE_URL)

        // Opened before any screen composes: the notes list reads from it immediately,
        // and it has to exist whether or not there is a connection.
        AppData.init(DatabaseDriverFactory(applicationContext))
        SyncWorker.schedule(applicationContext)
        if (ApiClient.isLoggedIn()) SyncWorker.syncNow(applicationContext)

        // Shared screens ask for a sync without knowing how one is scheduled. On Android
        // that is WorkManager, so it survives the app closing and waits for a connection;
        // iOS will install something else here.
        SyncTrigger.install { SyncWorker.syncNow(applicationContext) }

        pendingRoute.value = routeForLink(intent?.data)

        setContent {
            StudyPlatformTheme {
                EduNovaApp(
                    pendingRoute = pendingRoute.value,
                    onRouteHandled = { pendingRoute.value = null },
                    // Credential Manager is Android's, so the shared screens take the
                    // button as a slot rather than owning it.
                    googleButton = { onSignedIn ->
                        GoogleSignInButton(onSignedIn = onSignedIn, onError = {})
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = routeForLink(intent.data)
    }
}

/**
 * Where an `edunova://` link goes.
 *
 * The URI comes from outside the app — anything can send one — so it is matched against
 * a closed set of destinations rather than treated as a route. An unknown host returns
 * null and the app just opens normally, which is the right outcome for a link this build
 * does not understand.
 *
 * Signed-out users are not deep-linked into authenticated screens; the link is dropped
 * rather than producing a screen that immediately fails to load.
 */
private fun routeForLink(uri: Uri?): String? {
    if (uri == null) return null
    val id = uri.pathSegments?.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
    val loggedIn = ApiClient.isLoggedIn()
    return when (uri.host) {
        "course" -> "guest/course/$id"
        "tournament" -> if (loggedIn) "tournaments/detail/$id" else null
        "group" -> if (loggedIn) "groups/detail/$id" else null
        else -> null
    }
}
