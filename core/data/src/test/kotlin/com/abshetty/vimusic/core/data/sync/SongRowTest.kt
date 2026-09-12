package com.abshetty.vimusic.core.data.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test

class SongRowTest {
    private val base = buildJsonObject {
        put("songId", "abc")
        put("userId", "me@example.com")
        put("title", "A Song")
    }

    @Test
    fun `a payload without likedAt does not send the column`() {
        val row = songRow(base)

        assertThat(row.containsKey("likedAt")).isFalse()
        assertThat(row["id"].toString()).contains("abc")
    }

    @Test
    fun `an explicit null likedAt is sent through`() {
        val row = songRow(buildJsonObject {
            base.forEach { (k, v) -> put(k, v) }
            put("likedAt", JsonNull)
        })

        assertThat(row.containsKey("likedAt")).isTrue()
        assertThat(row["likedAt"]).isEqualTo(JsonNull)
    }

    @Test
    fun `a real likedAt is sent through`() {
        val row = songRow(buildJsonObject {
            base.forEach { (k, v) -> put(k, v) }
            put("likedAt", 1_700_000_000_000L)
        })

        assertThat(row["likedAt"].toString()).isEqualTo("1700000000000")
    }
}
