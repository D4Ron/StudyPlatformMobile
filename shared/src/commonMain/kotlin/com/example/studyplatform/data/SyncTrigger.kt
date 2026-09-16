package com.example.studyplatform.data

/**
 * "Push what is queued, now."
 *
 * Shared screens need to ask for a sync — after writing a note, after finishing a quiz,
 * from the settings button — but *how* a sync gets scheduled is entirely a platform
 * matter. Android hands it to WorkManager so it survives the app being closed and waits
 * for a connection; iOS has no equivalent and will want something else.
 *
 * So the platform installs a trigger at startup and shared code just asks. That avoids an
 * `expect`/`actual` pair, which would not have worked cleanly anyway: the Android
 * implementation needs a `Context`, and a common signature has nowhere to put one.
 *
 * Uninstalled, [now] does nothing rather than throwing. A screen asking for a sync is
 * making a request, not a demand — the outbox still holds the work, and the next
 * scheduled run will carry it. Crashing a note editor because no trigger was registered
 * would be the wrong trade.
 */
object SyncTrigger {

    private var trigger: (() -> Unit)? = null

    /** Called once, by the platform, before any screen composes. */
    fun install(trigger: () -> Unit) {
        this.trigger = trigger
    }

    fun now() {
        trigger?.invoke()
    }
}
