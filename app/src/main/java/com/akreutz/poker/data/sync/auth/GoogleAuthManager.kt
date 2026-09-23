package com.akreutz.poker.data.sync.auth

import android.accounts.Account
import android.content.Context
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.akreutz.poker.BuildConfig
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Drive scope limited to this app's own hidden folder - never the user's general Drive files. */
private const val DRIVE_APPDATA_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"

sealed interface SignInResult {
    data class SignedIn(val account: Account) : SignInResult
    data object NoCredential : SignInResult
    data class Error(val message: String?) : SignInResult
}

/**
 * Handles Google sign-in (via Credential Manager) and mints OAuth access tokens scoped to the
 * app's Drive appDataFolder. Must be driven from an Activity context since Credential Manager
 * needs one to show the account picker, and since granting the Drive scope for the first time
 * requires launching a system consent screen for a result (see [consentLauncher]).
 */
class GoogleAuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    var signedInAccount: Account? = null
        private set

    /**
     * Set by MainActivity to a function that launches an Intent for a result and completes the
     * given [CompletableDeferred] with whether the user granted consent. Without this set,
     * a first-time Drive authorization request will fail rather than prompt the user.
     */
    var consentLauncher: ((Intent, CompletableDeferred<Boolean>) -> Unit)? = null

    /** Silently tries previously-authorized accounts first, then falls back to the account picker. */
    suspend fun signIn(): SignInResult {
        val filtered = trySignIn(filterByAuthorizedAccounts = true)
        if (filtered !is SignInResult.NoCredential) return filtered
        return trySignIn(filterByAuthorizedAccounts = false)
    }

    private suspend fun trySignIn(filterByAuthorizedAccounts: Boolean): SignInResult {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(filterByAuthorizedAccounts)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return SignInResult.Error("Unexpected credential type")
            }
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val account = Account(googleIdTokenCredential.id, "com.google")
            signedInAccount = account
            SignInResult.SignedIn(account)
        } catch (e: GetCredentialException) {
            if (filterByAuthorizedAccounts) SignInResult.NoCredential else SignInResult.Error(e.message)
        }
    }

    /**
     * Mints a fresh OAuth access token for the Drive appdata scope. If the user hasn't granted
     * that scope yet, this launches the system consent screen (via [consentLauncher]) and
     * retries once after the user responds.
     */
    suspend fun getDriveAccessToken(): String? {
        val account = signedInAccount ?: return null
        return try {
            fetchToken(account)
        } catch (e: UserRecoverableAuthException) {
            val granted = requestConsent(e.intent ?: return null)
            if (!granted) return null
            fetchToken(account)
        }
    }

    private suspend fun fetchToken(account: Account): String? = withContext(Dispatchers.IO) {
        GoogleAuthUtil.getToken(context, account, DRIVE_APPDATA_SCOPE)
    }

    private suspend fun requestConsent(intent: Intent): Boolean {
        val launcher = consentLauncher ?: return false
        val result = CompletableDeferred<Boolean>()
        launcher(intent, result)
        return result.await()
    }
}
