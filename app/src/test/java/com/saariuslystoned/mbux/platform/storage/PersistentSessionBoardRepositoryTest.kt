package com.saariuslystoned.mbux.platform.storage

import com.saariuslystoned.mbux.domain.board.SessionOrigin
import com.saariuslystoned.mbux.domain.board.SessionProvider
import com.saariuslystoned.mbux.domain.board.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentSessionBoardRepositoryTest {
    @Test
    fun defaultsContainOnlyMbuxLaunchedOrExplicitlyAttachedFixtures() {
        val repository = PersistentSessionBoardRepository(InMemoryBoardPreferences())

        assertEquals(SessionProvider.CLAUDE, repository.snapshot.value.selectedProvider)
        assertTrue(repository.snapshot.value.sessions.isNotEmpty())
        assertTrue(
            repository.snapshot.value.sessions.all {
                it.origin == SessionOrigin.MBUX_LAUNCHED_FIXTURE ||
                    it.origin == SessionOrigin.ATTACHED_FIXTURE
            },
        )
    }

    @Test
    fun selectedProviderPersistsAcrossRepositoryRecreation() {
        val preferences = InMemoryBoardPreferences()
        val firstRepository = PersistentSessionBoardRepository(preferences)

        firstRepository.selectProvider(SessionProvider.CODEX)
        val reopenedRepository = PersistentSessionBoardRepository(preferences)

        assertEquals(SessionProvider.CODEX, reopenedRepository.snapshot.value.selectedProvider)
    }

    @Test
    fun archiveRestoreAndDeletePersistLocally() {
        val preferences = InMemoryBoardPreferences()
        val repository = PersistentSessionBoardRepository(preferences)
        val sessionId = repository.snapshot.value.sessions.first().id

        assertTrue(repository.archive(sessionId))
        assertTrue(repository.snapshot.value.sessions.single { it.id == sessionId }.archived)
        assertTrue(repository.restore(sessionId))
        assertFalse(repository.snapshot.value.sessions.single { it.id == sessionId }.archived)
        assertTrue(repository.delete(sessionId))

        val reopenedRepository = PersistentSessionBoardRepository(preferences)
        assertFalse(reopenedRepository.snapshot.value.sessions.any { it.id == sessionId })
    }

    @Test
    fun attachRequiresAnExplicitKnownFixtureAndOffersAtMostThreeMatches() {
        val preferences = InMemoryBoardPreferences()
        val repository = PersistentSessionBoardRepository(preferences) { "new-local-fixture" }
        val before = repository.snapshot.value.sessions.size
        val matches = repository.attachMatches(SessionProvider.CLAUDE)

        assertTrue(matches.size <= 3)
        assertFalse(repository.attach("not-a-fixture"))
        assertEquals(before, repository.snapshot.value.sessions.size)

        val match = matches.first()
        assertTrue(repository.attach(match.id))
        assertFalse(repository.attach(match.id))
        assertEquals(before + 1, repository.snapshot.value.sessions.size)
        assertEquals(
            SessionOrigin.ATTACHED_FIXTURE,
            repository.snapshot.value.sessions.single { it.id == "new-local-fixture" }.origin,
        )
    }

    @Test
    fun numberedDecisionUpdatesOnlyTheLocalFixtureAndPersists() {
        val preferences = InMemoryBoardPreferences()
        val repository = PersistentSessionBoardRepository(preferences)
        val needsYou = repository.snapshot.value.sessions.first { it.status == SessionStatus.NEEDS_YOU }

        assertFalse(repository.chooseLocalDecision(needsYou.id, 0))
        assertTrue(repository.chooseLocalDecision(needsYou.id, 2))

        val updated = repository.snapshot.value.sessions.single { it.id == needsYou.id }
        assertEquals(SessionStatus.RUNNING, updated.status)
        assertEquals(null, updated.needsYouPrompt)

        val reopened = PersistentSessionBoardRepository(preferences)
            .snapshot.value.sessions.single { it.id == needsYou.id }
        assertEquals(SessionStatus.RUNNING, reopened.status)
        assertEquals(null, reopened.needsYouPrompt)
    }

    private class InMemoryBoardPreferences : BoardPreferences {
        private val values = mutableMapOf<String, String>()

        override fun getString(key: String): String? = values[key]

        override fun putString(key: String, value: String) {
            values[key] = value
        }
    }
}
