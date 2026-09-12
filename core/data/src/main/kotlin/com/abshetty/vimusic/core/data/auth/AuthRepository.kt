package com.abshetty.vimusic.core.data.auth

import android.content.Context
import com.abshetty.vimusic.core.model.UserId
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

data class Account(val email: String, val displayName: String?, val avatarUrl: String?)

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val googleSignIn: GoogleSignInLauncher,
    private val scope: CoroutineScope,
) {
    private val _userId = MutableStateFlow(UserId.GUEST)

    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account.asStateFlow()

    private val _resolved = MutableStateFlow(false)

    val isResolved: StateFlow<Boolean> = _resolved.asStateFlow()

    init {
        scope.launch {
            supabase.auth.sessionStatus.collect { status ->
                publish()

                if (status !is SessionStatus.Initializing) _resolved.value = true
            }
        }
    }

    private fun publish() {
        val user = supabase.auth.currentUserOrNull()
        val email = user?.email
        _userId.value = SessionIdentity.from(email)
        _account.value = email?.let {
            Account(
                email = it,
                displayName = user.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull(),
                avatarUrl = user.userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull(),
            )
        }
    }

    suspend fun signIn(activityContext: Context): Result<Unit> = runCatching {
        val idToken = googleSignIn.requestIdToken(activityContext).getOrThrow()
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken.token
            provider = Google
            nonce = idToken.rawNonce
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
        runCatching { content }.getOrNull()?.takeIf { it.isNotBlank() && it != "null" }
}
