package com.abshetty.vimusic.core.data.local

import com.abshetty.vimusic.core.model.CacheState
import com.abshetty.vimusic.core.model.LocalId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalScanTest {
    private val URI = "content://com.android.externalstorage/tree/x/document/y%2Fa.mp3"

    @Test
    fun `placeholder titles the song after its file`() {
        val song = placeholderSong(URI, "Sanam Re.mp3", 1_700_000_000_000)

        assertEquals("Sanam Re", song.title)
    }

    @Test
    fun `placeholder keeps a dot inside the name and drops only the extension`() {
        val song = placeholderSong(URI, "Mr. Brightside.flac", null)

        assertEquals("Mr. Brightside", song.title)
    }

    @Test
    fun `placeholder carries the fields the list is built from`() {
        val song = placeholderSong(URI, "a.mp3", 1_700_000_000_000)

        assertEquals(LocalId.of(URI), song.id)
        assertEquals(LocalId.USER, song.userId)

        assertEquals(1_700_000_000_000, song.likedAt)

        assertEquals(CacheState.CACHED, song.cacheState)
        assertEquals(URI, song.thumbnailUrl)
    }

    @Test
    fun `placeholder leaves what only the tags can say empty`() {
        val song = placeholderSong(URI, "a.mp3", null)

        assertNull(song.artistsText)
        assertNull(song.durationText)
    }

    @Test
    fun `tags replace the filename title`() {
        val enriched = placeholderSong(URI, "track01.mp3", 1L)
            .withTags(title = "Sanam Re", artist = "Arijit Singh", durationMs = 272_000)

        assertEquals("Sanam Re", enriched.title)
        assertEquals("Arijit Singh", enriched.artistsText)
        assertEquals("4:32", enriched.durationText)
    }

    @Test
    fun `a file with no title tag keeps its filename`() {
        val enriched = placeholderSong(URI, "track01.mp3", 1L)
            .withTags(title = null, artist = null, durationMs = null)

        assertEquals("track01", enriched.title)
    }

    @Test
    fun `a blank title tag does not wipe the filename`() {
        val enriched = placeholderSong(URI, "track01.mp3", 1L)
            .withTags(title = "   ", artist = "", durationMs = null)

        assertEquals("track01", enriched.title)

        assertNull(enriched.artistsText)
    }

    @Test
    fun `tags never move a row`() {
        val placeholder = placeholderSong(URI, "track01.mp3", 1_700_000_000_000)
        val enriched = placeholder.withTags("Sanam Re", "Arijit Singh", 272_000)

        assertEquals(placeholder.id, enriched.id)
        assertEquals(placeholder.likedAt, enriched.likedAt)
        assertEquals(placeholder.thumbnailUrl, enriched.thumbnailUrl)
        assertEquals(placeholder.cacheState, enriched.cacheState)
    }

    @Test
    fun `a tagless file is returned unchanged rather than copied`() {
        val placeholder = placeholderSong(URI, "track01.mp3", 1L)

        assertSame(placeholder, placeholder.withTags(null, null, null))
    }

    @Test
    fun `durations pad their seconds`() {
        val song = placeholderSong(URI, "a.mp3", 1L)

        assertEquals("3:05", song.withTags(null, null, 185_000).durationText)
        assertEquals("0:07", song.withTags(null, null, 7_000).durationText)
        assertEquals("61:00", song.withTags(null, null, 3_660_000).durationText)
    }

    @Test
    fun `octet-stream files are judged by their extension`() {
        assertTrue(isAudioFile("application/octet-stream", "song.flac"))
        assertTrue(isAudioFile("application/octet-stream", "SONG.MP3"))
    }

    @Test
    fun `non-audio is left alone whatever it is called`() {
        assertTrue(!isAudioFile("image/jpeg", "cover.jpg"))
        assertTrue(!isAudioFile("application/octet-stream", "notes.txt"))
        assertTrue(!isAudioFile("application/pdf", "booklet.pdf"))
    }
}
