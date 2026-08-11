package com.saariuslystoned.mbux.platform.storage

import com.saariuslystoned.mbux.domain.board.AttachFixtureMatch
import com.saariuslystoned.mbux.domain.board.NeedsYouPrompt
import com.saariuslystoned.mbux.domain.board.SessionBoardItem
import com.saariuslystoned.mbux.domain.board.SessionBoardRepository
import com.saariuslystoned.mbux.domain.board.SessionBoardSnapshot
import com.saariuslystoned.mbux.domain.board.SessionOrigin
import com.saariuslystoned.mbux.domain.board.SessionProvider
import com.saariuslystoned.mbux.domain.board.SessionStatus
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-private persistence for public fixture data only. It never stores audio or provider data. */
class PersistentSessionBoardRepository(
    private val preferences: BoardPreferences,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : SessionBoardRepository {
    private val lock = Any()
    private var selectedProvider = preferences.getString(KEY_PROVIDER)
        ?.let { value -> enumValues<SessionProvider>().firstOrNull { it.name == value } }
        ?: SessionProvider.CLAUDE
    private var sessions = preferences.getString(KEY_SESSIONS)?.let(::decodeSessions)
        ?: initialBoardFixtures().also(::persistSessions)
    private val mutableSnapshot = MutableStateFlow(snapshotValue())

    override val snapshot: StateFlow<SessionBoardSnapshot> = mutableSnapshot.asStateFlow()

    override fun selectProvider(provider: SessionProvider) = synchronized(lock) {
        if (provider == selectedProvider) return@synchronized
        selectedProvider = provider
        preferences.putString(KEY_PROVIDER, provider.name)
        publish()
    }

    override fun attachMatches(provider: SessionProvider): List<AttachFixtureMatch> = synchronized(lock) {
        val attachedIds = sessions.mapNotNullTo(mutableSetOf()) { it.fixtureMatchId }
        attachFixtureMatches()
            .asSequence()
            .filter { it.provider == provider && it.id !in attachedIds }
            .take(MAX_ATTACH_MATCHES)
            .toList()
    }

    override fun attach(matchId: String): Boolean = synchronized(lock) {
        if (sessions.any { it.fixtureMatchId == matchId }) return@synchronized false
        val match = attachFixtureMatches().firstOrNull { it.id == matchId } ?: return@synchronized false

        sessions = sessions + SessionBoardItem(
            id = idFactory(),
            provider = match.provider,
            repository = match.repository,
            taskTitle = match.taskTitle,
            status = match.status,
            artifactReady = match.artifactReady,
            origin = SessionOrigin.ATTACHED_FIXTURE,
            needsYouPrompt = match.needsYouPrompt,
            fixtureMatchId = match.id,
        )
        persistAndPublish()
        true
    }

    override fun archive(sessionId: String): Boolean = updateSession(sessionId) { session ->
        if (session.archived) session else session.copy(archived = true)
    }

    override fun restore(sessionId: String): Boolean = updateSession(sessionId) { session ->
        if (!session.archived) session else session.copy(archived = false)
    }

    override fun delete(sessionId: String): Boolean = synchronized(lock) {
        val remaining = sessions.filterNot { it.id == sessionId }
        if (remaining.size == sessions.size) return@synchronized false
        sessions = remaining
        persistAndPublish()
        true
    }

    override fun chooseLocalDecision(sessionId: String, choiceNumber: Int): Boolean =
        updateSession(sessionId) { session ->
            val choices = session.needsYouPrompt?.choices.orEmpty()
            if (session.status != SessionStatus.NEEDS_YOU || choiceNumber !in 1..choices.size) {
                session
            } else {
                session.copy(
                    status = SessionStatus.RUNNING,
                    needsYouPrompt = null,
                )
            }
        }

    private fun updateSession(
        sessionId: String,
        transform: (SessionBoardItem) -> SessionBoardItem,
    ): Boolean = synchronized(lock) {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index < 0) return@synchronized false
        val current = sessions[index]
        val updated = transform(current)
        if (updated == current) return@synchronized false
        sessions = sessions.toMutableList().also { it[index] = updated }
        persistAndPublish()
        true
    }

    private fun persistAndPublish() {
        persistSessions(sessions)
        publish()
    }

    private fun persistSessions(value: List<SessionBoardItem>) {
        preferences.putString(KEY_SESSIONS, encodeSessions(value))
    }

    private fun publish() {
        mutableSnapshot.value = snapshotValue()
    }

    private fun snapshotValue() = SessionBoardSnapshot(
        selectedProvider = selectedProvider,
        sessions = sessions,
    )

    private fun encodeSessions(value: List<SessionBoardItem>): String = buildString {
        append(FORMAT_VERSION)
        value.forEach { session ->
            append('\n')
            append(
                listOf(
                    encodeText(session.id),
                    session.provider.name,
                    encodeText(session.repository),
                    encodeText(session.taskTitle),
                    session.status.name,
                    session.artifactReady.toString(),
                    session.origin.name,
                    session.archived.toString(),
                    encodeText(session.needsYouPrompt?.message.orEmpty()),
                    encodeText(session.needsYouPrompt?.choices?.joinToString(CHOICE_SEPARATOR).orEmpty()),
                    encodeText(session.fixtureMatchId.orEmpty()),
                ).joinToString(FIELD_SEPARATOR),
            )
        }
    }

    private fun decodeSessions(serialized: String): List<SessionBoardItem> {
        val lines = serialized.lineSequence().toList()
        if (lines.firstOrNull() != FORMAT_VERSION) return emptyList()
        return lines.drop(1).mapNotNull(::decodeSession)
    }

    private fun decodeSession(record: String): SessionBoardItem? = runCatching {
        val fields = record.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT) return@runCatching null
        val status = SessionStatus.valueOf(fields[4])
        val promptMessage = decodeText(fields[8])
        val promptChoices = decodeText(fields[9])
            .takeIf(String::isNotEmpty)
            ?.split(CHOICE_SEPARATOR)
            .orEmpty()
        SessionBoardItem(
            id = decodeText(fields[0]),
            provider = SessionProvider.valueOf(fields[1]),
            repository = decodeText(fields[2]),
            taskTitle = decodeText(fields[3]),
            status = status,
            artifactReady = fields[5].toBooleanStrict(),
            origin = SessionOrigin.valueOf(fields[6]),
            archived = fields[7].toBooleanStrict(),
            needsYouPrompt = if (
                status == SessionStatus.NEEDS_YOU && promptMessage.isNotEmpty() && promptChoices.isNotEmpty()
            ) {
                NeedsYouPrompt(promptMessage, promptChoices)
            } else {
                null
            },
            fixtureMatchId = decodeText(fields[10]).ifEmpty { null },
        )
    }.getOrNull()

    private fun encodeText(value: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )

    private companion object {
        const val KEY_PROVIDER = "selected_provider"
        const val KEY_SESSIONS = "fixture_sessions"
        const val FORMAT_VERSION = "mbux-board-v1"
        const val FIELD_SEPARATOR = "|"
        const val CHOICE_SEPARATOR = "\u001F"
        const val FIELD_COUNT = 11
        const val MAX_ATTACH_MATCHES = 3
    }
}

