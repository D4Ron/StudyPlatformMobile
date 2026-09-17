package com.example.studyplatform

import androidx.compose.ui.window.ComposeUIViewController
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.DatabaseDriverFactory
import com.example.studyplatform.data.IosSync
import com.example.studyplatform.ui.EduNovaApp
import com.example.studyplatform.ui.theme.StudyPlatformTheme
import platform.UIKit.UIViewController

/**
 * The iOS entry point.
 *
 * Rendered the Compose template's "Click me!" screen until now, because every real
 * screen lived in the Android app. They are in `commonMain`, so this renders the same
 * application Android does.
 *
 * Two gaps against Android, both deliberate and both named where they live:
 *
 * - **Foreground-only sync.** [IosSync] runs a requested sync while the app is open.
 *   Android's WorkManager keeps going after the app closes and waits for a connection;
 *   matching that needs `BGTaskScheduler`, which requires identifiers in `Info.plist`
 *   and registration from the app delegate, so it belongs in Swift.
 * - **No federated sign-in button.** The slot defaults to empty, which is correct:
 *   Credential Manager is Android's, and iOS would want Sign in with Apple.
 *
 * Deep links are not wired either. `EduNovaApp` takes a route, so it is a matter of
 * resolving one from the `UIApplication` open-URL callback the way `MainActivity` does
 * from an `Intent` — but that belongs in Swift, next to the URL types in Info.plist.
 */
fun MainViewController(): UIViewController {
    // Before anything composes, and outside the content lambda: that lambda runs on
    // every recomposition, and opening the database is not something to re-enter.
    // `init` is idempotent, but relying on that to paper over the wrong call site
    // would be the kind of thing that stops being true later.
    AppData.init(DatabaseDriverFactory())
    IosSync.install()

    return ComposeUIViewController {
        StudyPlatformTheme {
            EduNovaApp()
        }
    }
}
