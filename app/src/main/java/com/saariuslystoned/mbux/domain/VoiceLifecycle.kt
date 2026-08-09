package com.saariuslystoned.mbux.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface VoiceLifecycleState {
    data object Idle : VoiceLifecycleState

    data object RequestingPermission : VoiceLifecycleState

    data object PermissionDenied : VoiceLifecycleState

    data object PermissionPermanentlyDenied : VoiceLifecycleState

    data object Ready : VoiceLifecycleState

    data object Recording : VoiceLifecycleState

    /** Reserved for a separately approved future broker slice. */
    data class Sending(val requestId: String) : VoiceLifecycleState

    /** Reserved for a separately approved future playback slice. */
    data class Playing(val requestId: String) : VoiceLifecycleState

    data class Cancelled(val reason: String) : VoiceLifecycleState

    data class Error(val userMessage: String) : VoiceLifecycleState
}

sealed interface VoiceLifecycleEvent {
    data object BeginSetup : VoiceLifecycleEvent

    data object PermissionGranted : VoiceLifecycleEvent

    data class PermissionDenied(val permanently: Boolean) : VoiceLifecycleEvent

    data object PermissionRevoked : VoiceLifecycleEvent

    data object HoldStarted : VoiceLifecycleEvent

    data object CaptureFinished : VoiceLifecycleEvent

    data class CaptureFailed(val userMessage: String) : VoiceLifecycleEvent

    /** Reserved for a separately approved future broker slice. */
    data class HoldReleased(val requestId: String) : VoiceLifecycleEvent

    /** Reserved for a separately approved future broker slice. */
    data class PlaybackStarted(val requestId: String) : VoiceLifecycleEvent

    /** Reserved for a separately approved future playback slice. */
    data class PlaybackFinished(val requestId: String) : VoiceLifecycleEvent

    /** Reserved for a separately approved future broker slice. */
    data class BrokerFailed(val requestId: String, val userMessage: String) : VoiceLifecycleEvent

    data class Cancel(val reason: String) : VoiceLifecycleEvent

    data object Retry : VoiceLifecycleEvent

    data object ResetPermissionFlow : VoiceLifecycleEvent

    data object Teardown : VoiceLifecycleEvent
}

/**
 * Pure voice transition function. The current UI uses only permission, ready, recording,
 * cancellation, and error transitions; send/play transitions remain inert future boundaries.
 */
fun reduceVoiceLifecycle(
    state: VoiceLifecycleState,
    event: VoiceLifecycleEvent,
): VoiceLifecycleState = when (event) {
    VoiceLifecycleEvent.BeginSetup -> when (state) {
        VoiceLifecycleState.Idle,
        VoiceLifecycleState.PermissionDenied,
        -> VoiceLifecycleState.RequestingPermission

        else -> state
    }

    VoiceLifecycleEvent.PermissionGranted -> when (state) {
        VoiceLifecycleState.Idle,
        VoiceLifecycleState.RequestingPermission,
        VoiceLifecycleState.PermissionDenied,
        VoiceLifecycleState.PermissionPermanentlyDenied,
        -> VoiceLifecycleState.Ready

        else -> state
    }

    is VoiceLifecycleEvent.PermissionDenied ->
        if (state is VoiceLifecycleState.RequestingPermission) {
            if (event.permanently) {
                VoiceLifecycleState.PermissionPermanentlyDenied
            } else {
                VoiceLifecycleState.PermissionDenied
            }
        } else {
            state
        }

    VoiceLifecycleEvent.PermissionRevoked -> VoiceLifecycleState.PermissionPermanentlyDenied

    VoiceLifecycleEvent.HoldStarted ->
        if (state is VoiceLifecycleState.Ready) VoiceLifecycleState.Recording else state

    VoiceLifecycleEvent.CaptureFinished ->
        if (state is VoiceLifecycleState.Recording) VoiceLifecycleState.Ready else state

    is VoiceLifecycleEvent.CaptureFailed ->
        if (state is VoiceLifecycleState.Recording) {
            VoiceLifecycleState.Error(event.userMessage)
        } else {
            state
        }

    is VoiceLifecycleEvent.HoldReleased ->
        if (state is VoiceLifecycleState.Recording) {
            VoiceLifecycleState.Sending(event.requestId)
        } else {
            state
        }

    is VoiceLifecycleEvent.PlaybackStarted ->
        if (state is VoiceLifecycleState.Sending && state.requestId == event.requestId) {
            VoiceLifecycleState.Playing(event.requestId)
        } else {
            state
        }

    is VoiceLifecycleEvent.PlaybackFinished ->
        if (state is VoiceLifecycleState.Playing && state.requestId == event.requestId) {
            VoiceLifecycleState.Ready
        } else {
            state
        }

    is VoiceLifecycleEvent.BrokerFailed ->
        if (state.requestIdOrNull() == event.requestId) {
            VoiceLifecycleState.Error(event.userMessage)
        } else {
            state
        }

    is VoiceLifecycleEvent.Cancel -> when (state) {
        VoiceLifecycleState.RequestingPermission,
        VoiceLifecycleState.Recording,
        is VoiceLifecycleState.Sending,
        is VoiceLifecycleState.Playing,
        -> VoiceLifecycleState.Cancelled(event.reason)

        else -> state
    }

    VoiceLifecycleEvent.Retry -> when (state) {
        is VoiceLifecycleState.Cancelled,
        is VoiceLifecycleState.Error,
        -> VoiceLifecycleState.Ready

        else -> state
    }

    VoiceLifecycleEvent.ResetPermissionFlow -> when (state) {
        is VoiceLifecycleState.Cancelled,
        is VoiceLifecycleState.Error,
        -> VoiceLifecycleState.Idle

        else -> state
    }

    VoiceLifecycleEvent.Teardown -> VoiceLifecycleState.Idle
}

private fun VoiceLifecycleState.requestIdOrNull(): String? = when (this) {
    is VoiceLifecycleState.Sending -> requestId
    is VoiceLifecycleState.Playing -> requestId
    else -> null
}

class VoiceLifecycleMachine(initialState: VoiceLifecycleState = VoiceLifecycleState.Idle) {
    private val mutableState = MutableStateFlow(initialState)

    val state: StateFlow<VoiceLifecycleState> = mutableState.asStateFlow()

    fun handle(event: VoiceLifecycleEvent) {
        mutableState.update { current -> reduceVoiceLifecycle(current, event) }
    }
}
