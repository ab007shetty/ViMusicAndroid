package com.abshetty.vimusic.core.innertube

import com.abshetty.vimusic.core.innertube.model.AdaptiveFormat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormatSelectorTest {
    private fun fmt(itag: Int, mime: String, bitrate: Long, url: String? = "https://x") =
        AdaptiveFormat(
            itag = itag, url = url, mimeType = mime, bitrate = bitrate,
            contentLength = "1000", audioQuality = null, signatureCipher = null,
        )

    @Test fun `prefers opus 251 over aac 140`() {
        val best = FormatSelector.bestAudio(listOf(
            fmt(140, "audio/mp4; codecs=\"mp4a.40.2\"", 128_000),
            fmt(251, "audio/webm; codecs=\"opus\"", 160_000),
        ))
        assertThat(best?.itag).isEqualTo(251)
    }

    @Test fun `falls back to aac 140 when opus is absent`() {
        val best = FormatSelector.bestAudio(listOf(
            fmt(140, "audio/mp4; codecs=\"mp4a.40.2\"", 128_000),
        ))
        assertThat(best?.itag).isEqualTo(140)
    }

    @Test fun `ignores video formats entirely`() {
        val best = FormatSelector.bestAudio(listOf(
            fmt(137, "video/mp4; codecs=\"avc1.640028\"", 3_000_000),
        ))
        assertThat(best).isNull()
    }

    @Test fun `skips formats with no plain url`() {
        val best = FormatSelector.bestAudio(listOf(
            fmt(251, "audio/webm; codecs=\"opus\"", 160_000, url = null),
            fmt(140, "audio/mp4; codecs=\"mp4a.40.2\"", 128_000),
        ))
        assertThat(best?.itag).isEqualTo(140)
    }

    @Test fun `among same-codec formats picks the highest bitrate`() {
        val best = FormatSelector.bestAudio(listOf(
            fmt(249, "audio/webm; codecs=\"opus\"", 50_000),
            fmt(250, "audio/webm; codecs=\"opus\"", 70_000),
            fmt(251, "audio/webm; codecs=\"opus\"", 160_000),
        ))
        assertThat(best?.itag).isEqualTo(251)
    }

    @Test fun `returns null for an empty list`() {
        assertThat(FormatSelector.bestAudio(emptyList())).isNull()
    }

    @Test fun `returns null when every format lacks a url`() {
        val best = FormatSelector.bestAudio(listOf(
            fmt(251, "audio/webm; codecs=\"opus\"", 160_000, url = null),
            fmt(140, "audio/mp4; codecs=\"mp4a.40.2\"", 128_000, url = ""),
        ))
        assertThat(best).isNull()
    }
}
