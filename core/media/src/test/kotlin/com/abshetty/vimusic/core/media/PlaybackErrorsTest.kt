package com.abshetty.vimusic.core.media

import androidx.media3.common.PlaybackException
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackErrorsTest {
    private val badStatus = PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
    private val offline = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
    private val timedOut = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT

    @Test fun `a missing video is skipped`() {
        assertThat(PlaybackErrors.shouldSkip(404, badStatus)).isTrue()
        assertThat(PlaybackErrors.shouldSkip(410, badStatus)).isTrue()
    }

    @Test fun `a refusal aimed at this device is not skipped`() {
        assertThat(PlaybackErrors.shouldSkip(403, badStatus)).isFalse()
        assertThat(PlaybackErrors.shouldSkip(401, badStatus)).isFalse()
        assertThat(PlaybackErrors.shouldSkip(429, badStatus)).isFalse()
    }

    @Test fun `being offline is not the track's fault`() {
        assertThat(PlaybackErrors.shouldSkip(null, offline)).isFalse()
        assertThat(PlaybackErrors.shouldSkip(null, timedOut)).isFalse()
    }

    @Test fun `a decode failure is about this file, so move on`() {
        assertThat(
            PlaybackErrors.shouldSkip(null, PlaybackException.ERROR_CODE_DECODING_FAILED)
        ).isTrue()
    }

    @Test fun `an unrecognised failure still moves the queue along`() {
        assertThat(
            PlaybackErrors.shouldSkip(null, PlaybackException.ERROR_CODE_UNSPECIFIED)
        ).isTrue()
    }

    @Test fun `the 403 message does not promise a skip that will not happen`() {
        val message = PlaybackErrors.message(403, badStatus, "Source error")
        assertThat(message).doesNotContain("Skipping")
        assertThat(message).contains("few minutes")
    }

    @Test fun `a missing track says it is skipping, because it is`() {
        assertThat(PlaybackErrors.message(404, badStatus, "Source error")).contains("Skipping")
    }

    @Test fun `an http status wins over the generic error code`() {
        assertThat(PlaybackErrors.message(403, badStatus, null))
            .isNotEqualTo(PlaybackErrors.message(404, badStatus, null))
    }

    @Test fun `a rejected client is named rather than reported as a source error`() {
        val message = PlaybackErrors.message(
            null,
            PlaybackException.ERROR_CODE_UNSPECIFIED,
            "YouTube is no longer supported in this application",
        )
        assertThat(message).contains("needs updating")
    }
}
