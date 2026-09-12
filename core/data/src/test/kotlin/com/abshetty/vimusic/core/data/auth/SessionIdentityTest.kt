package com.abshetty.vimusic.core.data.auth

import com.abshetty.vimusic.core.model.UserId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionIdentityTest {
    @Test fun `a signed-in session yields the normalized email`() {
        assertThat(SessionIdentity.from("Person@Gmail.com")).isEqualTo("person@gmail.com")
    }

    @Test fun `no session yields the guest bucket`() {
        assertThat(SessionIdentity.from(null)).isEqualTo(UserId.GUEST)
    }

    @Test fun `identity matches what the web backend would compute`() {
        val webEquivalent = "  Person@Gmail.com ".trim().lowercase()
        assertThat(SessionIdentity.from("  Person@Gmail.com ")).isEqualTo(webEquivalent)
    }

    @Test fun `guest cannot be mistaken for a real account`() {
        assertThat(SessionIdentity.isAuthenticated(SessionIdentity.from(null))).isFalse()
        assertThat(SessionIdentity.isAuthenticated(SessionIdentity.from("a@b.com"))).isTrue()
    }
}
