package com.abshetty.vimusic.core.media.embedded

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IFrameHostHolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scope: CoroutineScope,
) {
    @Volatile private var host: YouTubeIFrameHost? = null

    fun get(): YouTubeIFrameHost = host ?: synchronized(this) {
        host ?: YouTubeIFrameHost(context).also { host = it; relay(it) }
    }

    fun peek(): YouTubeIFrameHost? = host

    fun releaseHost() = synchronized(this) {
        host?.release()
        host = null
        statusRelay.value = null
    }

    private val statusRelay = MutableStateFlow<EmbeddedStatus?>(null)

    val statusOrNull: StateFlow<EmbeddedStatus?> = statusRelay.asStateFlow()

    private fun relay(host: YouTubeIFrameHost) {
        scope.launch { host.status.collect { statusRelay.value = it } }
    }
}
