package com.example.studyplatform.api

import kotlinx.serialization.json.Json

/**
 * How this app reads and writes JSON, everywhere.
 *
 * Separate from [ApiClient] on purpose. The data layer needs this configuration to store
 * payloads in the local database, and it has nothing to do with the network — but while
 * it lived on `ApiClient`, touching it dragged in the HTTP client, the logger, and the
 * settings store. On Android that meant a `Context`, which made the whole offline layer
 * impossible to test off-device: the first repository call failed in `ApiClient.<clinit>`
 * with a null Context, and every test after it with `NoClassDefFoundError`.
 *
 * `ignoreUnknownKeys` matters twice over here. It lets the client survive the server
 * adding a field, and it lets a payload cached by an older build of the app still parse
 * after an update rather than being silently discarded.
 */
object AppJson {
    val instance = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
}
