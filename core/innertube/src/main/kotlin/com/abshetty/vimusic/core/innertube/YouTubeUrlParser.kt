package com.abshetty.vimusic.core.innertube

import java.net.URI

enum class UrlSource { YOUTUBE, YOUTUBE_MUSIC }

data class ParsedUrl(val videoId: String, val source: UrlSource, val isShort: Boolean = false)

object YouTubeUrlParser {
    fun parse(input: String?): ParsedUrl? {
        val trimmed = input?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (!trimmed.contains("youtube.com") && !trimmed.contains("youtu.be")) return null

        val uri = runCatching {
            URI(if (trimmed.startsWith("http")) trimmed else "https://" + trimmed)
        }.getOrNull() ?: return null

        val host = uri.host?.removePrefix("www.") ?: return null
        val path = uri.path.orEmpty()

        return when {
            host == "music.youtube.com" && path == "/watch" ->
                queryParam(uri.query, "v")?.let { ParsedUrl(it, UrlSource.YOUTUBE_MUSIC) }

            host == "youtube.com" && path == "/watch" ->
                queryParam(uri.query, "v")?.let { ParsedUrl(it, UrlSource.YOUTUBE) }

            host == "youtube.com" && path.startsWith("/shorts/") ->
                path.removePrefix("/shorts/").substringBefore('?')
                    .takeIf { it.isNotEmpty() }
                    ?.let { ParsedUrl(it, UrlSource.YOUTUBE, isShort = true) }

            host == "youtu.be" ->
                path.removePrefix("/").substringBefore('?')
                    .takeIf { it.isNotEmpty() }
                    ?.let { ParsedUrl(it, UrlSource.YOUTUBE) }

            else -> null
        }
    }

    private fun queryParam(query: String?, key: String): String? =
        query?.split('&')
            ?.firstOrNull { it.startsWith(key + "=") }
            ?.substringAfter('=')
            ?.takeIf { it.isNotEmpty() }
}
