package com.abshetty.vimusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abshetty.vimusic.core.designsystem.chroma.ChromaState
import com.abshetty.vimusic.core.designsystem.vimusic.ViMusicTheme
import com.abshetty.vimusic.core.media.MusicServiceConnection
import com.abshetty.vimusic.navigation.ViMusicApp
import com.abshetty.vimusic.setup.SetupScreen
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var connection: MusicServiceConnection
    @Inject lateinit var librarySync: com.abshetty.vimusic.core.data.sync.LibrarySyncCoordinator

    @Inject lateinit var libraryRealtime: com.abshetty.vimusic.core.data.sync.LibraryRealtime

    @Inject lateinit var settings: com.abshetty.vimusic.core.datastore.SettingsStore

    @Inject lateinit var localTrackPurge:
        com.abshetty.vimusic.core.data.local.LocalTrackPurge

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        connection.connect()
        localTrackPurge.run()

        setContent {
            val playback by connection.state.collectAsStateWithLifecycle()
            val seed = ChromaState.rememberSeedFor(playback.artworkUri)
            val amoled by settings.amoled.collectAsStateWithLifecycle(initialValue = false)

            val pinColours by settings.pinColours
                .collectAsStateWithLifecycle(initialValue = false)

            val setupDone by settings.setupComplete.collectAsStateWithLifecycle(

                initialValue = true,
            )
            val scope = rememberCoroutineScope()

            ViMusicTheme(seed = seed, amoled = amoled, pinColours = pinColours) {
                if (setupDone) {
                    ViMusicApp(sharedLink = intent.extractSharedLink())
                } else {
                    SetupScreen(
                        onFinish = { scope.launch { settings.setSetupComplete(true) } },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        librarySync.syncNow()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        connection.release()
        super.onDestroy()
    }

    private fun Intent.extractSharedLink(): String? = when (action) {
        Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
        Intent.ACTION_VIEW -> dataString
        else -> null
    }
}
