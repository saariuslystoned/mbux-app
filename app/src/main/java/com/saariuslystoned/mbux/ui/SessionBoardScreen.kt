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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saariuslystoned.mbux.domain.board.AttachFixtureMatch
import com.saariuslystoned.mbux.domain.board.SessionBoardItem
import com.saariuslystoned.mbux.domain.board.SessionBoardRepository
import com.saariuslystoned.mbux.domain.board.SessionBoardSnapshot
import com.saariuslystoned.mbux.domain.board.SessionOrigin
import com.saariuslystoned.mbux.domain.board.SessionProvider
import com.saariuslystoned.mbux.domain.board.SessionStatus
import com.saariuslystoned.mbux.domain.status.SessionStatusFeed
import com.saariuslystoned.mbux.ui.theme.MbuxTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SessionBoardScreen(
    repository: SessionBoardRepository,
    sessionStatusFeed: SessionStatusFeed,
    onOpenDispatch: (SessionProvider) -> Unit,
    onOpenMicrophone: () -> Unit,
) {
    val snapshot by repository.snapshot.collectAsState()
    val relayState by sessionStatusFeed.state.collectAsState()
    var mode by rememberSaveable { mutableStateOf(BoardMode.BOARD) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var localNotice by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteCandidate by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedSessions = snapshot.sessions.filter { it.provider == snapshot.selectedProvider }
    val activeSessions = selectedSessions.filterNot { it.archived }
    val archivedSessions = selectedSessions.filter { it.archived }

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
                text = "MBUX Companion",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${snapshot.selectedProvider.displayName} session board",
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )

            PhoneSectionPicker(
                selected = snapshot.selectedProvider.toPhoneSection(),
                onSelect = { section ->
                    when (section) {
                        PhoneSection.CLAUDE -> repository.selectProvider(SessionProvider.CLAUDE)
                        PhoneSection.CODEX -> repository.selectProvider(SessionProvider.CODEX)
                        PhoneSection.DISPATCH -> onOpenDispatch(snapshot.selectedProvider)
                    }
                },
                modifier = Modifier.padding(top = 14.dp),
            )

            SessionRelayPanel(
                state = relayState,
                selectedProvider = snapshot.selectedProvider,
                modifier = Modifier.padding(top = 14.dp),
            )

            PrototypeNotice(modifier = Modifier.padding(top = 14.dp))

            if (localNotice != null) {
                LocalNotice(
                    message = requireNotNull(localNotice),
                    onDismiss = { localNotice = null },
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            when (mode) {
                BoardMode.BOARD -> BoardContent(
                    sessions = activeSessions,
                    archivedSessions = archivedSessions,
                    showArchived = showArchived,
                    onToggleArchived = { showArchived = !showArchived },
                    onAttach = { mode = BoardMode.ATTACH },
                    onOpenMicrophone = onOpenMicrophone,
                    onArchive = repository::archive,
                    onRestore = repository::restore,
                    onDelete = { deleteCandidate = it },
                    onChoice = { session, choiceNumber ->
                        if (repository.chooseLocalDecision(session.id, choiceNumber)) {
                            localNotice = "Local simulation only: choice $choiceNumber updated this fixture. Nothing was sent."
                        }
                    },
                )

                BoardMode.ATTACH -> AttachExistingContent(
                    provider = snapshot.selectedProvider,
                    matches = repository.attachMatches(snapshot.selectedProvider),
                    onAttach = { match ->
                        if (repository.attach(match.id)) {
                            localNotice = "Attached a local fixture. No Mac session was scanned or contacted."
                            mode = BoardMode.BOARD
                        }
                    },
                    onBack = { mode = BoardMode.BOARD },
                )
            }
        }
    }

    val deleteId = deleteCandidate
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete local fixture?") },
            text = { Text("This removes only the app-private fixture record. It cannot affect a provider session.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.delete(deleteId)
                        deleteCandidate = null
                        localNotice = "Deleted the local fixture record."
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Keep")
                }
            },
        )
    }
}

