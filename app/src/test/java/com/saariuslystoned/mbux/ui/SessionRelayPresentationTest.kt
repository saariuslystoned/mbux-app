package com.saariuslystoned.mbux.ui

import com.saariuslystoned.mbux.domain.board.SessionProvider
import com.saariuslystoned.mbux.domain.status.NormalizedSessionLifecycle
import com.saariuslystoned.mbux.domain.status.NormalizedSessionOrigin
import com.saariuslystoned.mbux.domain.status.NormalizedSessionProvider
import com.saariuslystoned.mbux.domain.status.NormalizedSessionSummary
import com.saariuslystoned.mbux.domain.status.SessionStatusFeedState
import com.saariuslystoned.mbux.domain.status.VerifiedSessionStatusSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRelayPresentationTest {
    @Test
    fun `not enrolled shows no remote rows`() {
        val presentation = sessionRelayPresentation(
            SessionStatusFeedState.NotEnrolled,
            SessionProvider.CLAUDE,
        )

        assertEquals("Not enrolled", presentation.statusLabel)
        assertTrue(presentation.sessions.isEmpty())
        assertFalse(presentation.isCurrent)
    }

    @Test
    fun `live snapshot is filtered to selected provider`() {
        val presentation = sessionRelayPresentation(
            SessionStatusFeedState.Live(snapshot()),
            SessionProvider.CODEX,
        )

        assertEquals("Live", presentation.statusLabel)
        assertEquals(listOf("codex-opaque"), presentation.sessions.map { it.opaqueId })
        assertTrue(presentation.isCurrent)
    }

    @Test
    fun `stale and disconnected rows are never presented as current`() {
        val stale = sessionRelayPresentation(
            SessionStatusFeedState.Stale(snapshot()),
            SessionProvider.CLAUDE,
        )
        val disconnected = sessionRelayPresentation(
            SessionStatusFeedState.Disconnected(lastVerified = snapshot()),
            SessionProvider.CLAUDE,
        )

        assertEquals("Stale", stale.statusLabel)
        assertFalse(stale.isCurrent)
        assertEquals("Disconnected", disconnected.statusLabel)
        assertFalse(disconnected.isCurrent)
        assertTrue(disconnected.detail.contains("stale"))
    }

    @Test
    fun `source unknown remains distinct from disconnected transport`() {
        val unknownSession = snapshot().sessions.first()
        val unknownFeed = sessionRelayPresentation(
            SessionStatusFeedState.Unknown,
            SessionProvider.CLAUDE,
        )
        val live = sessionRelayPresentation(
            SessionStatusFeedState.Live(snapshot()),
            SessionProvider.CLAUDE,
        )

        assertEquals("Unknown", unknownFeed.statusLabel)
        assertEquals(NormalizedSessionLifecycle.UNKNOWN, unknownSession.lifecycle)
        assertEquals(NormalizedSessionLifecycle.UNKNOWN, live.sessions.single().lifecycle)
    }

    private fun snapshot() = VerifiedSessionStatusSnapshot(
        schemaVersion = VerifiedSessionStatusSnapshot.SUPPORTED_SCHEMA_VERSION,
        opaqueVersion = "version-1",
        observedAtEpochMillis = 1000L,
        sessions = listOf(
            session(
                id = "claude-opaque",
                provider = NormalizedSessionProvider.CLAUDE,
                lifecycle = NormalizedSessionLifecycle.UNKNOWN,
            ),
            session(
                id = "codex-opaque",
                provider = NormalizedSessionProvider.CODEX,
                lifecycle = NormalizedSessionLifecycle.RUNNING,
            ),
        ),
    )

    private fun session(
        id: String,
        provider: NormalizedSessionProvider,
        lifecycle: NormalizedSessionLifecycle,
    ) = NormalizedSessionSummary(
        opaqueId = id,
        provider = provider,
        repository = "owner/repository",
        taskTitle = "Safe short title",
        lifecycle = lifecycle,
        artifactReady = false,
        origin = NormalizedSessionOrigin.MBUX_LAUNCHED,
    )
}
