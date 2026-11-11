package dev.arbjerg.lavalink.client

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkVoiceSessionTest {
    @Test
    fun `voice session without any voice update is never refreshed`() {
        assertFalse(Link.isVoiceSessionRefreshed(lastVoiceUpdateMillis = 0L, nowMillis = 1_000L))
    }

    @Test
    fun `voice session refreshed within the window counts as refreshed`() {
        assertTrue(Link.isVoiceSessionRefreshed(lastVoiceUpdateMillis = 1_000L, nowMillis = 11_000L))
    }

    @Test
    fun `voice session refreshed one millis before the window boundary counts as refreshed`() {
        val lastVoiceUpdateMillis = 1_000L
        assertTrue(
            Link.isVoiceSessionRefreshed(
                lastVoiceUpdateMillis = lastVoiceUpdateMillis,
                nowMillis = lastVoiceUpdateMillis + Link.VOICE_SESSION_REFRESH_WINDOW_MS - 1,
            )
        )
    }

    @Test
    fun `voice session refreshed at the window boundary counts as stale`() {
        val lastVoiceUpdateMillis = 1_000L
        assertFalse(
            Link.isVoiceSessionRefreshed(
                lastVoiceUpdateMillis = lastVoiceUpdateMillis,
                nowMillis = lastVoiceUpdateMillis + Link.VOICE_SESSION_REFRESH_WINDOW_MS,
            )
        )
    }
}
