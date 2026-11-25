package com.example.studyplatform.api

import com.example.studyplatform.i18n.Msg

import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {

    /**
     * Where the API lives.
     *
     * A `var` rather than a constant so a build can point at a laptop instead of the
     * deployed server without editing source. The platform entry point sets it before
     * anything makes a request; leaving it alone gives the deployed default.
     *
     * Running against a local backend needs the right host for the client, and the
     * obvious one is wrong: `localhost` on a phone or emulator means *the phone*.
     * An emulator reaches the host machine at `10.0.2.2`; a physical device needs the
     * laptop's address on the same network.
     */
    private const val DEPLOYED = "https://studyplatform-api.onrender.com"
    private const val SERVER_KEY = "server_url"

    private val settings = Settings()

    /** What this build was configured with — the deployed server unless set otherwise. */
    private var buildUrl: String = DEPLOYED

    /**
     * The server the app talks to: one chosen on the phone if there is one, otherwise
     * the build's.
     *
     * The phone's choice exists for showing the app off. A demo runs against a laptop,
     * and the laptop's address changes with every network it joins; baking it into the
     * APK meant rebuilding at the venue. It is set from the welcome screen.
     */
    var baseUrl: String = settings.getStringOrNull(SERVER_KEY) ?: DEPLOYED
        private set

    /**
     * Called once at startup with the build's setting.
     *
     * Ignores a blank value so an unset build setting falls back to the deployed server
     * rather than pointing the app at nothing, and never overrides a server chosen on
     * the phone.
     */
    fun useBaseUrl(url: String?) {
        if (!url.isNullOrBlank()) buildUrl = url.trimEnd('/')
        baseUrl = settings.getStringOrNull(SERVER_KEY) ?: buildUrl
    }

    /**
     * Whether an EduNova server answers at [url], checked before switching to it so a
     * mistyped address is caught on the spot rather than as a broken app afterwards.
     */
    suspend fun isReachable(url: String): Boolean = try {
        val cleaned = url.trim().trimEnd('/')
            .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
        client.get("$cleaned/api/auth/providers") {
            timeout { requestTimeoutMillis = 8_000; connectTimeoutMillis = 8_000 }
        }.status.value == 200
    } catch (e: Exception) {
        false
    }

    /** True when the server was chosen on this phone rather than by the build. */
    fun isServerChosenOnDevice(): Boolean = settings.getStringOrNull(SERVER_KEY) != null

    /**
     * Points the app at another server, or back at the build's with null.
     *
     * Signs out as it does: a session belongs to the server that issued it, and sending
     * it to a different one only produces confusing 401s.
     */
    fun chooseServer(url: String?) {
        val cleaned = url?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
            ?.let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
        if (cleaned == null) settings.remove(SERVER_KEY) else settings.putString(SERVER_KEY, cleaned)
        listOf("access_token", "refresh_token", "user_id", "user_email",
               "user_first_name", "user_last_name", "user_account_type").forEach(settings::remove)
        baseUrl = cleaned ?: buildUrl
    }

    // Delegates to AppJson so the data layer can share the configuration without
    // pulling the HTTP client and the settings store in behind it.
    val json = AppJson.instance

    val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("HTTP: $message")
                }
            }
            // Headers, not bodies, and never the token. At ALL this logged every request
            // body — passwords at sign-in included — and the bearer token on every call,
            // to a log any app with log access on a debug device could read.
            level = LogLevel.HEADERS
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 90_000  // 90s — Render cold start + AI generation
            connectTimeoutMillis = 30_000  // 30s — Render wake-up time
            socketTimeoutMillis = 90_000
        }
        // Group chat. A WebSocket is a long-lived connection, so the timeouts above
        // must not apply to it — Ktor exempts WebSocket sessions from HttpTimeout,
        // which is why this can share the client rather than needing its own.
        install(WebSockets)
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            val token = settings.getStringOrNull("access_token")
            if (token != null) {
                header("Authorization", "Bearer $token")
            }
        }
        HttpResponseValidator {
            validateResponse { response ->
                val status = response.status.value
                if (status >= 400) {
                    val body = try { response.bodyAsText() } catch (_: Exception) { "" }
                    println("API ERROR [$status]: $body")
                    val userMessage = when (status) {
                        401 -> Msg.t("Session expired. Please sign in again.", "Session expirée. Veuillez vous reconnecter.")
                        403 -> Msg.t("You don't have permission to do this.", "Vous n'avez pas l'autorisation de faire cela.")
                        404 -> Msg.t("The requested resource was not found.", "L'élément demandé est introuvable.")
                        409 -> Msg.t("This action conflicts with existing data.", "Cette action entre en conflit avec des données existantes.")
                        422 -> Msg.t("Please check your input and try again.", "Vérifiez les informations saisies et réessayez.")
                        500 -> Msg.t("Something went wrong on our end. Please try again.", "Une erreur est survenue de notre côté. Réessayez.")
                        502, 503 -> Msg.t("The server is starting up. Please wait a moment and try again.", "Le serveur démarre. Patientez un instant puis réessayez.")
                        504 -> Msg.t("The request took too long. The server might be waking up — please try again.", "La requête a pris trop de temps. Le serveur est peut-être en train de démarrer — réessayez.")
                        else -> Msg.t("Something went wrong. Please try again.", "Une erreur est survenue. Réessayez.")
                    }
                    throw ApiException(status, userMessage, body)
                }
            }
        }
    }

    fun saveTokens(access: String, refresh: String) {
        settings.putString("access_token", access)
        settings.putString("refresh_token", refresh)
    }

    fun saveUser(userId: String, email: String, firstName: String, lastName: String, accountType: String) {
        settings.putString("user_id", userId)
        settings.putString("user_email", email)
        settings.putString("user_first_name", firstName)
        settings.putString("user_last_name", lastName)
        settings.putString("user_account_type", accountType)
    }

    fun getUserId(): String? = settings.getStringOrNull("user_id")
    fun getFirstName(): String? = settings.getStringOrNull("user_first_name")
    fun getAccountType(): String? = settings.getStringOrNull("user_account_type")
    fun getAccessToken(): String? = settings.getStringOrNull("access_token")
    fun getRefreshToken(): String? = settings.getStringOrNull("refresh_token")
    fun isLoggedIn(): Boolean = settings.getStringOrNull("access_token") != null

    /** Signs out. Keeps the chosen server: signing out is not a reason to lose it. */
    fun clearAll() {
        val server = settings.getStringOrNull(SERVER_KEY)
        settings.clear()
        if (server != null) settings.putString(SERVER_KEY, server)
    }
}

class ApiException(val statusCode: Int, override val message: String, val rawBody: String) : Exception(message)
