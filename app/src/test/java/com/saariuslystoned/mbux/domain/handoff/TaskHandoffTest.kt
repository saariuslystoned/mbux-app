package com.saariuslystoned.mbux.domain.handoff

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskHandoffTest {
    @Test
    fun `shared brief contains reviewed target repository task and disclosure`() {
        val text = TaskHandoffBuilder.buildPlainText(
            TaskHandoffDraft.publicProof(
                target = HandoffTarget.CODEX_CHATGPT,
                taskBrief = "  Run the focused tests.  ",
            ),
        )

        assertTrue(text.contains("Intended destination: Codex / ChatGPT"))
        assertTrue(text.contains("Repository: saariuslystoned/mbux-app"))
        assertTrue(text.contains("Task: Run the focused tests."))
        assertTrue(text.contains("has not created or sent a task"))
    }

    @Test
    fun `public proof draft is fixed to the single approved repository`() {
        val draft = TaskHandoffDraft.publicProof(
            target = HandoffTarget.CLAUDE,
            taskBrief = "Review the proof",
        )

        assertEquals("saariuslystoned/mbux-app", TaskHandoffDraft.PUBLIC_PROOF_REPOSITORY)
        assertEquals(TaskHandoffDraft.PUBLIC_PROOF_REPOSITORY, draft.repository)
        assertTrue(draft.isReady)
    }

    @Test
    fun `oversized task brief fails closed before intent construction`() {
        val draft = TaskHandoffDraft.publicProof(
            target = HandoffTarget.CODEX_CHATGPT,
            taskBrief = "x".repeat(TaskHandoffDraft.MAX_TASK_BRIEF_LENGTH + 1),
        )

        assertFalse(draft.isReady)
        assertThrows(IllegalArgumentException::class.java) {
            TaskHandoffBuilder.buildPlainText(draft)
        }
    }

    @Test
    fun `both targets select the generic chooser endpoint`() {
        HandoffTarget.entries.forEach { target ->
            val endpoint = TaskHandoffBuilder.endpointFor(readyDraft(target), HandoffRoute.ANDROID_CHOOSER)
            assertTrue(endpoint is TaskHandoffEndpoint.AndroidChooser)
        }
    }

    @Test
    fun `claude draft endpoint encodes task and repository as query components`() {
        val endpoint = TaskHandoffBuilder.endpointFor(
            TaskHandoffDraft.publicProof(
                target = HandoffTarget.CLAUDE,
                taskBrief = "Fix flaky test? #1 — café",
            ),
            HandoffRoute.CLAUDE_CODE_DRAFT,
        ) as TaskHandoffEndpoint.ClaudeCodeDraft

        assertEquals(
            "claude://code/new?q=Fix%20flaky%20test%3F%20%231%20%E2%80%94%20caf%C3%A9&repo=saariuslystoned%2Fmbux-app",
            endpoint.uri,
        )
    }

    @Test
    fun `claude route is not selected for codex or chatgpt`() {
        val endpoint = TaskHandoffBuilder.endpointFor(
            readyDraft(HandoffTarget.CODEX_CHATGPT),
            HandoffRoute.CLAUDE_CODE_DRAFT,
        )

        assertNull(endpoint)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank repository is rejected before endpoint construction`() {
        TaskHandoffBuilder.endpointFor(
            TaskHandoffDraft(
                target = HandoffTarget.CLAUDE,
                repository = "  ",
                taskBrief = "Do the work",
            ),
            HandoffRoute.ANDROID_CHOOSER,
        )
    }

    private fun readyDraft(target: HandoffTarget) = TaskHandoffDraft.publicProof(
        target = target,
        taskBrief = "Run the focused tests",
    )
}
