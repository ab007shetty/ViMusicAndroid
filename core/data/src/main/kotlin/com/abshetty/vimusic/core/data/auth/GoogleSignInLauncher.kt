package com.abshetty.vimusic.core.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.abshetty.vimusic.core.data.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleIdToken(val token: String, val rawNonce: String)

@Singleton
class GoogleSignInLauncher @Inject constructor() {
    suspend fun requestIdToken(context: Context): Result<GoogleIdToken> {
        val rawNonce = ByteArray(32)
            .also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }

        val hashedNonce = MessageDigest.getInstance("SHA-256")
            .digest(rawNonce.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            return Result.failure(
                IllegalStateException("GOOGLE_WEB_CLIENT_ID is empty in local.properties")
            )
        }

        val options = listOf(
            GetSignInWithGoogleOption.Builder(clientId).setNonce(hashedNonce).build(),
            GetGoogleIdOption.Builder()
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(false)
                .setNonce(hashedNonce)
                .build(),
        )

        var last: Throwable? = null
        for (option in options) {
            val attempt = runCatching {
                val response = CredentialManager.create(context).getCredential(
                    context,
                    GetCredentialRequest.Builder().addCredentialOption(option).build(),
                )
                val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
                GoogleIdToken(token = credential.idToken, rawNonce = rawNonce)
            }

            attempt.onSuccess { return Result.success(it) }
            attempt.onFailure { error ->
                last = error

                Log.e(
                    "ViMusicAuth",
                    "sign-in via " + option::class.simpleName + " failed: " +
                        (error as? GetCredentialException)?.type + " " + error.message,
                    error,
                )
            }
        }

        return Result.failure(last ?: IllegalStateException("Sign-in failed with no error"))
    }
}
