package com.saariuslystoned.mbux.domain.status

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionStatusFeedTest {
    @Test
    fun `verified snapshot accepts only the supported normalized shape`() {
        val snapshot = VerifiedSessionStatusSnapshot(
            schemaVersion = VerifiedSessionStatusSnapshot.SUPPORTED_SCHEMA_VERSION,
            opaqueVersion = "snapshot-7",
            observedAtEpochMillis = 1234L,
            sessions = listOf(session()),
        )

        assertEquals(NormalizedSessionLifecycle.UNKNOWN, snapshot.sessions.single().lifecycle)
    }

    @Test
    fun `unsupported schema fails atomically`() {
        assertThrows(IllegalArgumentException::class.java) {
            VerifiedSessionStatusSnapshot(
                schemaVersion = 2,
                opaqueVersion = "snapshot-7",
                observedAtEpochMillis = 1234L,
                sessions = listOf(session()),
            )
        }
    }

    @Test
    fun `duplicate opaque ids fail atomically`() {
        assertThrows(IllegalArgumentException::class.java) {
            VerifiedSessionStatusSnapshot(
                schemaVersion = VerifiedSessionStatusSnapshot.SUPPORTED_SCHEMA_VERSION,
                opaqueVersion = "snapshot-7",
                observedAtEpochMillis = 1234L,
                sessions = listOf(session(), session()),
            )
        }
    }

    @Test
    fun `multiline labels are rejected from normalized metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            session(taskTitle = "unsafe\nraw output")
        }
    }

    @Test
    fun `oversized snapshot fails atomically before UI rendering`() {
        assertThrows(IllegalArgumentException::class.java) {
            VerifiedSessionStatusSnapshot(
                schemaVersion = VerifiedSessionStatusSnapshot.SUPPORTED_SCHEMA_VERSION,
                opaqueVersion = "snapshot-7",
                observedAtEpochMillis = 1234L,
                sessions = List(VerifiedSessionStatusSnapshot.MAX_SESSIONS_PER_SNAPSHOT + 1) { index ->
                    session(id = "opaque-session-$index")
                },
            )
        }
    }

    private fun session(
        id: String = "opaque-session-1",
        taskTitle: String = "Waiting for a verified transition",
    ) = NormalizedSessionSummary(
        opaqueId = id,
        provider = NormalizedSessionProvider.CLAUDE,
        repository = "owner/repository",
        taskTitle = taskTitle,
        lifecycle = NormalizedSessionLifecycle.UNKNOWN,
        artifactReady = false,
        origin = NormalizedSessionOrigin.EXPLICITLY_ATTACHED,
    )
}
