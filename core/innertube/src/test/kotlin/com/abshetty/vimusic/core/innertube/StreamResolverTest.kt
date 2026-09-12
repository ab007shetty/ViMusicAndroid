package com.abshetty.vimusic.core.innertube

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class StreamResolverTest {
    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader!!.getResourceAsStream("fixtures/" + name))
            .bufferedReader().readText()

    private fun clientReturning(body: String): HttpClient {
        val engine = MockEngine {
            respond(
                body, HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        }
    }

    private fun resolver(body: String, now: Long = 1_000_000L) =
        InnertubeStreamResolver(InnertubeClient(clientReturning(body)), nowMs = { now })

    @Test fun `resolves a real player response to a playable audio stream`() = runTest {
        val stream = resolver(fixture("player_android_vr.json")).resolve("dQw4w9WgXcQ").getOrThrow()

        assertThat(stream.url).startsWith("http")
        assertThat(stream.itag).isIn(listOf(140, 251))
        assertThat(stream.mimeType).startsWith("audio/")
        assertThat(stream.bitrate).isGreaterThan(0L)
    }

    @Test fun `prefers opus from the real response`() = runTest {
        val stream = resolver(fixture("player_android_vr.json")).resolve("dQw4w9WgXcQ").getOrThrow()
        assertThat(stream.itag).isEqualTo(251)
    }

    @Test fun `sets an expiry ahead of now so the cache can honour it`() = runTest {
        val stream = resolver(fixture("player_android_vr.json")).resolve("dQw4w9WgXcQ").getOrThrow()
        assertThat(stream.expiresAtMs).isGreaterThan(1_000_000L)
    }

    @Test fun `expiry honours the response TTL minus the safety margin`() = runTest {
        val stream = resolver(fixture("player_android_vr.json")).resolve("dQw4w9WgXcQ").getOrThrow()
        assertThat(stream.expiresAtMs).isEqualTo(1_000_000L + (21_540L - 300L) * 1000L)
    }

    @Test fun `carries the user agent of the client that minted the url`() = runTest {
        val stream = resolver(fixture("player_android_vr.json")).resolve("dQw4w9WgXcQ").getOrThrow()

        assertThat(stream.userAgent).isEqualTo(ClientContext.FALLBACK_ORDER.first().userAgent)
    }

    @Test fun `uses the fallback client's agent when the first client is rejected`() = runTest {
        var players = 0
        val engine = MockEngine { request ->

            val body = if (!request.url.encodedPath.endsWith("/player")) {
                """{"responseContext":{"visitorData":"test-visitor"}}"""
            } else if (++players == 1) {
                """{"playabilityStatus":{"status":"LOGIN_REQUIRED","reason":"Sign in"}}"""
            } else {
                fixture("player_android_vr.json")
            }
            respond(
                body, HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val http = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        }

        val stream = InnertubeStreamResolver(InnertubeClient(http)).resolve("x").getOrThrow()

        assertThat(stream.userAgent).isEqualTo(ClientContext.FALLBACK_ORDER[1].userAgent)
        assertThat(stream.userAgent).isNotEqualTo(ClientContext.FALLBACK_ORDER[0].userAgent)
    }

    @Test fun `reads loudnessDb so volume normalization has data`() = runTest {
        val stream = resolver(fixture("player_android_vr.json")).resolve("dQw4w9WgXcQ").getOrThrow()
        assertThat(stream.loudnessDb).isNotNull()
        assertThat(stream.loudnessDb!!.isFinite()).isTrue()
    }

    @Test fun `reads contentLength when present`() = runTest {
        val stream = resolver(fixture("player_android_vr.json")).resolve("dQw4w9WgXcQ").getOrThrow()
        assertThat(stream.contentLength).isNotNull()
        assertThat(stream.contentLength!!).isGreaterThan(0L)
    }

    @Test fun `returns a failure when no audio format is present`() = runTest {
        val empty = """{"streamingData":{"adaptiveFormats":[]},"playabilityStatus":{"status":"OK"}}"""
        val result = resolver(empty).resolve("x")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NoPlayableFormatException::class.java)
    }

    @Test fun `an unplayable verdict still tries the remaining clients`() = runTest {
        val blocked = """{"playabilityStatus":{"status":"UNPLAYABLE","reason":"Video unavailable"}}"""
        val result = resolver(blocked).resolve("x")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(UnplayableVideoException::class.java)
        assertThat(result.exceptionOrNull()!!.message).contains("Video unavailable")
    }

    @Test fun `a client rejection is reported, not swallowed as success`() = runTest {
        val rejected =
            """{"playabilityStatus":{"status":"ERROR","reason":"YouTube is no longer supported in this application or device."}}"""
        val result = resolver(rejected).resolve("x")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()!!.message).contains("no longer supported")
    }

    @Test fun `LOGIN_REQUIRED falls through the client list`() = runTest {
        val refused = """{"playabilityStatus":{"status":"LOGIN_REQUIRED"}}"""
        val result = resolver(refused).resolve("x")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()!!.message).contains("LOGIN_REQUIRED")
    }
}
