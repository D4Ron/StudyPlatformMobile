package com.example.studyplatform.api

import com.example.studyplatform.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Sign-in succeeded on credentials but the address has not been confirmed yet.
 * The backend re-sends a code when this happens, so callers should send the user
 * to the verification screen.
 */
class EmailNotVerifiedException(val email: String) :
    Exception("Please confirm your email. We've sent you a new code.")

object AuthApi {

    /**
     * Exchanges a Google ID token for a session here.
     *
     * <p>The Google token is used once and never stored: everything after this runs on
     * this platform's own tokens, so a signed-in user does not depend on Google being
     * reachable for every subsequent request.
     */
    suspend fun google(idToken: String, groupInviteCode: String? = null): AuthResponse {
        try {
            val auth: AuthResponse = ApiClient.client.post("/api/auth/google") {
                setBody(GoogleSignInRequest(idToken, groupInviteCode))
            }.body()
            ApiClient.saveTokens(auth.accessToken, auth.refreshToken)
            ApiClient.saveUser(auth.userId, auth.email, auth.firstName, auth.lastName, auth.accountType)
            return auth
        } catch (e: ApiException) {
            println("Google sign-in failed [${e.statusCode}]: ${e.rawBody}")
            throw Exception(
                when (e.statusCode) {
                    400 -> "Google sign-in isn't available on this server yet."
                    401 -> "That Google sign-in could not be verified. Please try again."
                    else -> e.message
                }
            )
        } catch (e: Exception) {
            println("Google sign-in network error: ${e.message}")
            throw Exception("Could not reach the server. Please check your connection.")
        }
    }

    /**
     * Which sign-in methods this deployment actually supports.
     *
     * Asked before drawing the Google button. A button that cannot work is worse than no
     * button, because it fails only after the person has already chosen an account.
     * Defaults to none on any error — the email path always works.
     */
    suspend fun providers(): Map<String, Boolean> =
        try {
            ApiClient.client.get("/api/auth/providers").body()
        } catch (e: Exception) {
            println("Could not read auth providers: ${e.message}")
            emptyMap()
        }

    suspend fun login(request: LoginRequest): AuthResponse {
        try {
            val auth: AuthResponse = ApiClient.client.post("/api/auth/login") { setBody(request) }.body()
            ApiClient.saveTokens(auth.accessToken, auth.refreshToken)
            ApiClient.saveUser(auth.userId, auth.email, auth.firstName, auth.lastName, auth.accountType)
            return auth
        } catch (e: ApiException) {
            println("Login failed [${e.statusCode}]: ${e.rawBody}")
            when (e.statusCode) {
                401 -> throw Exception("Invalid email or password.")
                // The credentials were right but the address is unconfirmed. The
                // backend has just re-sent a code, so the caller should route to
                // verification rather than showing a dead end.
                403 -> throw EmailNotVerifiedException(request.email)
                else -> throw Exception(e.message)
            }
        } catch (e: Exception) {
            println("Login network error: ${e.message}")
            throw Exception("Could not connect to the server. Please check your connection and try again.")
        }
    }

    /**
     * Creates the account and triggers the verification email. No session is
     * established here — the user must confirm the emailed code via [verifyOtp].
     */
    suspend fun register(request: RegisterRequest): RegistrationResponse {
        try {
            return ApiClient.client.post("/api/auth/register") { setBody(request) }.body()
        } catch (e: ApiException) {
            println("Register failed [${e.statusCode}]: ${e.rawBody}")
            when (e.statusCode) {
                409 -> throw Exception("An account with this email already exists.")
                400 -> throw Exception("Please check your information and try again.")
                else -> throw Exception(e.message)
            }
        } catch (e: Exception) {
            if (e is ApiException) throw e
            println("Register network error: ${e.message}")
            throw Exception("Could not connect to the server. Please check your connection and try again.")
        }
    }

    /** Confirms the emailed code. This is what actually signs a new user in. */
    suspend fun verifyOtp(request: VerifyOtpRequest): AuthResponse {
        try {
            val auth: AuthResponse = ApiClient.client.post("/api/auth/verify-otp") { setBody(request) }.body()
            ApiClient.saveTokens(auth.accessToken, auth.refreshToken)
            ApiClient.saveUser(auth.userId, auth.email, auth.firstName, auth.lastName, auth.accountType)
            return auth
        } catch (e: ApiException) {
            println("Verify OTP failed [${e.statusCode}]: ${e.rawBody}")
            when (e.statusCode) {
                400 -> throw Exception("That code is invalid or has expired.")
                else -> throw Exception(e.message)
            }
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw Exception("Could not connect to the server. Please check your connection and try again.")
        }
    }

    /** Requests a fresh code for a pending registration. */
    suspend fun resendOtp(email: String): MessageResponse {
        try {
            return ApiClient.client.post("/api/auth/resend-otp") {
                setBody(ResendOtpRequest(email))
            }.body()
        } catch (e: ApiException) {
            throw Exception(e.message)
        } catch (e: Exception) {
            throw Exception("Could not connect to the server. Please check your connection and try again.")
        }
    }

    suspend fun refresh(): AuthResponse {
        val token = ApiClient.getRefreshToken() ?: throw Exception("Session expired. Please sign in again.")
        try {
            val auth: AuthResponse = ApiClient.client.post("/api/auth/refresh") { setBody(RefreshTokenRequest(token)) }.body()
            ApiClient.saveTokens(auth.accessToken, auth.refreshToken)
            return auth
        } catch (e: Exception) {
            println("Token refresh failed: ${e.message}")
            ApiClient.clearAll()
            throw Exception("Session expired. Please sign in again.")
        }
    }

    fun logout() { ApiClient.clearAll() }
}
