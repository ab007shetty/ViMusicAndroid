package com.abshetty.vimusic.core.data.local

import android.util.Log
import com.abshetty.vimusic.core.data.auth.AuthRepository
import com.abshetty.vimusic.core.database.dao.OutboxDao
import com.abshetty.vimusic.core.database.dao.SongDao
import com.abshetty.vimusic.core.model.LocalId
import com.abshetty.vimusic.core.model.UserId
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalTrackPurge @Inject constructor(
    private val songs: SongDao,
    private val outbox: OutboxDao,
    private val supabase: SupabaseClient,
    private val auth: AuthRepository,
    private val scope: CoroutineScope,
) {
    fun run() {
        scope.launch {
            val queued = runCatching { outbox.deleteLocalTrackEntries() }.getOrDefault(0)
            val rows = runCatching { songs.deleteLocalTracks() }.getOrDefault(0)

            if (queued > 0 || rows > 0) {
                Log.i(TAG, "purged " + rows + " local rows and " + queued + " queued pushes")
            }

            val userId = auth.userId.value
            if (UserId.isGuest(userId)) return@launch
            runCatching {
                supabase.from("song").delete {
                    filter {
                        eq("user_id", userId)
                        like("id", LocalId.PREFIX + "%")
                    }
                }
            }.onFailure { Log.w(TAG, "server purge failed: " + it.message) }
        }
    }

    private companion object { const val TAG = "ViMusicLocal" }
}
