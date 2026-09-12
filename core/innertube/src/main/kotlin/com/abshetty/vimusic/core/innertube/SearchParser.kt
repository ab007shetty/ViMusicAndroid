package com.abshetty.vimusic.core.innertube

import com.abshetty.vimusic.core.innertube.model.SearchItem
import com.abshetty.vimusic.core.innertube.model.SearchResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

object SearchParser {
    private val DURATION = Regex("""^\d{1,2}:\d{2}(:\d{2})?$""")

    fun parse(root: JsonObject): SearchResult {
        val renderers = mutableListOf<JsonObject>()
        collect(root, "musicResponsiveListItemRenderer", renderers)

        val seen = mutableSetOf<String>()
        val items = renderers.mapNotNull(::toItem).filter { seen.add(it.videoId) }

        return SearchResult(items = items, continuation = findContinuation(root))
    }

    fun suggestionTexts(root: JsonObject): List<String> {
        val out = mutableListOf<JsonObject>()
        collect(root, "searchSuggestionRenderer", out)
        return out.mapNotNull { r -> allRunTexts(r).joinToString("").takeIf { it.isNotBlank() } }
            .distinct()
    }

    private fun collect(element: JsonElement, key: String, into: MutableList<JsonObject>) {
        when (element) {
            is JsonObject -> element.forEach { (k, v) ->
                if (k == key && v is JsonObject) into += v else collect(v, key, into)
            }
            is JsonArray -> element.forEach { collect(it, key, into) }
            else -> Unit
        }
    }

    private fun toItem(renderer: JsonObject): SearchItem? {
        val videoId = firstString(renderer, "videoId") ?: return null
        val texts = allRunTexts(renderer)
        val title = texts.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null

        return SearchItem(
            videoId = videoId,
            title = title,

            artistsText = texts.getOrNull(1)?.trim(),
            durationText = texts.lastOrNull { DURATION.matches(it) },
            thumbnailUrl = largestThumbnail(renderer),
        )
    }

    private fun firstString(element: JsonElement, key: String): String? = when (element) {
        is JsonObject ->
            (element[key] as? JsonPrimitive)?.contentOrNull()
                ?: element.values.firstNotNullOfOrNull { firstString(it, key) }
        is JsonArray -> element.firstNotNullOfOrNull { firstString(it, key) }
        else -> null
    }

    private fun allRunTexts(element: JsonElement): List<String> {
        val out = mutableListOf<String>()
        fun walk(e: JsonElement) {
            when (e) {
                is JsonObject -> {
                    (e["runs"] as? JsonArray)?.forEach { run ->
                        (run.jsonObject["text"] as? JsonPrimitive)?.contentOrNull()?.let(out::add)
                    }
                    e.values.forEach(::walk)
                }
                is JsonArray -> e.forEach(::walk)
                else -> Unit
            }
        }
        walk(element)
        return out.map { it.trim() }.filter { it.isNotBlank() && it != "•" }
    }

    private fun largestThumbnail(renderer: JsonObject): String? {
        var bestWidth = -1
        var bestUrl: String? = null
        fun walk(e: JsonElement) {
            when (e) {
                is JsonObject -> {
                    val url = (e["url"] as? JsonPrimitive)?.contentOrNull()
                    val width = (e["width"] as? JsonPrimitive)?.contentOrNull()?.toIntOrNull()
                    if (url != null && width != null && width > bestWidth) {
                        bestWidth = width
                        bestUrl = url
                    }
                    e.values.forEach(::walk)
                }
                is JsonArray -> e.forEach(::walk)
                else -> Unit
            }
        }
        walk(renderer)
        return bestUrl
    }

    private fun findContinuation(root: JsonObject): String? =
        firstString(root, "continuation") ?: firstString(root, "token")

    private fun JsonPrimitive.contentOrNull(): String? =
        runCatching { content }.getOrNull()?.takeIf { it != "null" }
}
