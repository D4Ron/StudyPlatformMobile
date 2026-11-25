package com.example.studyplatform.api

import com.example.studyplatform.i18n.Msg

import com.example.studyplatform.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Sign-in succeeded on credentials but the address has not been confirmed yet.
 * The backend re-sends a code when this happens, so callers should send the user
 * to the verification screen.
 */
class EmailNotVerifiedException(val email: String) :
    Exception(Msg.t("Please confirm your email. We've sent you a new code.", "Confirmez votre adresse e-mail. Nous vous avons envoyé un nouveau code."))

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
                    400 -> Msg.t("Google sign-in isn't available on this server yet.", "La connexion avec Google n'est pas encore disponible sur ce serveur.")
                    401 -> Msg.t("That Google sign-in could not be verified. Please try again.", "Cette connexion Google n'a pas pu être vérifiée. Réessayez.")
                    else -> e.message
                }
            )
        } catch (e: Exception) {
            println("Google sign-in network error: ${e.message}")
            throw Exception(Msg.t("Could not reach the server. Please check your connection.", "Impossible de joindre le serveur. Vérifiez votre connexion."))
        }
    }

    /**
     * Enters the app as the shared demo account, with no credentials.
     *
     * Only exists where the server has demo mode on; anywhere else this 404s. The
     * screens ask `providers()` before drawing the button, but that is presentation —
     * the gate is the server's, and a client that asked anyway would simply be refused.
     */
    suspend fun demo(): AuthResponse {
        try {
            val auth: AuthResponse = ApiClient.client.post("/api/auth/demo").body()
            ApiClient.saveTokens(auth.accessToken, auth.refreshToken)
            ApiClient.saveUser(auth.userId, auth.email, auth.firstName, auth.lastName, auth.accountType)
            return auth
        } catch (e: ApiException) {
            throw Exception(
                if (e.statusCode == 404) "Demo mode is not enabled on this server."
                else e.message ?: Msg.t("Could not start the demo.", "Impossible de lancer la démo.")
            )
        } catch (e: Exception) {
            throw Exception(Msg.t("Could not reach the server. Please check your connection.", "Impossible de joindre le serveur. Vérifiez votre connexion."))
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
                401 -> throw Exception(Msg.t("Invalid email or password.", "E-mail ou mot de passe incorrect."))
                // The credentials were right but the address is unconfirmed. The
                // backend has just re-sent a code, so the caller should route to
                // verification rather than showing a dead end.
                403 -> throw EmailNotVerifiedException(request.email)
                else -> throw Exception(e.message)
            }
        } catch (e: Exception) {
            println("Login network error: ${e.message}")
            throw Exception(Msg.t("Could not connect to the server. Please check your connection and try again.", "Impossible de se connecter au serveur. Vérifiez votre connexion et réessayez."))
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
                409 -> throw Exception(Msg.t("An account with this email already exists.", "Un compte existe déjà avec cette adresse."))
                400 -> throw Exception(Msg.t("Please check your information and try again.", "Vérifiez vos informations et réessayez."))
                else -> throw Exception(e.message)
            }
        } catch (e: Exception) {
            if (e is ApiException) throw e
            println("Register network error: ${e.message}")
            throw Exception(Msg.t("Could not connect to the server. Please check your connection and try again.", "Impossible de se connecter au serveur. Vérifiez votre connexion et réessayez."))
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
                400 -> throw Exception(Msg.t("That code is invalid or has expired.", "Ce code est invalide ou a expiré."))
                else -> throw Exception(e.message)
            }
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw Exception(Msg.t("Could not connect to the server. Please check your connection and try again.", "Impossible de se connecter au serveur. Vérifiez votre connexion et réessayez."))
        }
    }

    /**
     * Asks for a password reset code. The server answers the same whether or not the
     * address has an account, so this only fails when the server cannot be reached.
     */
    suspend fun requestPasswordReset(email: String) {
        try {
            ApiClient.client.post("/api/auth/reset-password") { setBody(PasswordResetRequest(email)) }
        } catch (e: ApiException) {
            throw Exception(e.message)
        } catch (e: Exception) {
            throw Exception(Msg.t("Could not connect to the server. Please check your connection and try again.", "Impossible de se connecter au serveur. Vérifiez votre connexion et réessayez."))
        }
    }

    /** Sets a new password with the emailed code, and signs the user in. */
    suspend fun confirmPasswordReset(email: String, code: String, newPassword: String): AuthResponse {
        try {
            val auth: AuthResponse = ApiClient.client.post("/api/auth/reset-password/confirm") {
                setBody(PasswordResetConfirmRequest(email, code, newPassword))
            }.body()
            ApiClient.saveTokens(auth.accessToken, auth.refreshToken)
            ApiClient.saveUser(auth.userId, auth.email, auth.firstName, auth.lastName, auth.accountType)
            return auth
        } catch (e: ApiException) {
            when (e.statusCode) {
                400 -> throw Exception(Msg.t("That code is invalid or has expired, or the password is under 8 characters.", "Ce code est invalide ou a expiré, ou le mot de passe fait moins de 8 caractères."))
                else -> throw Exception(e.message)
            }
        } catch (e: Exception) {
            throw Exception(Msg.t("Could not connect to the server. Please check your connection and try again.", "Impossible de se connecter au serveur. Vérifiez votre connexion et réessayez."))
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
            throw Exception(Msg.t("Could not connect to the server. Please check your connection and try again.", "Impossible de se connecter au serveur. Vérifiez votre connexion et réessayez."))
        }
    }

    suspend fun refresh(): AuthResponse {
        val token = ApiClient.getRefreshToken() ?: throw Exception(Msg.t("Session expired. Please sign in again.", "Session expirée. Veuillez vous reconnecter."))
        try {
            val auth: AuthResponse = ApiClient.client.post("/api/auth/refresh") { setBody(RefreshTokenRequest(token)) }.body()
            ApiClient.saveTokens(auth.accessToken, auth.refreshToken)
            return auth
        } catch (e: Exception) {
            println("Token refresh failed: ${e.message}")
            ApiClient.clearAll()
            throw Exception(Msg.t("Session expired. Please sign in again.", "Session expirée. Veuillez vous reconnecter."))
        }
    }

    fun logout() { ApiClient.clearAll() }
}
