package com.saariuslystoned.mbux.ui

import com.saariuslystoned.mbux.domain.VoiceLifecycleEvent
import com.saariuslystoned.mbux.domain.VoiceLifecycleMachine
import com.saariuslystoned.mbux.domain.VoiceLifecycleState
import com.saariuslystoned.mbux.platform.audio.CaptureInterruption
import com.saariuslystoned.mbux.platform.audio.CaptureStartResult
import com.saariuslystoned.mbux.platform.audio.LocalAudioCapture
import kotlinx.coroutines.flow.StateFlow

/** Coordinates permission-gated, local-only microphone capture. */
class VoiceCaptureController(
    private val capture: LocalAudioCapture,
    initialPermissionGranted: Boolean,
) {
    private val machine = VoiceLifecycleMachine(
        if (initialPermissionGranted) VoiceLifecycleState.Ready else VoiceLifecycleState.Idle,
    )
    private var permissionGranted = initialPermissionGranted
    private var closed = false

    val state: StateFlow<VoiceLifecycleState> = machine.state

    /** Returns true only when the caller should launch the system permission dialog. */
    fun beginPermissionRequest(): Boolean {
        if (closed || permissionGranted) return false

        machine.handle(VoiceLifecycleEvent.BeginSetup)
        return state.value is VoiceLifecycleState.RequestingPermission
    }

    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        if (closed) return

        permissionGranted = granted
        machine.handle(
            if (granted) {
                VoiceLifecycleEvent.PermissionGranted
            } else {
                VoiceLifecycleEvent.PermissionDenied(permanently = permanentlyDenied)
            },
        )
    }

    /** Reconciles permission after returning from settings or after an external revocation. */
    fun syncPermission(granted: Boolean) {
        if (closed || granted == permissionGranted) return

        permissionGranted = granted
        if (granted) {
            machine.handle(VoiceLifecycleEvent.PermissionGranted)
        } else {
            capture.cancel()
            machine.handle(VoiceLifecycleEvent.PermissionRevoked)
        }
    }

    fun beginHold() {
        if (closed || state.value !is VoiceLifecycleState.Ready) return
        if (!permissionGranted) {
            machine.handle(VoiceLifecycleEvent.PermissionRevoked)
            return
        }

        machine.handle(VoiceLifecycleEvent.HoldStarted)
        when (val result = capture.start(::onCaptureInterrupted)) {
            CaptureStartResult.Started -> Unit
            CaptureStartResult.PermissionMissing -> {
                permissionGranted = false
                machine.handle(VoiceLifecycleEvent.PermissionRevoked)
            }

            is CaptureStartResult.Failed -> machine.handle(
                VoiceLifecycleEvent.CaptureFailed(result.userMessage),
            )
        }
    }

    /** Stops capture, discards all buffered PCM, and returns to ready. */
    fun releaseHold() {
        if (closed || state.value !is VoiceLifecycleState.Recording) return

        capture.stop()
        machine.handle(VoiceLifecycleEvent.CaptureFinished)
    }

    fun cancel(reason: String = "Recording cancelled; audio discarded") {
        if (closed || state.value !is VoiceLifecycleState.Recording) return

        capture.cancel()
        machine.handle(VoiceLifecycleEvent.Cancel(reason))
    }

    fun retry() {
        if (closed) return

        machine.handle(
            if (permissionGranted) {
                VoiceLifecycleEvent.Retry
            } else {
                VoiceLifecycleEvent.ResetPermissionFlow
            },
        )
    }

    fun onHostStopped() {
        if (closed || state.value !is VoiceLifecycleState.Recording) return

        capture.cancel()
        machine.handle(
            VoiceLifecycleEvent.Cancel("Recording stopped when the app left the foreground"),
        )
    }

    fun close() {
        if (closed) return

        closed = true
        capture.close()
        machine.handle(VoiceLifecycleEvent.Teardown)
    }

    private fun onCaptureInterrupted(interruption: CaptureInterruption) {
        if (closed || state.value !is VoiceLifecycleState.Recording) return

        when (interruption) {
            CaptureInterruption.AudioFocusLost -> machine.handle(
                VoiceLifecycleEvent.Cancel("Recording stopped after audio focus was lost"),
            )

            CaptureInterruption.TimeLimitReached -> machine.handle(
                VoiceLifecycleEvent.Cancel("The 30-second recording limit was reached"),
            )

            CaptureInterruption.ReadFailed -> machine.handle(
                VoiceLifecycleEvent.CaptureFailed("Microphone capture stopped unexpectedly"),
            )
        }
    }
}
