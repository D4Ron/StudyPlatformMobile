package com.example.studyplatform.i18n

import androidx.compose.ui.text.intl.Locale

/**
 * A message in the phone's language, for text produced outside a screen.
 *
 * <p>The screens read their strings from composeResources, in English and French. The
 * network layer could not: it throws its errors from code that runs outside any
 * composition, where resources are only reachable asynchronously, so it threw them in
 * English — sixty-one messages, every error a student could meet, in English inside a
 * French app. This keeps both languages beside each other at the point of use, and
 * follows the device the same way the resource files do.
 */
object Msg {
    fun t(en: String, fr: String): String =
        if (Locale.current.language == "fr") fr else en
}
