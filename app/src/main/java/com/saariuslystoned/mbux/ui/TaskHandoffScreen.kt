package com.saariuslystoned.mbux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saariuslystoned.mbux.domain.handoff.HandoffTarget
import com.saariuslystoned.mbux.domain.handoff.TaskHandoffBuilder
import com.saariuslystoned.mbux.domain.handoff.TaskHandoffDraft
import com.saariuslystoned.mbux.ui.theme.MbuxTheme

@Composable
fun TaskHandoffScreen(
    initialTarget: HandoffTarget,
    onOpenBoard: () -> Unit,
    onContinueIn: (TaskHandoffDraft) -> Boolean,
    onOpenClaudeCodeDraft: (TaskHandoffDraft) -> Boolean,
) {
    var selectedTargetName by rememberSaveable { mutableStateOf(initialTarget.name) }
    var repository by rememberSaveable { mutableStateOf("") }
    var taskBrief by rememberSaveable { mutableStateOf("") }
    var localNotice by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTarget = HandoffTarget.valueOf(selectedTargetName)
    val draft = TaskHandoffDraft(
        target = selectedTarget,
        repository = repository,
        taskBrief = taskBrief,
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 26.dp),
        ) {
            Text(
                text = "Delegate a task",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Phone-only reviewed handoff",
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "DRAFT ONLY  •  You review and choose the destination  •  Nothing is auto-sent",
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = "TARGET",
                modifier = Modifier.padding(top = 22.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HandoffTarget.entries.forEach { target ->
                    FilterChip(
                        selected = target == selectedTarget,
                        onClick = {
                            selectedTargetName = target.name
                            localNotice = null
                        },
                        label = { Text(target.displayName) },
                    )
                }
            }

            OutlinedTextField(
                value = repository,
                onValueChange = {
                    repository = it
                    localNotice = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                label = { Text("Repository name (required)") },
                supportingText = { Text("Use an explicit, user-safe repository name.") },
                singleLine = true,
            )
            OutlinedTextField(
                value = taskBrief,
                onValueChange = {
                    taskBrief = it
                    localNotice = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text("Editable task brief (required)") },
                supportingText = {
                    Text("Typed draft input only. Voice transcription is the next separate device slice.")
                },
                minLines = 4,
            )

            ReviewCard(
                draft = draft,
                modifier = Modifier.padding(top = 18.dp),
            )

            if (localNotice != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = requireNotNull(localNotice),
                        modifier = Modifier.padding(13.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Button(
                onClick = {
                    localNotice = if (onContinueIn(draft)) {
                        "Android opened destination choices. MBUX did not create or send a task."
                    } else {
                        "No compatible handoff activity was available. Nothing was sent."
                    }
                },
                enabled = draft.isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            ) {
                Text("Continue in…")
            }
            Text(
                text = "The Android chooser remains the shared supported lane. You select the app and review there before any send.",
                modifier = Modifier.padding(top = 7.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )

            if (selectedTarget == HandoffTarget.CLAUDE) {
                OutlinedButton(
                    onClick = {
                        localNotice = if (onOpenClaudeCodeDraft(draft)) {
                            "Opened a Claude-owned draft. Review and send from Claude; MBUX did not submit it."
                        } else {
                            "Claude Code's draft route was unavailable. Nothing was sent."
                        }
                    },
                    enabled = draft.isReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                ) {
                    Text("Open Claude Code draft")
                }
                Text(
                    text = "Optional fast path: Claude owns the prefilled composer, final review, and send.",
                    modifier = Modifier.padding(top = 7.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    text = "Codex / ChatGPT uses the generic chooser only. MBUX cannot claim task creation or status tracking from this handoff.",
                    modifier = Modifier.padding(top = 14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            OutlinedButton(
                onClick = onOpenBoard,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text("Back to session board")
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ReviewCard(
    draft: TaskHandoffDraft,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "REVIEW",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (draft.isReady) {
                    TaskHandoffBuilder.buildPlainText(draft)
                } else {
                    "Choose a target, then add the required repository and task brief."
                },
                modifier = Modifier.padding(top = 9.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskHandoffPreview() {
    MbuxTheme {
        TaskHandoffScreen(
            initialTarget = HandoffTarget.CLAUDE,
            onOpenBoard = {},
            onContinueIn = { true },
            onOpenClaudeCodeDraft = { true },
        )
    }
}
