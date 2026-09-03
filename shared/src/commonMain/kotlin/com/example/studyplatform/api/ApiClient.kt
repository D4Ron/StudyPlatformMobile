package com.example.studyplatform.api

import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {
    const val BASE_URL = "https://studyplatform-api.onrender.com"

    private val settings = Settings()

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
            level = LogLevel.ALL
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 90_000  // 90s — Render cold start + AI generation
            connectTimeoutMillis = 30_000  // 30s — Render wake-up time
            socketTimeoutMillis = 90_000
        }
        defaultRequest {
            url(BASE_URL)
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
                        401 -> "Session expired. Please sign in again."
                        403 -> "You don't have permission to do this."
                        404 -> "The requested resource was not found."
                        409 -> "This action conflicts with existing data."
                        422 -> "Please check your input and try again."
                        500 -> "Something went wrong on our end. Please try again."
                        502, 503 -> "The server is starting up. Please wait a moment and try again."
                        504 -> "The request took too long. The server might be waking up — please try again."
                        else -> "Something went wrong. Please try again."
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

    fun clearAll() { settings.clear() }
}

class ApiException(val statusCode: Int, override val message: String, val rawBody: String) : Exception(message)
