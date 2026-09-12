package com.abshetty.vimusic.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UserIdTest {
    @Test fun `lowercases the email`() {
        assertThat(UserId.of("Person@Gmail.COM")).isEqualTo("person@gmail.com")
    }

    @Test fun `trims surrounding whitespace`() {
        assertThat(UserId.of("  person@gmail.com \n")).isEqualTo("person@gmail.com")
    }

    @Test fun `null becomes the guest bucket`() {
        assertThat(UserId.of(null)).isEqualTo(UserId.GUEST)
    }

    @Test fun `blank becomes the guest bucket`() {
        assertThat(UserId.of("   ")).isEqualTo(UserId.GUEST)
    }

    @Test fun `web sentinel values become the guest bucket`() {
        assertThat(UserId.of("null")).isEqualTo(UserId.GUEST)
        assertThat(UserId.of("undefined")).isEqualTo(UserId.GUEST)
        assertThat(UserId.of("guest")).isEqualTo(UserId.GUEST)
    }

    @Test fun `guest bucket is the empty string`() {
        assertThat(UserId.GUEST).isEmpty()
    }

    @Test fun `isGuest reflects the bucket`() {
        assertThat(UserId.isGuest(UserId.of(null))).isTrue()
        assertThat(UserId.isGuest(UserId.of("person@gmail.com"))).isFalse()
    }
}
