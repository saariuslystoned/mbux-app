package com.saariuslystoned.mbux.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saariuslystoned.mbux.domain.VoiceLifecycleState
import com.saariuslystoned.mbux.domain.board.SessionBoardRepository
import com.saariuslystoned.mbux.domain.board.SessionProvider
import com.saariuslystoned.mbux.domain.handoff.HandoffTarget
import com.saariuslystoned.mbux.domain.handoff.TaskHandoffDraft
import com.saariuslystoned.mbux.domain.navigation.AppLaunchDestination
import com.saariuslystoned.mbux.domain.status.SessionStatusFeed
import com.saariuslystoned.mbux.ui.theme.MbuxTheme

@Composable
fun MbuxCompanionApp(
    controller: VoiceCaptureController,
    boardRepository: SessionBoardRepository,
    sessionStatusFeed: SessionStatusFeed,
    initialDestination: AppLaunchDestination = AppLaunchDestination.DEFAULT,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinueHandoff: (TaskHandoffDraft) -> Boolean,
    onOpenClaudeCodeDraft: (TaskHandoffDraft) -> Boolean,
) {
    val state by controller.state.collectAsState()
    var destination by rememberSaveable {
        mutableStateOf(
            when (initialDestination) {
                AppLaunchDestination.DEFAULT -> PhoneDestination.SESSION_BOARD
                AppLaunchDestination.DISPATCH -> PhoneDestination.DISPATCH
            },
        )
    }
    var handoffTarget by rememberSaveable { mutableStateOf(HandoffTarget.CLAUDE) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (destination) {
            PhoneDestination.SESSION_BOARD -> SessionBoardScreen(
                repository = boardRepository,
                sessionStatusFeed = sessionStatusFeed,
                onOpenDispatch = { provider ->
                    handoffTarget = when (provider) {
                        SessionProvider.CLAUDE -> HandoffTarget.CLAUDE
                        SessionProvider.CODEX -> HandoffTarget.CODEX_CHATGPT
                    }
                    destination = PhoneDestination.DISPATCH
                },
                onOpenMicrophone = { destination = PhoneDestination.MICROPHONE },
            )

            PhoneDestination.DISPATCH -> TaskHandoffScreen(
                initialTarget = handoffTarget,
                onSelectSection = { section ->
                    when (section) {
                        PhoneSection.CLAUDE -> {
                            boardRepository.selectProvider(SessionProvider.CLAUDE)
                            destination = PhoneDestination.SESSION_BOARD
                        }

                        PhoneSection.CODEX -> {
                            boardRepository.selectProvider(SessionProvider.CODEX)
                            destination = PhoneDestination.SESSION_BOARD
                        }

                        PhoneSection.DISPATCH -> Unit
                    }
                },
                onContinueIn = onContinueHandoff,
                onOpenClaudeCodeDraft = onOpenClaudeCodeDraft,
            )

            PhoneDestination.MICROPHONE -> VoiceCaptureScreen(
                state = state,
                onOpenBoard = {
                    controller.cancel("Recording stopped when leaving the microphone screen")
                    destination = PhoneDestination.SESSION_BOARD
                },
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings,
                onHoldStarted = controller::beginHold,
                onHoldReleased = controller::releaseHold,
                onHoldCancelled = { controller.cancel("Touch interaction ended; audio discarded") },
                onCancel = controller::cancel,
                onRetry = controller::retry,
            )
        }
    }
}

private enum class PhoneDestination {
    SESSION_BOARD,
    DISPATCH,
    MICROPHONE,
}

