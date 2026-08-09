package com.saariuslystoned.mbux.domain.board

import kotlinx.coroutines.flow.StateFlow

enum class SessionProvider(val displayName: String) {
    CLAUDE("Claude"),
    CODEX("Codex"),
}

enum class SessionStatus(val displayName: String) {
    RUNNING("Running"),
    DONE("Done"),
    NEEDS_YOU("Needs you"),
    FAILED("Failed"),
}

enum class SessionOrigin(val displayName: String) {
    MBUX_LAUNCHED_FIXTURE("MBUX-launched fixture"),
    ATTACHED_FIXTURE("Attached fixture"),
}

data class NeedsYouPrompt(
    val message: String,
    val choices: List<String>,
)

data class SessionBoardItem(
    val id: String,
    val provider: SessionProvider,
    val repository: String,
    val taskTitle: String,
    val status: SessionStatus,
    val artifactReady: Boolean,
    val origin: SessionOrigin,
    val archived: Boolean = false,
    val needsYouPrompt: NeedsYouPrompt? = null,
    val fixtureMatchId: String? = null,
)

data class AttachFixtureMatch(
    val id: String,
    val provider: SessionProvider,
    val repository: String,
    val taskTitle: String,
    val status: SessionStatus,
    val artifactReady: Boolean,
    val needsYouPrompt: NeedsYouPrompt? = null,
)

data class SessionBoardSnapshot(
    val selectedProvider: SessionProvider,
    val sessions: List<SessionBoardItem>,
)

interface SessionBoardRepository {
    val snapshot: StateFlow<SessionBoardSnapshot>

    fun selectProvider(provider: SessionProvider)

    fun attachMatches(provider: SessionProvider): List<AttachFixtureMatch>

    fun attach(matchId: String): Boolean

    fun archive(sessionId: String): Boolean

    fun restore(sessionId: String): Boolean

    fun delete(sessionId: String): Boolean

    /** Applies only to a local fixture and never sends a choice to a provider. */
    fun chooseLocalDecision(sessionId: String, choiceNumber: Int): Boolean
}
