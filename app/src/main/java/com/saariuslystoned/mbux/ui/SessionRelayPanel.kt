package com.saariuslystoned.mbux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saariuslystoned.mbux.domain.board.SessionProvider
import com.saariuslystoned.mbux.domain.status.NormalizedSessionLifecycle
import com.saariuslystoned.mbux.domain.status.NormalizedSessionProvider
import com.saariuslystoned.mbux.domain.status.NormalizedSessionSummary
import com.saariuslystoned.mbux.domain.status.SessionStatusFeedState
import com.saariuslystoned.mbux.domain.status.VerifiedSessionStatusSnapshot

internal enum class RelayPresentationTone {
    NEUTRAL,
    GOOD,
    CAUTION,
    ERROR,
}

internal data class SessionRelayPresentation(
    val statusLabel: String,
    val detail: String,
    val emptyMessage: String,
    val tone: RelayPresentationTone,
    val sessions: List<NormalizedSessionSummary>,
    val isCurrent: Boolean,
)

internal fun sessionRelayPresentation(
    state: SessionStatusFeedState,
    selectedProvider: SessionProvider,
): SessionRelayPresentation {
    val provider = when (selectedProvider) {
        SessionProvider.CLAUDE -> NormalizedSessionProvider.CLAUDE
        SessionProvider.CODEX -> NormalizedSessionProvider.CODEX
    }

    fun filtered(snapshot: VerifiedSessionStatusSnapshot) =
        snapshot.sessions.filter { it.provider == provider }

    return when (state) {
        SessionStatusFeedState.Unknown -> SessionRelayPresentation(
            statusLabel = "Unknown",
            detail = "No CP-1 session status has been verified.",
            emptyMessage = "No verified status is available.",
            tone = RelayPresentationTone.NEUTRAL,
            sessions = emptyList(),
            isCurrent = false,
        )

        SessionStatusFeedState.NotEnrolled -> SessionRelayPresentation(
            statusLabel = "Not enrolled",
            detail = "A safe CP-1 read capability and device enrollment are not configured.",
            emptyMessage = "Remote session status remains unavailable.",
            tone = RelayPresentationTone.NEUTRAL,
            sessions = emptyList(),
            isCurrent = false,
        )

        is SessionStatusFeedState.Live -> SessionRelayPresentation(
            statusLabel = "Live",
            detail = "Normalized read-only status. MBUX cannot send commands.",
            emptyMessage = "No verified ${provider.displayName} sessions in the latest snapshot.",
            tone = RelayPresentationTone.GOOD,
            sessions = filtered(state.snapshot),
            isCurrent = true,
        )

        is SessionStatusFeedState.Stale -> SessionRelayPresentation(
            statusLabel = "Stale",
            detail = "The last verified snapshot is out of date; rows are not current.",
            emptyMessage = "No retained ${provider.displayName} rows are available.",
            tone = RelayPresentationTone.CAUTION,
            sessions = filtered(state.snapshot),
            isCurrent = false,
        )

        is SessionStatusFeedState.Disconnected -> SessionRelayPresentation(
            statusLabel = "Disconnected",
            detail = if (state.lastVerified == null) {
                "The relay is unavailable; no verified session status is shown."
            } else {
                "The relay is unavailable; retained rows are stale and read-only."
            },
            emptyMessage = "No verified ${provider.displayName} status is available.",
            tone = RelayPresentationTone.ERROR,
            sessions = state.lastVerified?.let(::filtered).orEmpty(),
            isCurrent = false,
        )
    }
}

@Composable
internal fun SessionRelayPanel(
    state: SessionStatusFeedState,
    selectedProvider: SessionProvider,
    modifier: Modifier = Modifier,
) {
    val presentation = sessionRelayPresentation(state, selectedProvider)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "CP-1 SESSION STATUS",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                RelayHealthBadge(presentation.statusLabel, presentation.tone)
            }
            Text(
                text = presentation.detail,
                modifier = Modifier.padding(top = 7.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )

            if (presentation.sessions.isEmpty()) {
                Text(
                    text = presentation.emptyMessage,
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                presentation.sessions.forEach { session ->
                    RelaySessionRow(
                        session = session,
                        isCurrent = presentation.isCurrent,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RelayHealthBadge(label: String, tone: RelayPresentationTone) {
    val color = when (tone) {
        RelayPresentationTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
        RelayPresentationTone.GOOD -> Color(0xFF9FD4C4)
        RelayPresentationTone.CAUTION -> Color(0xFFFFCB74)
        RelayPresentationTone.ERROR -> Color(0xFFFFB4AB)
    }
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = if (tone == RelayPresentationTone.NEUTRAL) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                Color(0xFF08131F)
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RelaySessionRow(
    session: NormalizedSessionSummary,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = session.provider.displayName,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                RelayLifecycleBadge(session.lifecycle)
                Spacer(modifier = Modifier.weight(1f))
                if (session.artifactReady) {
                    Text(
                        text = "● Artifact ready",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = session.repository,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = session.taskTitle,
                modifier = Modifier.padding(top = 2.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (isCurrent) session.origin.displayName else "Not current • ${session.origin.displayName}",
                modifier = Modifier.padding(top = 5.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun RelayLifecycleBadge(lifecycle: NormalizedSessionLifecycle) {
    val (color, contentColor) = when (lifecycle) {
        NormalizedSessionLifecycle.RUNNING -> Color(0xFF7CC7FF) to Color(0xFF08131F)
        NormalizedSessionLifecycle.DONE -> Color(0xFF9FD4C4) to Color(0xFF08131F)
        NormalizedSessionLifecycle.NEEDS_YOU -> Color(0xFFFFCB74) to Color(0xFF08131F)
        NormalizedSessionLifecycle.FAILED -> Color(0xFFFFB4AB) to Color(0xFF08131F)
        NormalizedSessionLifecycle.UNKNOWN ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(
            text = lifecycle.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