@Composable
fun VoiceCaptureScreen(
    state: VoiceLifecycleState,
    onOpenBoard: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onHoldStarted: () -> Unit,
    onHoldReleased: () -> Unit,
    onHoldCancelled: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Text(
                text = "MBUX Companion",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Private phone microphone",
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )

            OutlinedButton(
                onClick = onOpenBoard,
                modifier = Modifier.padding(top = 14.dp),
            ) {
                Text("Open session board")
            }

            PrivacyNotice(modifier = Modifier.padding(top = 22.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.title(),
                        color = state.accentColor(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = state.detail(),
                        modifier = Modifier.padding(top = 7.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    PushToTalkControl(
                        state = state,
                        onHoldStarted = onHoldStarted,
                        onHoldReleased = onHoldReleased,
                        onHoldCancelled = onHoldCancelled,
                        modifier = Modifier.padding(top = 28.dp),
                    )

                    StateAction(
                        state = state,
                        onRequestPermission = onRequestPermission,
                        onOpenSettings = onOpenSettings,
                        onCancel = onCancel,
                        onRetry = onRetry,
                    )
                }
            }

            LifecycleStrip(
                state = state,
                modifier = Modifier.padding(top = 24.dp),
            )

            Text(
                text = "Microphone access begins only while you deliberately hold the control. Release, cancel, focus loss, a 30-second limit, or leaving the app stops capture and discards all buffered audio.",
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PrivacyNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Text(
            text = "MIC ONLY  •  Memory only  •  No files  •  No network",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StateAction(
    state: VoiceLifecycleState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        VoiceLifecycleState.Idle -> Button(
            onClick = onRequestPermission,
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Text("Enable microphone")
        }

        VoiceLifecycleState.PermissionDenied -> Button(
            onClick = onRequestPermission,
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Text("Try permission again")
        }

        VoiceLifecycleState.PermissionPermanentlyDenied -> Button(
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Text("Open app settings")
        }

        VoiceLifecycleState.Recording -> OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Text("Cancel and discard")
        }

        is VoiceLifecycleState.Cancelled,
        is VoiceLifecycleState.Error,
        -> Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Text("Reset capture")
        }

        else -> Spacer(modifier = Modifier.height(58.dp))
    }
}

@Composable
private fun PushToTalkControl(
    state: VoiceLifecycleState,
    onHoldStarted: () -> Unit,
    onHoldReleased: () -> Unit,
    onHoldCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state is VoiceLifecycleState.Ready
    val recording = state is VoiceLifecycleState.Recording
    val background = when {
        ready -> MaterialTheme.colorScheme.primary
        recording -> Color(0xFFE14B4B)
        else -> state.accentColor()
    }
    val foreground = if (ready) MaterialTheme.colorScheme.onPrimary else Color.White

    // This handler remains attached while Ready changes to Recording, preserving the release.
    val interactionModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                onHoldStarted()
                if (tryAwaitRelease()) onHoldReleased() else onHoldCancelled()
            },
        )
    }

    Surface(
        modifier = modifier
            .size(176.dp)
            .then(interactionModifier)
            .semantics {
                role = Role.Button
                contentDescription = "Local push to talk microphone"
                stateDescription = state.title()
                when (state) {
                    VoiceLifecycleState.Ready -> onClick(label = "Start local capture") {
                        onHoldStarted()
                        true
                    }

                    VoiceLifecycleState.Recording -> onClick(label = "Stop and discard capture") {
                        onHoldReleased()
                        true
                    }

                    else -> Unit
                }
            },
        color = background,
        contentColor = foreground,
        shape = CircleShape,
        shadowElevation = 8.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.controlLabel(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Text(
                    text = state.controlHint(),
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun LifecycleStrip(state: VoiceLifecycleState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "LOCAL, CANCELLABLE CAPTURE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LifecycleStep(
                    "ACCESS",
                    state is VoiceLifecycleState.Idle ||
                        state is VoiceLifecycleState.RequestingPermission ||
                        state is VoiceLifecycleState.PermissionDenied ||
                        state is VoiceLifecycleState.PermissionPermanentlyDenied,
                )
                LifecycleStep("READY", state is VoiceLifecycleState.Ready)
                LifecycleStep("RECORD", state is VoiceLifecycleState.Recording)
                LifecycleStep("DISCARD", state is VoiceLifecycleState.Cancelled)
            }
        }
    }
}

