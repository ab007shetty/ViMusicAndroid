package com.abshetty.vimusic.core.innertube

import com.abshetty.vimusic.core.innertube.model.PlayerResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class InnertubeClient constructor(private val http: HttpClient) {
    private val visitorData = VisitorData(http)

    suspend fun player(videoId: String, context: ClientContext): PlayerResponse {
        val visitor = visitorData.get()
        return http.post(WATCH_BASE + "/player?prettyPrint=false") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, context.userAgent)
            visitor?.let { header("X-Goog-Visitor-Id", it) }
            setBody(buildJsonObject {
                put("context", clientBlock(context, visitor))
                put("videoId", videoId)
                put("contentCheckOk", true)
                put("racyCheckOk", true)
            })
        }.body()
    }

    suspend fun search(
        query: String,
        params: String?,
        continuation: String?,
        context: ClientContext,
    ): JsonObject = http.post(MUSIC_BASE + "/search?prettyPrint=false") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.UserAgent, context.userAgent)
        setBody(buildJsonObject {
            put("context", clientBlock(context))
            put("query", query)
            params?.let { put("params", it) }
            continuation?.let { put("continuation", it) }
        })
    }.body()

    suspend fun searchSuggestions(query: String, context: ClientContext): List<String> {
        val body: JsonObject = http.post(MUSIC_BASE + "/music/get_search_suggestions?prettyPrint=false") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, context.userAgent)
            setBody(buildJsonObject {
                put("context", clientBlock(context))
                put("input", query)
            })
        }.body()
        return SearchParser.suggestionTexts(body)
    }

    private fun clientBlock(ctx: ClientContext, visitor: String? = null): JsonObject = buildJsonObject {
        put("client", buildJsonObject {
            put("clientName", ctx.clientName)
            put("clientVersion", ctx.clientVersion)
            ctx.androidSdkVersion?.let { put("androidSdkVersion", it) }
            ctx.deviceMake?.let { put("deviceMake", it) }
            ctx.deviceModel?.let { put("deviceModel", it) }
            ctx.osName?.let { put("osName", it) }
            ctx.osVersion?.let { put("osVersion", it) }
            put("hl", ctx.hl)
            put("gl", ctx.gl)
            visitor?.let { put("visitorData", it) }
        })
    }

    companion object {
        const val WATCH_BASE = "https://www.youtube.com/youtubei/v1"
        const val MUSIC_BASE = "https://music.youtube.com/youtubei/v1"
    }
}