private fun initialBoardFixtures(): List<SessionBoardItem> = listOf(
    SessionBoardItem(
        id = "fixture-claude-running",
        provider = SessionProvider.CLAUDE,
        repository = "mbux-app",
        taskTitle = "Build the phone session board",
        status = SessionStatus.RUNNING,
        artifactReady = false,
        origin = SessionOrigin.MBUX_LAUNCHED_FIXTURE,
    ),
    SessionBoardItem(
        id = "fixture-claude-needs-you",
        provider = SessionProvider.CLAUDE,
        repository = "tao-lab",
        taskTitle = "Review the migration plan",
        status = SessionStatus.NEEDS_YOU,
        artifactReady = true,
        origin = SessionOrigin.ATTACHED_FIXTURE,
        needsYouPrompt = NeedsYouPrompt(
            message = "Choose how this local fixture should continue.",
            choices = listOf("Use the minimal patch", "Split a follow-up", "Stop for review"),
        ),
        fixtureMatchId = "attached-claude-tao",
    ),
    SessionBoardItem(
        id = "fixture-codex-done",
        provider = SessionProvider.CODEX,
        repository = "pixel-fold",
        taskTitle = "Validate compact layout states",
        status = SessionStatus.DONE,
        artifactReady = true,
        origin = SessionOrigin.MBUX_LAUNCHED_FIXTURE,
    ),
    SessionBoardItem(
        id = "fixture-codex-failed",
        provider = SessionProvider.CODEX,
        repository = "stellarai",
        taskTitle = "Repair the fixture test",
        status = SessionStatus.FAILED,
        artifactReady = false,
        origin = SessionOrigin.ATTACHED_FIXTURE,
        fixtureMatchId = "attached-codex-stellar",
    ),
)

private fun attachFixtureMatches(): List<AttachFixtureMatch> = listOf(
    AttachFixtureMatch(
        id = "attached-claude-commerce",
        provider = SessionProvider.CLAUDE,
        repository = "aicommerce",
        taskTitle = "Review the checkout boundary",
        status = SessionStatus.RUNNING,
        artifactReady = false,
    ),
    AttachFixtureMatch(
        id = "attached-claude-stellar",
        provider = SessionProvider.CLAUDE,
        repository = "stellarai",
        taskTitle = "Choose a release proof",
        status = SessionStatus.NEEDS_YOU,
        artifactReady = true,
        needsYouPrompt = NeedsYouPrompt(
            message = "Select a local proof-path simulation.",
            choices = listOf("Unit proof", "UI proof", "Pause this fixture"),
        ),
    ),
    AttachFixtureMatch(
        id = "attached-claude-swarm",
        provider = SessionProvider.CLAUDE,
        repository = "swarmdash",
        taskTitle = "Inspect publisher health",
        status = SessionStatus.DONE,
        artifactReady = true,
    ),
    AttachFixtureMatch(
        id = "attached-codex-mbux",
        provider = SessionProvider.CODEX,
        repository = "mbux-app",
        taskTitle = "Audit board persistence",
        status = SessionStatus.RUNNING,
        artifactReady = false,
    ),
    AttachFixtureMatch(
        id = "attached-codex-tao",
        provider = SessionProvider.CODEX,
        repository = "tao-lab",
        taskTitle = "Confirm the bounded follow-up",
        status = SessionStatus.NEEDS_YOU,
        artifactReady = false,
        needsYouPrompt = NeedsYouPrompt(
            message = "Choose a local next-step simulation.",
            choices = listOf("Continue bounded", "Write a note", "Stop this fixture"),
        ),
    ),
    AttachFixtureMatch(
        id = "attached-codex-pixel",
        provider = SessionProvider.CODEX,
        repository = "pixel-fold",
        taskTitle = "Package the layout artifact",
        status = SessionStatus.DONE,
        artifactReady = true,
    ),
)
