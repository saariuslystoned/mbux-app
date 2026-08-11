package com.saariuslystoned.mbux.domain.status

import kotlinx.coroutines.flow.StateFlow

/** Provider names allowed across the normalized CP-1 status boundary. */
enum class NormalizedSessionProvider(val displayName: String) {
    CLAUDE("Claude"),
    CODEX("Codex"),
}

/** A read-only lifecycle. UNKNOWN must remain distinct from a transport failure. */
enum class NormalizedSessionLifecycle(val displayName: String) {
    RUNNING("Running"),
    DONE("Done"),
    NEEDS_YOU("Needs you"),
    FAILED("Failed"),
    UNKNOWN("Unknown"),
}

/** Only sessions deliberately tracked by MBUX may cross the future relay. */
enum class NormalizedSessionOrigin(val displayName: String) {
    MBUX_LAUNCHED("MBUX launched"),
    EXPLICITLY_ATTACHED("Explicitly attached"),
}

/**
 * Minimal, user-safe session metadata accepted by the phone.
 *
 * Provider-native IDs, hostnames, paths, pane identifiers, prompts, and raw output do not belong
 * in this model. The future CP-1 wrapper is responsible for redacting them before delivery.
 */
data class NormalizedSessionSummary(
    val opaqueId: String,
    val provider: NormalizedSessionProvider,
    val repository: String,
    val taskTitle: String,
    val lifecycle: NormalizedSessionLifecycle,
    val artifactReady: Boolean,
    val origin: NormalizedSessionOrigin,
) {
    init {
        requireSafeSingleLine("opaqueId", opaqueId, MAX_OPAQUE_VALUE_LENGTH)
        requireSafeSingleLine("repository", repository, MAX_USER_LABEL_LENGTH)
        requireSafeSingleLine("taskTitle", taskTitle, MAX_USER_LABEL_LENGTH)
    }
}

/** One atomically verified, replaceable status snapshot. */
data class VerifiedSessionStatusSnapshot(
    val schemaVersion: Int,
    val opaqueVersion: String,
    val observedAtEpochMillis: Long,
    val sessions: List<NormalizedSessionSummary>,
) {
    init {
        require(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported normalized session status schema"
        }
        requireSafeSingleLine("opaqueVersion", opaqueVersion, MAX_OPAQUE_VALUE_LENGTH)
        require(observedAtEpochMillis >= 0L) { "observedAtEpochMillis must not be negative" }
        require(sessions.size <= MAX_SESSIONS_PER_SNAPSHOT) {
            "Normalized session snapshot exceeds its row limit"
        }
        require(sessions.map { it.opaqueId }.toSet().size == sessions.size) {
            "Normalized session IDs must be unique within a snapshot"
        }
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val MAX_SESSIONS_PER_SNAPSHOT = 100
    }
}

/**
 * Transport truth and data freshness are explicit. A disconnected state may retain only the last
 * atomically verified snapshot, which the UI must label as stale rather than current.
 */
sealed interface SessionStatusFeedState {
    data object Unknown : SessionStatusFeedState

    data object NotEnrolled : SessionStatusFeedState

    data class Live(val snapshot: VerifiedSessionStatusSnapshot) : SessionStatusFeedState

    data class Stale(val snapshot: VerifiedSessionStatusSnapshot) : SessionStatusFeedState

    data class Disconnected(
        val lastVerified: VerifiedSessionStatusSnapshot? = null,
    ) : SessionStatusFeedState
}

/** Read-only seam for a future enrolled CP-1 status capability. */
interface SessionStatusFeed {
    val state: StateFlow<SessionStatusFeedState>
}

private const val MAX_OPAQUE_VALUE_LENGTH = 128
private const val MAX_USER_LABEL_LENGTH = 160

private fun requireSafeSingleLine(field: String, value: String, maxLength: Int) {
    require(value.isNotBlank()) { "$field must not be blank" }
    require(value.length <= maxLength) { "$field exceeds its normalized length limit" }
    require('\n' !in value && '\r' !in value) { "$field must be a single line" }
}
