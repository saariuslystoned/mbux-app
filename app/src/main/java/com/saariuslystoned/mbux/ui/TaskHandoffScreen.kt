package com.saariuslystoned.mbux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saariuslystoned.mbux.domain.handoff.HandoffTarget
import com.saariuslystoned.mbux.domain.handoff.TaskHandoffDraft
import com.saariuslystoned.mbux.ui.theme.MbuxTheme

@Composable
fun TaskHandoffScreen(
    initialTarget: HandoffTarget,
    onSelectSection: (PhoneSection) -> Unit,
    onContinueIn: (TaskHandoffDraft) -> Boolean,
    onOpenClaudeCodeDraft: (TaskHandoffDraft) -> Boolean,
) {
    var selectedTargetName by rememberSaveable { mutableStateOf(initialTarget.name) }
    // Task text is private user data and must not enter Android saved-instance state.
    var taskBrief by remember { mutableStateOf("") }
    var localNotice by remember { mutableStateOf<String?>(null) }
    val selectedTarget = HandoffTarget.valueOf(selectedTargetName)
    val draft = TaskHandoffDraft.publicProof(
        target = selectedTarget,
        taskBrief = taskBrief,
    )

    val selectTarget: (HandoffTarget) -> Unit = { target ->
        selectedTargetName = target.name
        localNotice = null
    }
    val updateTaskBrief: (String) -> Unit = {
        taskBrief = it.take(TaskHandoffDraft.MAX_TASK_BRIEF_LENGTH)
        localNotice = null
    }
    val continueIn: () -> Unit = {
        localNotice = if (onContinueIn(draft)) {
            "Chooser opened. MBUX did not create or send a task."
        } else {
            "No compatible handoff activity. Nothing was sent."
        }
    }
    val openClaudeDraft: () -> Unit = {
        localNotice = if (onOpenClaudeCodeDraft(draft)) {
            "Claude draft opened. Claude owns review and send."
        } else {
            "Claude Code draft unavailable. Nothing was sent."
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useFoldLayout = maxWidth >= 720.dp && maxHeight >= 600.dp
            if (useFoldLayout) {
                FoldHandoffContent(
                    draft = draft,
                    localNotice = localNotice,
                    onSelectSection = onSelectSection,
                    onSelectTarget = selectTarget,
                    onTaskBriefChange = updateTaskBrief,
                    onContinueIn = continueIn,
                    onOpenClaudeCodeDraft = openClaudeDraft,
                )
            } else {
                PhoneHandoffContent(
                    draft = draft,
                    localNotice = localNotice,
                    onSelectSection = onSelectSection,
                    onSelectTarget = selectTarget,
                    onTaskBriefChange = updateTaskBrief,
                    onContinueIn = continueIn,
                    onOpenClaudeCodeDraft = openClaudeDraft,
                )
            }
        }
    }
}

