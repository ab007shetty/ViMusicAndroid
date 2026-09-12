package com.abshetty.vimusic.core.innertube

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class VisitorData(private val http: HttpClient) {
    private val mutex = Mutex()
    private var cached: String? = null

    suspend fun get(): String? {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: fetch().also { cached = it }
        }
    }

    private suspend fun fetch(): String? = runCatching {
        val body: JsonObject = http.post(
            InnertubeClient.WATCH_BASE + "/visitor_id?prettyPrint=false"
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, WEB_UA)
            setBody(buildJsonObject {
                put("context", buildJsonObject {
                    put("client", buildJsonObject {
                        put("clientName", "WEB")
                        put("clientVersion", WEB_VERSION)
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
            })
        }.body()

        body["responseContext"]?.jsonObject
            ?.get("visitorData")?.jsonPrimitive?.content
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private companion object {
        const val WEB_VERSION = "2.20250310.01.00"
        const val WEB_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}
