package com.abshetty.vimusic.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HtmlTextTest {
    @Test fun `decodes named entities`() {
        assertThat(HtmlText.decode("Jay-Z &amp; Kanye West"))
            .isEqualTo("Jay-Z & Kanye West")
    }

    @Test fun `decodes numeric and hex references`() {
        assertThat(HtmlText.decode("Don&#39;t Stop")).isEqualTo("Don't Stop")
        assertThat(HtmlText.decode("Caf&#xe9;")).isEqualTo("Café")
    }

    @Test fun `leaves an unknown entity alone rather than dropping it`() {
        assertThat(HtmlText.decode("a &notreal; b")).isEqualTo("a &notreal; b")
    }

    @Test fun `leaves bare angle brackets and ampersands untouched`() {
        assertThat(HtmlText.decode("<3 you & me")).isEqualTo("<3 you & me")
        assertThat(HtmlText.decode("a > b")).isEqualTo("a > b")
    }

    @Test fun `returns the same string when there is nothing to decode`() {
        assertThat(HtmlText.decode("Plain title")).isEqualTo("Plain title")
    }
}