@Composable
private fun LifecycleStep(label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
        )
        Text(
            text = label,
            modifier = Modifier.padding(top = 6.dp),
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private fun VoiceLifecycleState.title(): String = when (this) {
    VoiceLifecycleState.Idle -> "Microphone access needed"
    VoiceLifecycleState.RequestingPermission -> "Requesting permission"
    VoiceLifecycleState.PermissionDenied -> "Microphone permission denied"
    VoiceLifecycleState.PermissionPermanentlyDenied -> "Enable access in settings"
    VoiceLifecycleState.Ready -> "Ready"
    VoiceLifecycleState.Recording -> "Recording locally"
    is VoiceLifecycleState.Sending -> "Unavailable in mic-only build"
    is VoiceLifecycleState.Playing -> "Unavailable in mic-only build"
    is VoiceLifecycleState.Cancelled -> "Capture stopped"
    is VoiceLifecycleState.Error -> "Capture error"
}

private fun VoiceLifecycleState.detail(): String = when (this) {
    VoiceLifecycleState.Idle -> "Enable microphone access to use private, on-device push to talk."
    VoiceLifecycleState.RequestingPermission -> "Choose whether this app may use the phone microphone."
    VoiceLifecycleState.PermissionDenied -> "Nothing was recorded. You can review the explanation and try again."
    VoiceLifecycleState.PermissionPermanentlyDenied -> "Capture remains off. Android settings can restore access."
    VoiceLifecycleState.Ready -> "Press and hold to capture. Audio is discarded as it is read."
    VoiceLifecycleState.Recording -> "Release to stop. No audio is saved or sent anywhere."
    is VoiceLifecycleState.Sending -> "Sending is not part of the current local microphone scope."
    is VoiceLifecycleState.Playing -> "Playback is not part of the current local microphone scope."
    is VoiceLifecycleState.Cancelled -> reason
    is VoiceLifecycleState.Error -> userMessage
}

private fun VoiceLifecycleState.controlLabel(): String = when (this) {
    VoiceLifecycleState.Ready -> "HOLD TO TALK"
    VoiceLifecycleState.Recording -> "RELEASE TO STOP"
    VoiceLifecycleState.RequestingPermission -> "WAITING"
    VoiceLifecycleState.PermissionDenied,
    VoiceLifecycleState.PermissionPermanentlyDenied,
    VoiceLifecycleState.Idle,
    -> "MIC OFF"

    is VoiceLifecycleState.Cancelled -> "STOPPED"
    is VoiceLifecycleState.Error -> "ERROR"
    else -> "NOT AVAILABLE"
}

private fun VoiceLifecycleState.controlHint(): String = when (this) {
    VoiceLifecycleState.Ready -> "local capture"
    VoiceLifecycleState.Recording -> "audio stays private"
    VoiceLifecycleState.RequestingPermission -> "system prompt"
    VoiceLifecycleState.PermissionDenied -> "permission required"
    VoiceLifecycleState.PermissionPermanentlyDenied -> "use settings below"
    VoiceLifecycleState.Idle -> "enable below"
    else -> "no audio retained"
}

@Composable
private fun VoiceLifecycleState.accentColor(): Color = when (this) {
    VoiceLifecycleState.Ready -> MaterialTheme.colorScheme.primary
    VoiceLifecycleState.Recording -> Color(0xFFE14B4B)
    VoiceLifecycleState.PermissionDenied,
    VoiceLifecycleState.PermissionPermanentlyDenied,
    is VoiceLifecycleState.Error,
    -> MaterialTheme.colorScheme.error

    is VoiceLifecycleState.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.outline
}

@Preview(showBackground = true)
@Composable
private fun VoiceCapturePreview() {
    MbuxTheme {
        VoiceCaptureScreen(
            state = VoiceLifecycleState.Ready,
            onOpenBoard = {},
            onRequestPermission = {},
            onOpenSettings = {},
            onHoldStarted = {},
            onHoldReleased = {},
            onHoldCancelled = {},
            onCancel = {},
            onRetry = {},
        )
    }
}
