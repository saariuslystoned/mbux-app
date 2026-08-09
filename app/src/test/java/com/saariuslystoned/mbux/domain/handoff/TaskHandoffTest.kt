package com.saariuslystoned.mbux.domain.handoff

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskHandoffTest {
    @Test
    fun `shared brief contains reviewed target repository task and disclosure`() {
        val text = TaskHandoffBuilder.buildPlainText(
            TaskHandoffDraft(
                target = HandoffTarget.CODEX_CHATGPT,
                repository = "  mbux-app  ",
                taskBrief = "  Run the focused tests.  ",
            ),
        )

        assertTrue(text.contains("Intended destination: Codex / ChatGPT"))
        assertTrue(text.contains("Repository: mbux-app"))
        assertTrue(text.contains("Task: Run the focused tests."))
        assertTrue(text.contains("has not created or sent a task"))
        assertFalse(text.contains("  mbux-app  "))
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
            TaskHandoffDraft(
                target = HandoffTarget.CLAUDE,
                repository = "owner/repo & tools",
                taskBrief = "Fix flaky test? #1 — café",
            ),
            HandoffRoute.CLAUDE_CODE_DRAFT,
        ) as TaskHandoffEndpoint.ClaudeCodeDraft

        assertEquals(
            "claude://code/new?q=Fix%20flaky%20test%3F%20%231%20%E2%80%94%20caf%C3%A9&repo=owner%2Frepo%20%26%20tools",
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

    private fun readyDraft(target: HandoffTarget) = TaskHandoffDraft(
        target = target,
        repository = "mbux-app",
        taskBrief = "Run the focused tests",
    )
}