@Composable
private fun FoldHandoffContent(
    draft: TaskHandoffDraft,
    localNotice: String?,
    onSelectSection: (PhoneSection) -> Unit,
    onSelectTarget: (HandoffTarget) -> Unit,
    onTaskBriefChange: (String) -> Unit,
    onContinueIn: () -> Unit,
    onOpenClaudeCodeDraft: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        HandoffHeader(
            onSelectSection = onSelectSection,
            foldLayout = true,
        )
        DraftBoundaryBanner(modifier = Modifier.padding(top = 7.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HandoffEntryPanel(
                draft = draft,
                onSelectTarget = onSelectTarget,
                onTaskBriefChange = onTaskBriefChange,
                modifier = Modifier
                    .weight(1.06f),
                fillTaskHeight = true,
            )
            Column(
                modifier = Modifier.weight(0.94f),
            ) {
                ReviewCard(
                    draft = draft,
                    modifier = Modifier.fillMaxWidth(),
                    compact = true,
                )
                LocalHandoffNotice(
                    message = localNotice,
                    modifier = Modifier.padding(top = 7.dp),
                )
                HandoffActions(
                    draft = draft,
                    onContinueIn = onContinueIn,
                    onOpenClaudeCodeDraft = onOpenClaudeCodeDraft,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun PhoneHandoffContent(
    draft: TaskHandoffDraft,
    localNotice: String?,
    onSelectSection: (PhoneSection) -> Unit,
    onSelectTarget: (HandoffTarget) -> Unit,
    onTaskBriefChange: (String) -> Unit,
    onContinueIn: () -> Unit,
    onOpenClaudeCodeDraft: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        HandoffHeader(
            onSelectSection = onSelectSection,
            foldLayout = false,
        )
        DraftBoundaryBanner(modifier = Modifier.padding(top = 8.dp))
        HandoffEntryPanel(
            draft = draft,
            onSelectTarget = onSelectTarget,
            onTaskBriefChange = onTaskBriefChange,
            modifier = Modifier.padding(top = 10.dp),
            fillTaskHeight = false,
        )
        ReviewCard(
            draft = draft,
            modifier = Modifier.padding(top = 10.dp),
            compact = false,
        )
        LocalHandoffNotice(
            message = localNotice,
            modifier = Modifier.padding(top = 8.dp),
        )
        HandoffActions(
            draft = draft,
            onContinueIn = onContinueIn,
            onOpenClaudeCodeDraft = onOpenClaudeCodeDraft,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HandoffHeader(
    onSelectSection: (PhoneSection) -> Unit,
    foldLayout: Boolean,
) {
    if (foldLayout) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HandoffTitle(modifier = Modifier.weight(0.8f))
            PhoneSectionPicker(
                selected = PhoneSection.DISPATCH,
                onSelect = onSelectSection,
                modifier = Modifier.weight(1.2f),
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            HandoffTitle()
            PhoneSectionPicker(
                selected = PhoneSection.DISPATCH,
                onSelect = onSelectSection,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun HandoffTitle(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
            Text(
                text = "Dispatch",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Phone draft • review before handoff",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
    }
}

@Composable
private fun DraftBoundaryBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = "DRAFT • Review first • Nothing auto-sent • No status callback",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HandoffEntryPanel(
    draft: TaskHandoffDraft,
    onSelectTarget: (HandoffTarget) -> Unit,
    onTaskBriefChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fillTaskHeight: Boolean,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "TARGET",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            HandoffTarget.entries.forEach { target ->
                FilterChip(
                    selected = target == draft.target,
                    onClick = { onSelectTarget(target) },
                    label = { Text(target.displayName) },
                )
            }
        }
        ProofRepositoryCard(
            repository = draft.repository,
            modifier = Modifier.padding(top = 6.dp),
        )
        OutlinedTextField(
            value = draft.taskBrief,
            onValueChange = onTaskBriefChange,
            modifier = if (fillTaskHeight) {
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            },
            label = { Text("Task brief *") },
            minLines = if (fillTaskHeight) 7 else 3,
            maxLines = 10,
        )
        Text(
            text = "Typed only • ${draft.taskBrief.length}/${TaskHandoffDraft.MAX_TASK_BRIEF_LENGTH} • Voice later",
            modifier = Modifier.padding(top = 3.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProofRepositoryCard(
    repository: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(
                text = "PUBLIC PROOF REPOSITORY",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = repository,
                modifier = Modifier.padding(top = 2.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Fixed for this proof • Catalog + sign-in deferred",
                modifier = Modifier.padding(top = 2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReviewCard(
    draft: TaskHandoffDraft,
    modifier: Modifier = Modifier,
    compact: Boolean,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(if (compact) 12.dp else 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "REVIEW",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = draft.target.displayName,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            ReviewValue(
                label = "REPOSITORY",
                value = draft.repository.trim().ifEmpty { "Required" },
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 1,
            )
            ReviewValue(
                label = "TASK",
                value = draft.taskBrief.trim().ifEmpty { "Required" },
                modifier = Modifier.padding(top = 8.dp),
                maxLines = if (compact) 6 else 8,
            )
            Text(
                text = "Review here, then review again in the destination app.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ReviewValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    maxLines: Int,
) {
    Text(
        text = label,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = value,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun LocalHandoffNotice(
    message: String?,
    modifier: Modifier = Modifier,
) {
    if (message == null) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HandoffActions(
    draft: TaskHandoffDraft,
    onContinueIn: () -> Unit,
    onOpenClaudeCodeDraft: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = onContinueIn,
            enabled = draft.isReady,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue in…")
        }
        Text(
            text = "System chooser • You pick the app • No status result",
            modifier = Modifier.padding(top = 3.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (draft.target == HandoffTarget.CLAUDE) {
            OutlinedButton(
                onClick = onOpenClaudeCodeDraft,
                enabled = draft.isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                Text("Open Claude Code draft")
            }
            Text(
                text = "Optional • Claude owns final review and send",
                modifier = Modifier.padding(top = 3.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = "Codex / ChatGPT uses the chooser only.",
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 883, heightDp = 851)
@Composable
private fun FoldTaskHandoffPreview() {
    MbuxTheme {
        TaskHandoffScreen(
            initialTarget = HandoffTarget.CLAUDE,
            onSelectSection = {},
            onContinueIn = { true },
            onOpenClaudeCodeDraft = { true },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhoneTaskHandoffPreview() {
    MbuxTheme {
        TaskHandoffScreen(
            initialTarget = HandoffTarget.CODEX_CHATGPT,
            onSelectSection = {},
            onContinueIn = { true },
            onOpenClaudeCodeDraft = { true },
        )
    }
}
