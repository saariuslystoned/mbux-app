package com.saariuslystoned.mbux.broker

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeVoiceBrokerTest {
    @Test
    fun emitsOnlyOpaqueLifecycleEvents() = runBlocking {
        val broker = FakeVoiceBroker(
            processingDelayMillis = 0,
            playbackDelayMillis = 0,
        )

        val events = broker.responseEvents(VoiceRequest("fake-request")).toList()

        assertEquals(
            listOf(
                VoiceBrokerEvent.PlaybackStarted("fake-request"),
                VoiceBrokerEvent.PlaybackFinished("fake-request"),
            ),
            events,
        )
    }

    @Test
    fun cancelledRequestEmitsNothing() = runBlocking {
        val broker = FakeVoiceBroker(
            processingDelayMillis = 0,
            playbackDelayMillis = 0,
        )
        val request = VoiceRequest("cancelled-request")

        broker.cancel(request.id)

        assertTrue(broker.responseEvents(request).toList().isEmpty())
    }
}
