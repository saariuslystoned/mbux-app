package com.saariuslystoned.mbux.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceLifecycleTest {
    @Test
    fun micOnlyHappyPathRequestsPermissionRecordsAndReturnsReady() {
        val machine = VoiceLifecycleMachine()

        machine.handle(VoiceLifecycleEvent.BeginSetup)
        assertEquals(VoiceLifecycleState.RequestingPermission, machine.state.value)
        machine.handle(VoiceLifecycleEvent.PermissionGranted)
        assertEquals(VoiceLifecycleState.Ready, machine.state.value)
        machine.handle(VoiceLifecycleEvent.HoldStarted)
        assertEquals(VoiceLifecycleState.Recording, machine.state.value)
        machine.handle(VoiceLifecycleEvent.CaptureFinished)
        assertEquals(VoiceLifecycleState.Ready, machine.state.value)
    }

    @Test
    fun denialAndPermanentDenialAreDistinctFailClosedStates() {
        val machine = VoiceLifecycleMachine()

        machine.handle(VoiceLifecycleEvent.BeginSetup)
        machine.handle(VoiceLifecycleEvent.PermissionDenied(permanently = false))
        assertEquals(VoiceLifecycleState.PermissionDenied, machine.state.value)

        machine.handle(VoiceLifecycleEvent.BeginSetup)
        machine.handle(VoiceLifecycleEvent.PermissionDenied(permanently = true))
        assertEquals(VoiceLifecycleState.PermissionPermanentlyDenied, machine.state.value)
    }

    @Test
    fun permissionRevocationFailsClosedFromRecording() {
        val machine = VoiceLifecycleMachine(VoiceLifecycleState.Ready)

        machine.handle(VoiceLifecycleEvent.HoldStarted)
        machine.handle(VoiceLifecycleEvent.PermissionRevoked)

        assertEquals(VoiceLifecycleState.PermissionPermanentlyDenied, machine.state.value)
    }

    @Test
    fun cancellationIgnoresFutureCompletionAndIsRetryable() {
        val machine = VoiceLifecycleMachine(VoiceLifecycleState.Ready)

        machine.handle(VoiceLifecycleEvent.HoldStarted)
        machine.handle(VoiceLifecycleEvent.Cancel("User stopped the capture"))
        machine.handle(VoiceLifecycleEvent.CaptureFinished)

        assertEquals(
            VoiceLifecycleState.Cancelled("User stopped the capture"),
            machine.state.value,
        )

        machine.handle(VoiceLifecycleEvent.Retry)
        assertEquals(VoiceLifecycleState.Ready, machine.state.value)
    }

    @Test
    fun teardownAlwaysReturnsToIdle() {
        val machine = VoiceLifecycleMachine(VoiceLifecycleState.Recording)

        machine.handle(VoiceLifecycleEvent.Teardown)

        assertEquals(VoiceLifecycleState.Idle, machine.state.value)
    }
}
