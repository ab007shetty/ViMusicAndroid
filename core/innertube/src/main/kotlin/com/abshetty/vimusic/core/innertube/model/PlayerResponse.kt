package com.abshetty.vimusic.core.innertube.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus? = null,
    val streamingData: StreamingData? = null,
    val videoDetails: VideoDetails? = null,
    val playerConfig: PlayerConfig? = null,
)

@Serializable
data class PlayabilityStatus(val status: String? = null, val reason: String? = null)

@Serializable
data class StreamingData(
    val expiresInSeconds: String? = null,
    val adaptiveFormats: List<AdaptiveFormat> = emptyList(),
    val formats: List<AdaptiveFormat> = emptyList(),
)

@Serializable
data class AdaptiveFormat(
    val itag: Int,
    val url: String? = null,
    val mimeType: String,
    val bitrate: Long = 0,
    val contentLength: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val audioQuality: String? = null,
    val signatureCipher: String? = null,
)

@Serializable
data class VideoDetails(
    val videoId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val channelId: String? = null,
    val lengthSeconds: String? = null,
)

@Serializable
data class PlayerConfig(val audioConfig: AudioConfig? = null)

@Serializable
data class AudioConfig(val loudnessDb: Float? = null)
