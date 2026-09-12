package com.abshetty.vimusic.core.innertube

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test

class SearchParserTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun fixture(): JsonObject = json.decodeFromString(
        checkNotNull(javaClass.classLoader!!.getResourceAsStream("fixtures/search_songs.json"))
            .bufferedReader().readText()
    )

    @Test fun `extracts song rows from a real search response`() {
        val result = SearchParser.parse(fixture())

        assertThat(result.items).isNotEmpty()
        result.items.forEach {
            assertThat(it.videoId).isNotEmpty()
            assertThat(it.title).isNotEmpty()
        }
    }

    @Test fun `every parsed item carries a thumbnail`() {
        assertThat(SearchParser.parse(fixture()).items.all { !it.thumbnailUrl.isNullOrBlank() })
            .isTrue()
    }

    @Test fun `video ids are unique`() {
        val ids = SearchParser.parse(fixture()).items.map { it.videoId }
        assertThat(ids).containsNoDuplicates()
    }

    @Test fun `durations that are parsed look like timecodes`() {
        val durations = SearchParser.parse(fixture()).items.mapNotNull { it.durationText }
        assertThat(durations).isNotEmpty()
        durations.forEach { assertThat(it).matches("""\d{1,2}:\d{2}(:\d{2})?""") }
    }

    @Test fun `the expected track is present in the results`() {
        val titles = SearchParser.parse(fixture()).items.map { it.title.lowercase() }
        assertThat(titles.any { it.contains("midnight city") }).isTrue()
    }

    @Test fun `returns an empty result rather than throwing on an unknown shape`() {
        val result = SearchParser.parse(json.decodeFromString("""{"contents":{}}"""))
        assertThat(result.items).isEmpty()
        assertThat(result.continuation).isNull()
    }

    @Test fun `returns an empty result for an empty object`() {
        assertThat(SearchParser.parse(json.decodeFromString("{}")).items).isEmpty()
    }
}
