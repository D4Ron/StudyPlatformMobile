package com.example.studyplatform.data

import com.example.studyplatform.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * iOS's answer to "push what is queued, now".
 *
 * Android hands this to WorkManager, which runs it even after the app closes and waits
 * for a connection. iOS has no equivalent that a Kotlin module can reach on its own —
 * `BGTaskScheduler` needs identifiers declared in `Info.plist` and registered from the
 * app delegate, so it belongs in Swift.
 *
 * Until that exists, this runs the sync in the foreground: while the app is open, a
 * screen that asks for a sync gets one. That is a real weakening of the offline story
 * rather than a detail — work queued and then backgrounded waits for the next launch —
 * and it is why this file says so rather than quietly looking finished.
 *
 * The outbox is what makes that survivable. Nothing is lost by a sync that does not
 * happen; it is only late.
 */
object IosSync {

    /**
     * Its own scope, not the caller's.
     *
     * A sync outlives the screen that asked for it — the note editor that triggered one
     * may well be gone before the upload finishes — so tying it to a composition scope
     * would cancel it halfway. `SupervisorJob` so one failed sync does not poison later
     * ones.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Called once at startup, before any screen composes. */
    fun install() {
        SyncTrigger.install {
            // Signed out there is nothing to push, and the request would 401.
            if (!ApiClient.isLoggedIn()) return@install
            scope.launch {
                runCatching { AppData.syncEngine.sync() }
            }
        }
    }
}
