package com.abshetty.vimusic.core.innertube

import com.abshetty.vimusic.core.innertube.model.AdaptiveFormat

data class AudioStream(
    val url: String,
    val itag: Int,
    val mimeType: String,
    val bitrate: Long,
    val contentLength: Long?,
    val loudnessDb: Float?,
    val expiresAtMs: Long,

    val userAgent: String,
)

class UnplayableVideoException(reason: String) : Exception(reason)
class NoPlayableFormatException(videoId: String) :
    Exception("No playable audio format for " + videoId)

data class VideoStream(
    val url: String,
    val itag: Int,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val expiresAtMs: Long,

    val userAgent: String,
) {
    val isPortrait: Boolean get() = height > width
}

interface StreamResolver {
    suspend fun resolve(videoId: String): Result<AudioStream>

    suspend fun resolveVideo(videoId: String, maxHeight: Int = 720): Result<VideoStream>
}

object FormatSelector {
    fun bestVideo(formats: List<AdaptiveFormat>, maxHeight: Int): AdaptiveFormat? =
        formats
            .filter { it.mimeType.startsWith("video/") && !it.url.isNullOrBlank() }
            .filter { (it.height ?: 0) <= maxHeight }
            .maxByOrNull { (it.height ?: 0) * 100_000L + it.bitrate }

    private const val ITAG_OPUS = 251
    private const val ITAG_AAC = 140

    fun bestAudio(formats: List<AdaptiveFormat>): AdaptiveFormat? {
        val playable = formats.filter {
            it.mimeType.startsWith("audio/") && !it.url.isNullOrBlank()
        }
        if (playable.isEmpty()) return null

        val opus = playable.filter { it.mimeType.contains("opus") }
        val pool = if (opus.isEmpty()) playable else opus

        return pool.maxWithOrNull(
            compareBy<AdaptiveFormat> { if (it.itag == ITAG_OPUS || it.itag == ITAG_AAC) 1 else 0 }
                .thenBy { it.bitrate }
        )
    }
}

class InnertubeStreamResolver constructor(
    private val client: InnertubeClient,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : StreamResolver {
    override suspend fun resolve(videoId: String): Result<AudioStream> {
        var lastFailure: Throwable? = null

        for (context in ClientContext.FALLBACK_ORDER) {
            val attempt = runCatching { client.player(videoId, context) }
            val response = attempt.getOrElse { lastFailure = it; continue }

            val status = response.playabilityStatus?.status
            if (status != null && status != "OK") {
                lastFailure = UnplayableVideoException(
                    response.playabilityStatus.reason ?: status
                )
                continue
            }

            val format = FormatSelector.bestAudio(
                response.streamingData?.adaptiveFormats.orEmpty()
            )
            val url = format?.url
            if (url == null) {
                lastFailure = NoPlayableFormatException(videoId)
                continue
            }

            val ttlSeconds = response.streamingData?.expiresInSeconds?.toLongOrNull() ?: DEFAULT_TTL_S
            val expiresAtMs = nowMs() + (ttlSeconds - SAFETY_MARGIN_S).coerceAtLeast(60) * 1000

            return Result.success(
                AudioStream(
                    url = url,
                    itag = format.itag,
                    mimeType = format.mimeType,
                    bitrate = format.bitrate,
                    contentLength = format.contentLength?.toLongOrNull(),
                    loudnessDb = response.playerConfig?.audioConfig?.loudnessDb,
                    expiresAtMs = expiresAtMs,
                    userAgent = context.userAgent,
                )
            )
        }

        return Result.failure(lastFailure ?: NoPlayableFormatException(videoId))
    }

    override suspend fun resolveVideo(videoId: String, maxHeight: Int): Result<VideoStream> {
        for (context in ClientContext.FALLBACK_ORDER) {
            val response = runCatching { client.player(videoId, context) }.getOrNull() ?: continue
            if (response.playabilityStatus?.status?.let { it != "OK" } == true) continue

            val format = FormatSelector.bestVideo(
                response.streamingData?.adaptiveFormats.orEmpty(), maxHeight
            ) ?: continue
            val url = format.url ?: continue

            val ttlSeconds =
                response.streamingData?.expiresInSeconds?.toLongOrNull() ?: DEFAULT_TTL_S
            return Result.success(
                VideoStream(
                    url = url,
                    itag = format.itag,
                    mimeType = format.mimeType,
                    width = format.width ?: 16,
                    height = format.height ?: 9,
                    expiresAtMs = nowMs() +
                        (ttlSeconds - SAFETY_MARGIN_S).coerceAtLeast(60) * 1000,
                    userAgent = context.userAgent,
                )
            )
        }
        return Result.failure(NoPlayableFormatException(videoId))
    }

    private companion object {
        const val DEFAULT_TTL_S = 21_540L
        const val SAFETY_MARGIN_S = 300L
    }
}
