package com.saariuslystoned.mbux.ui

import com.saariuslystoned.mbux.domain.VoiceLifecycleState
import com.saariuslystoned.mbux.platform.audio.CaptureInterruption
import com.saariuslystoned.mbux.platform.audio.CaptureStartResult
import com.saariuslystoned.mbux.platform.audio.LocalAudioCapture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCaptureControllerTest {
    @Test
    fun permissionIsRequestedOnlyAfterExplicitControllerAction() {
        val capture = FakeCapture()
        val controller = VoiceCaptureController(capture, initialPermissionGranted = false)

        assertEquals(VoiceLifecycleState.Idle, controller.state.value)
        assertTrue(controller.beginPermissionRequest())
        assertEquals(VoiceLifecycleState.RequestingPermission, controller.state.value)

        controller.onPermissionResult(granted = true, permanentlyDenied = false)
        assertEquals(VoiceLifecycleState.Ready, controller.state.value)
        assertFalse(controller.beginPermissionRequest())
    }

    @Test
    fun holdStartsCaptureAndReleaseStopsAndDiscards() {
        val capture = FakeCapture()
        val controller = VoiceCaptureController(capture, initialPermissionGranted = true)

        controller.beginHold()
        assertEquals(1, capture.starts)
        assertEquals(VoiceLifecycleState.Recording, controller.state.value)

        controller.releaseHold()
        assertEquals(1, capture.stops)
        assertEquals(VoiceLifecycleState.Ready, controller.state.value)
    }

    @Test
    fun cancelAndHostStopBothTerminateRecording() {
        val capture = FakeCapture()
        val controller = VoiceCaptureController(capture, initialPermissionGranted = true)

        controller.beginHold()
        controller.cancel()
        assertEquals(1, capture.cancels)
        assertEquals(
            VoiceLifecycleState.Cancelled("Recording cancelled; audio discarded"),
            controller.state.value,
        )

        controller.retry()
        controller.beginHold()
        controller.onHostStopped()
        assertEquals(2, capture.cancels)
        assertEquals(
            VoiceLifecycleState.Cancelled("Recording stopped when the app left the foreground"),
            controller.state.value,
        )
    }

    @Test
    fun permissionLossCancelsCaptureAndFailsClosed() {
        val capture = FakeCapture()
        val controller = VoiceCaptureController(capture, initialPermissionGranted = true)

        controller.beginHold()
        controller.syncPermission(granted = false)

        assertEquals(1, capture.cancels)
        assertEquals(VoiceLifecycleState.PermissionPermanentlyDenied, controller.state.value)
    }

    @Test
    fun audioFocusLossAndTimeLimitAreExplicitCancellations() {
        val capture = FakeCapture()
        val controller = VoiceCaptureController(capture, initialPermissionGranted = true)

        controller.beginHold()
        capture.interrupt(CaptureInterruption.AudioFocusLost)
        assertEquals(
            VoiceLifecycleState.Cancelled("Recording stopped after audio focus was lost"),
            controller.state.value,
        )

        controller.retry()
        controller.beginHold()
        capture.interrupt(CaptureInterruption.TimeLimitReached)
        assertEquals(
            VoiceLifecycleState.Cancelled("The 30-second recording limit was reached"),
            controller.state.value,
        )
    }

    @Test
    fun startFailureAndCloseFailSafely() {
        val capture = FakeCapture(startResult = CaptureStartResult.Failed("Mic unavailable"))
        val controller = VoiceCaptureController(capture, initialPermissionGranted = true)

        controller.beginHold()
        assertEquals(VoiceLifecycleState.Error("Mic unavailable"), controller.state.value)

        controller.close()
        assertEquals(1, capture.closes)
        assertEquals(VoiceLifecycleState.Idle, controller.state.value)
    }

    @Test
    fun permissionRaceDuringStartFailsClosed() {
        val capture = FakeCapture(startResult = CaptureStartResult.PermissionMissing)
        val controller = VoiceCaptureController(capture, initialPermissionGranted = true)

        controller.beginHold()

        assertEquals(VoiceLifecycleState.PermissionPermanentlyDenied, controller.state.value)
    }

    private class FakeCapture(
        var startResult: CaptureStartResult = CaptureStartResult.Started,
    ) : LocalAudioCapture {
        var starts = 0
        var stops = 0
        var cancels = 0
        var closes = 0
        private var interruption: ((CaptureInterruption) -> Unit)? = null

        override fun start(onInterrupted: (CaptureInterruption) -> Unit): CaptureStartResult {
            starts += 1
            interruption = onInterrupted
            return startResult
        }

        override fun stop() {
            stops += 1
        }

        override fun cancel() {
            cancels += 1
        }

        override fun close() {
            closes += 1
        }

        fun interrupt(reason: CaptureInterruption) {
            interruption?.invoke(reason)
        }
    }
}
