package com.saariuslystoned.mbux.domain.handoff

import java.nio.charset.StandardCharsets

enum class HandoffTarget(val displayName: String) {
    CLAUDE("Claude"),
    CODEX_CHATGPT("Codex / ChatGPT"),
}

data class TaskHandoffDraft(
    val target: HandoffTarget,
    val repository: String,
    val taskBrief: String,
) {
    val isReady: Boolean
        get() = repository.isNotBlank() &&
            taskBrief.isNotBlank() &&
            taskBrief.trim().length <= MAX_TASK_BRIEF_LENGTH

    companion object {
        const val PUBLIC_PROOF_REPOSITORY = "saariuslystoned/mbux-app"
        const val MAX_TASK_BRIEF_LENGTH = 4_000

        fun publicProof(
            target: HandoffTarget,
            taskBrief: String,
        ) = TaskHandoffDraft(
            target = target,
            repository = PUBLIC_PROOF_REPOSITORY,
            taskBrief = taskBrief,
        )
    }
}

enum class HandoffRoute {
    ANDROID_CHOOSER,
    CLAUDE_CODE_DRAFT,
}

sealed interface TaskHandoffEndpoint {
    data class AndroidChooser(val plainText: String) : TaskHandoffEndpoint

    data class ClaudeCodeDraft(val uri: String) : TaskHandoffEndpoint
}

object TaskHandoffBuilder {
    fun endpointFor(
        draft: TaskHandoffDraft,
        route: HandoffRoute,
    ): TaskHandoffEndpoint? = when (route) {
        HandoffRoute.ANDROID_CHOOSER -> TaskHandoffEndpoint.AndroidChooser(
            plainText = buildPlainText(draft),
        )

        HandoffRoute.CLAUDE_CODE_DRAFT -> buildClaudeCodeEndpoint(draft)
    }

    fun buildPlainText(draft: TaskHandoffDraft): String {
        val repository = draft.requireRepository()
        val taskBrief = draft.requireTaskBrief()
        return """
            MBUX task handoff
            Intended destination: ${draft.target.displayName}
            Repository: $repository
            Task: $taskBrief

            Review this brief in the destination app. MBUX has not created or sent a task.
        """.trimIndent()
    }

    private fun buildClaudeCodeEndpoint(draft: TaskHandoffDraft): TaskHandoffEndpoint.ClaudeCodeDraft? {
        if (draft.target != HandoffTarget.CLAUDE) return null

        val repository = percentEncode(draft.requireRepository())
        val taskBrief = percentEncode(draft.requireTaskBrief())
        return TaskHandoffEndpoint.ClaudeCodeDraft(
            uri = "claude://code/new?q=$taskBrief&repo=$repository",
        )
    }

    private fun TaskHandoffDraft.requireRepository(): String = repository.trim().also {
        require(it.isNotEmpty()) { "Repository is required" }
    }

    private fun TaskHandoffDraft.requireTaskBrief(): String = taskBrief.trim().also {
        require(it.isNotEmpty()) { "Task brief is required" }
        require(it.length <= TaskHandoffDraft.MAX_TASK_BRIEF_LENGTH) {
            "Task brief exceeds the supported length"
        }
    }

    private fun percentEncode(value: String): String = buildString {
        value.toByteArray(StandardCharsets.UTF_8).forEach { byte ->
            val unsigned = byte.toInt() and 0xff
            if (unsigned.isUnreservedUriByte()) {
                append(unsigned.toChar())
            } else {
                append('%')
                append(HEX[unsigned ushr 4])
                append(HEX[unsigned and 0x0f])
            }
        }
    }

    private fun Int.isUnreservedUriByte(): Boolean =
        this in 'a'.code..'z'.code ||
            this in 'A'.code..'Z'.code ||
            this in '0'.code..'9'.code ||
            this == '-'.code ||
            this == '.'.code ||
            this == '_'.code ||
            this == '~'.code

    private const val HEX = "0123456789ABCDEF"
}
