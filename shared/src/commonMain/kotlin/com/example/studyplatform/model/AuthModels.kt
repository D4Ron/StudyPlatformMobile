package com.example.studyplatform.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val firstName: String, val lastName: String,
    val email: String, val password: String,
    val accountType: String = "STUDENT",
    val educationLevel: String = "UNIVERSITY",
    val registrationMode: String = "SOLO",
    val preferenceDomains: List<String>? = null,
    val objectives: String? = null,
    val groupInviteCode: String? = null
)

@Serializable
data class AuthResponse(
    val accessToken: String, val refreshToken: String,
    val userId: String, val email: String,
    val firstName: String, val lastName: String,
    val accountType: String
)

/**
 * Returned by /api/auth/register. Registration creates an unverified account and
 * carries no tokens — /api/auth/verify-otp is what establishes the session.
 */
@Serializable
data class RegistrationResponse(
    val email: String,
    val accountType: String = "STUDENT",
    val requiresVerification: Boolean = true,
    val message: String = ""
)

@Serializable
data class VerifyOtpRequest(
    val email: String,
    val code: String,
    val groupInviteCode: String? = null
)

@Serializable
data class ResendOtpRequest(val email: String)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class ApiError(
    val status: Int = 0, val error: String = "",
    val message: String = "", val fieldErrors: Map<String, String>? = null
)

/**
 * Posted after Google hands the client an ID token. Only the token travels: anything
 * else the client claimed about itself would be unverified, and the token already
 * carries the verified version of all of it.
 */
@Serializable
data class GoogleSignInRequest(val idToken: String, val groupInviteCode: String? = null)
