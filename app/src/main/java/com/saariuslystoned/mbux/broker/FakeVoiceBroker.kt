package com.saariuslystoned.mbux.broker

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** In-process demo only. It performs no audio capture, persistence, or network access. */
class FakeVoiceBroker(
    private val processingDelayMillis: Long = 650,
    private val playbackDelayMillis: Long = 1_100,
) : VoiceBroker {
    private val cancelledRequestIds = ConcurrentHashMap.newKeySet<String>()

    override fun responseEvents(request: VoiceRequest): Flow<VoiceBrokerEvent> = flow {
        delay(processingDelayMillis)
        if (request.id in cancelledRequestIds) return@flow

        emit(VoiceBrokerEvent.PlaybackStarted(request.id))

        delay(playbackDelayMillis)
        if (request.id in cancelledRequestIds) return@flow

        emit(VoiceBrokerEvent.PlaybackFinished(request.id))
    }

    override suspend fun cancel(requestId: String) {
        cancelledRequestIds += requestId
    }
}