@Composable
private fun PrototypeNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = "PHONE-SIDE PERSONAL PROTOTYPE  •  Fixture actions stay local  •  No provider or car control",
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LocalNotice(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun BoardContent(
    sessions: List<SessionBoardItem>,
    archivedSessions: List<SessionBoardItem>,
    showArchived: Boolean,
    onToggleArchived: () -> Unit,
    onAttach: () -> Unit,
    onOpenMicrophone: () -> Unit,
    onArchive: (String) -> Boolean,
    onRestore: (String) -> Boolean,
    onDelete: (String) -> Unit,
    onChoice: (SessionBoardItem, Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onAttach, modifier = Modifier.weight(1f)) {
            Text("Attach existing")
        }
        OutlinedButton(onClick = onOpenMicrophone, modifier = Modifier.weight(1f)) {
            Text("Private microphone")
        }
    }

    Text(
        text = "ACTIVE FIXTURES",
        modifier = Modifier.padding(top = 24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
    if (sessions.isEmpty()) {
        EmptyBoardMessage("No active fixtures for this provider.")
    } else {
        sessions.forEach { session ->
            SessionCard(
                session = session,
                archived = false,
                onArchive = { onArchive(session.id) },
                onRestore = {},
                onDelete = { onDelete(session.id) },
                onChoice = { choice -> onChoice(session, choice) },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }

    OutlinedButton(
        onClick = onToggleArchived,
        modifier = Modifier.padding(top = 20.dp),
    ) {
        Text(if (showArchived) "Hide archived (${archivedSessions.size})" else "Show archived (${archivedSessions.size})")
    }
    if (showArchived) {
        archivedSessions.forEach { session ->
            SessionCard(
                session = session,
                archived = true,
                onArchive = {},
                onRestore = { onRestore(session.id) },
                onDelete = { onDelete(session.id) },
                onChoice = {},
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (archivedSessions.isEmpty()) EmptyBoardMessage("No archived fixtures for this provider.")
    }
    Spacer(modifier = Modifier.height(28.dp))
}

@Composable
private fun SessionCard(
    session: SessionBoardItem,
    archived: Boolean,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onChoice: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProviderBadge(session.provider)
                StatusBadge(session.status)
                Spacer(modifier = Modifier.weight(1f))
                if (session.artifactReady) {
                    Text(
                        text = "● Artifact ready",
                        color = Color(0xFF9FD4C4),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = session.repository,
                modifier = Modifier.padding(top = 13.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = session.taskTitle,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = session.origin.displayName,
                modifier = Modifier.padding(top = 7.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )

            val prompt = session.needsYouPrompt
            if (!archived && session.status == SessionStatus.NEEDS_YOU && prompt != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Column(modifier = Modifier.padding(13.dp)) {
                        Text(
                            text = prompt.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        prompt.choices.take(3).forEachIndexed { index, choice ->
                            OutlinedButton(
                                onClick = { onChoice(index + 1) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            ) {
                                Text("${index + 1}. $choice")
                            }
                        }
                        Text(
                            text = "Local simulation only; choices are not sent.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = if (archived) onRestore else onArchive,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (archived) "Restore" else "Archive")
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun ProviderBadge(provider: SessionProvider) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = provider.displayName,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatusBadge(status: SessionStatus) {
    Surface(
        shape = RoundedCornerShape(50),
        color = status.badgeColor(),
    ) {
        Text(
            text = status.displayName,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = Color(0xFF08131F),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SessionStatus.badgeColor(): Color = when (this) {
    SessionStatus.RUNNING -> Color(0xFF7CC7FF)
    SessionStatus.DONE -> Color(0xFF9FD4C4)
    SessionStatus.NEEDS_YOU -> Color(0xFFFFCB74)
    SessionStatus.FAILED -> Color(0xFFFFB4AB)
}

@Composable
private fun EmptyBoardMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(top = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun AttachExistingContent(
    provider: SessionProvider,
    matches: List<AttachFixtureMatch>,
    onAttach: (AttachFixtureMatch) -> Unit,
    onBack: () -> Unit,
) {
    SectionHeader(
        title = "Attach existing fixture",
        description = "At most three representative local matches are shown. This does not enumerate or contact sessions on your Mac.",
    )
    Text(
        text = "${provider.displayName} fixture matches",
        modifier = Modifier.padding(top = 14.dp),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    if (matches.isEmpty()) {
        EmptyBoardMessage("All available fixture matches are already attached.")
    } else {
        matches.take(3).forEach { match ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProviderBadge(match.provider)
                        StatusBadge(match.status)
                    }
                    Text(
                        text = match.repository,
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = match.taskTitle,
                        modifier = Modifier.padding(top = 3.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(
                        onClick = { onAttach(match) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text("Attach this fixture")
                    }
                }
            }
        }
    }
    OutlinedButton(onClick = onBack, modifier = Modifier.padding(top = 18.dp)) {
        Text("Back to board")
    }
    Spacer(modifier = Modifier.height(28.dp))
}

@Composable
private fun SectionHeader(title: String, description: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = 22.dp),
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = description,
        modifier = Modifier.padding(top = 7.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

private enum class BoardMode {
    BOARD,
    ATTACH,
}

private fun SessionProvider.toPhoneSection(): PhoneSection = when (this) {
    SessionProvider.CLAUDE -> PhoneSection.CLAUDE
    SessionProvider.CODEX -> PhoneSection.CODEX
}

@Preview(showBackground = true)
@Composable
private fun SessionBoardPreview() {
    MbuxTheme {
        SessionBoardScreen(
            repository = PreviewBoardRepository(),
            sessionStatusFeed = com.saariuslystoned.mbux.broker.status.UnavailableSessionStatusFeed,
            onOpenDispatch = {},
            onOpenMicrophone = {},
        )
    }
}

private class PreviewBoardRepository : SessionBoardRepository {
    override val snapshot: StateFlow<SessionBoardSnapshot> = MutableStateFlow(
        SessionBoardSnapshot(
            selectedProvider = SessionProvider.CLAUDE,
            sessions = listOf(
                SessionBoardItem(
                    id = "preview",
                    provider = SessionProvider.CLAUDE,
                    repository = "mbux-app",
                    taskTitle = "Build the phone session board",
                    status = SessionStatus.NEEDS_YOU,
                    artifactReady = true,
                    origin = SessionOrigin.MBUX_LAUNCHED_FIXTURE,
                    needsYouPrompt = com.saariuslystoned.mbux.domain.board.NeedsYouPrompt(
                        "Choose a local fixture path.",
                        listOf("Minimal", "Follow-up", "Stop"),
                    ),
                ),
            ),
        ),
    )

    override fun selectProvider(provider: SessionProvider) = Unit
    override fun attachMatches(provider: SessionProvider): List<AttachFixtureMatch> = emptyList()
    override fun attach(matchId: String): Boolean = false
    override fun archive(sessionId: String): Boolean = false
    override fun restore(sessionId: String): Boolean = false
    override fun delete(sessionId: String): Boolean = false
    override fun chooseLocalDecision(sessionId: String, choiceNumber: Int): Boolean = false
}
