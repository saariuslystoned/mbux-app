package com.saariuslystoned.mbux.broker

import kotlinx.coroutines.flow.Flow

/** Opaque local request. No audio, transcript, provider ID, or credential is modeled here. */
data class VoiceRequest(val id: String)

sealed interface VoiceBrokerEvent {
    val requestId: String

    data class PlaybackStarted(override val requestId: String) : VoiceBrokerEvent

    data class PlaybackFinished(override val requestId: String) : VoiceBrokerEvent
}

/** Boundary that a future authenticated broker client can implement after separate review. */
interface VoiceBroker {
    fun responseEvents(request: VoiceRequest): Flow<VoiceBrokerEvent>

    suspend fun cancel(requestId: String)
}
