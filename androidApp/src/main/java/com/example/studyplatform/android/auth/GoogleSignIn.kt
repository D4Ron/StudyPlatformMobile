package com.example.studyplatform.android.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import tg.edunova.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Google Sign-In through Credential Manager.
 *
 * This is the supported route: `GoogleSignInClient` is deprecated, and Credential
 * Manager is what replaces it. It shows the account picker, and hands back an ID token
 * that the backend — not this class — verifies. Nothing here decides who anybody is;
 * that would be trusting the device, and the device is the thing being authenticated.
 */
object GoogleSignIn {

    /** The person closed the sheet. Not an error, and must not be shown as one. */
    class Cancelled : Exception("Sign-in cancelled")

    /**
     * False when this build has no client id, so the caller can leave the button out.
     *
     * The server is asked separately: both sides have to be configured, and either one
     * missing produces the same useless button.
     */
    val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /**
     * Shows the picker and returns Google's ID token.
     *
     * @param filterByAuthorizedAccounts true offers only accounts that have used this
     *        app before — a one-tap return for an existing user. Start with true and
     *        retry with false, which is the documented pattern: filtered gives the
     *        smoother return, unfiltered is what a first-time user needs, and asking
     *        unfiltered first makes returning users pick from every account on the phone.
     */
    suspend fun requestIdToken(
        context: Context,
        filterByAuthorizedAccounts: Boolean = true
    ): String {
        check(isConfigured) { "No Google web client id in this build." }

        val option = GetGoogleIdOption.Builder()
            // The WEB client id, not the Android one. Passing the Android id here is the
            // usual cause of "ApiException: 10", which says nothing about the mistake.
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(filterByAuthorizedAccounts)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = try {
            CredentialManager.create(context).getCredential(context, request)
        } catch (e: NoCredentialException) {
            // Nobody on this phone has used the app with Google yet. Ask again showing
            // every account rather than telling the user there is nothing available.
            if (filterByAuthorizedAccounts) {
                return requestIdToken(context, filterByAuthorizedAccounts = false)
            }
            throw Exception("No Google account is available on this device.")
        } catch (e: GetCredentialCancellationException) {
            throw Cancelled()
        } catch (e: GetCredentialException) {
            println("Credential Manager failed: ${e.message}")
            throw Exception("Google sign-in didn't complete. Please try again.")
        }

        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }

        throw Exception("Google returned an unexpected credential type.")
    }
}
