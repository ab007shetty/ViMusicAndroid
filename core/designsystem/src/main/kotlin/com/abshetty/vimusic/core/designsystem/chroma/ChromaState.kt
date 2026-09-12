package com.abshetty.vimusic.core.designsystem.chroma

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ChromaState {
    @Composable
    fun rememberSeedFor(artworkUrl: String?): Color? {
        val context = LocalContext.current
        var seed by remember(artworkUrl) { mutableStateOf<Color?>(null) }

        LaunchedEffect(artworkUrl) {
            if (artworkUrl.isNullOrBlank()) {
                seed = null
                return@LaunchedEffect
            }
            seed = withContext(Dispatchers.Default) {
                runCatching {
                    val result = SingletonImageLoader.get(context).execute(
                        ImageRequest.Builder(context)
                            .data(artworkUrl)
                            .allowHardware(false)
                            .build()
                    )
                    result.image?.toBitmap()?.let(ChromaSeed::fromBitmap)
                }.getOrNull()
            }
        }
        return seed
    }
}
