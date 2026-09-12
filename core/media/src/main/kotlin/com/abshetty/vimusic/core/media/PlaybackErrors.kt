package com.abshetty.vimusic.core.media

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource

object PlaybackErrors {
    fun httpCodeOf(error: PlaybackException): Int? =
        generateSequence(error.cause) { it.cause }
            .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
            .firstOrNull()
            ?.responseCode

    fun message(error: PlaybackException): String =
        message(httpCodeOf(error), error.errorCode, error.message)

    internal fun message(httpCode: Int?, errorCode: Int, rawMessage: String?): String {
        return when {
            httpCode == 403 || httpCode == 401 ->
                "YouTube is refusing streams to this device for the moment. " +
                    "It usually clears in a few minutes. Downloaded songs still play."

            httpCode == 404 || httpCode == 410 ->
                "This track is no longer available on YouTube. Skipping."

            httpCode == 429 ->
                "YouTube is rate-limiting this device. Wait a minute and try again."

            errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "No connection. Downloaded and previously played songs still work offline."

            errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "YouTube refused this stream."

            rawMessage?.contains("no longer supported", ignoreCase = true) == true ->
                "YouTube rejected every client this app knows. The app needs updating."

            rawMessage?.contains("not a bot", ignoreCase = true) == true ->
                "YouTube is asking this device to verify it is not a bot. Try again shortly."

            errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ->
                "This device could not decode the audio. Skipping."

            else -> "Could not play this track. Skipping."
        }
    }

    fun shouldSkip(error: PlaybackException): Boolean =
        shouldSkip(httpCodeOf(error), error.errorCode)

    internal fun shouldSkip(httpCode: Int?, errorCode: Int): Boolean {
        if (httpCode in THROTTLED_CODES) return false

        return errorCode != PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED &&
            errorCode != PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
    }

    private val THROTTLED_CODES = setOf(401, 403, 429)
}
