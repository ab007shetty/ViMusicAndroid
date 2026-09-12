package com.abshetty.vimusic.core.innertube

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class YouTubeUrlParserTest {
    @Test fun `parses a standard watch url`() {
        val r = YouTubeUrlParser.parse("https://www.youtube.com/watch?v=TUVcZfQe-Kw")!!
        assertThat(r.videoId).isEqualTo("TUVcZfQe-Kw")
        assertThat(r.source).isEqualTo(UrlSource.YOUTUBE)
        assertThat(r.isShort).isFalse()
    }

    @Test fun `parses a youtu dot be short link`() {
        assertThat(YouTubeUrlParser.parse("https://youtu.be/TUVcZfQe-Kw")!!.videoId)
            .isEqualTo("TUVcZfQe-Kw")
    }

    @Test fun `parses a youtu dot be link with query params`() {
        assertThat(YouTubeUrlParser.parse("https://youtu.be/TUVcZfQe-Kw?t=42")!!.videoId)
            .isEqualTo("TUVcZfQe-Kw")
    }

    @Test fun `parses a shorts url and flags it`() {
        val r = YouTubeUrlParser.parse("https://youtube.com/shorts/abc123XYZ_-")!!
        assertThat(r.videoId).isEqualTo("abc123XYZ_-")
        assertThat(r.isShort).isTrue()
    }

    @Test fun `parses a youtube music url and records the source`() {
        assertThat(YouTubeUrlParser.parse("https://music.youtube.com/watch?v=TUVcZfQe-Kw")!!.source)
            .isEqualTo(UrlSource.YOUTUBE_MUSIC)
    }

    @Test fun `tolerates a missing scheme`() {
        assertThat(YouTubeUrlParser.parse("www.youtube.com/watch?v=TUVcZfQe-Kw")!!.videoId)
            .isEqualTo("TUVcZfQe-Kw")
    }

    @Test fun `handles v not being the first query param`() {
        assertThat(YouTubeUrlParser.parse("https://www.youtube.com/watch?t=30&v=TUVcZfQe-Kw")!!.videoId)
            .isEqualTo("TUVcZfQe-Kw")
    }

    @Test fun `returns null for a plain search query`() {
        assertThat(YouTubeUrlParser.parse("m83 midnight city")).isNull()
    }

    @Test fun `returns null for a non-youtube url`() {
        assertThat(YouTubeUrlParser.parse("https://example.com/watch?v=abc")).isNull()
    }

    @Test fun `returns null for a youtube url with no video id`() {
        assertThat(YouTubeUrlParser.parse("https://www.youtube.com/feed/subscriptions")).isNull()
    }

    @Test fun `returns null for blank input`() {
        assertThat(YouTubeUrlParser.parse("   ")).isNull()
    }

    @Test fun `returns null for null input`() {
        assertThat(YouTubeUrlParser.parse(null)).isNull()
    }
}
