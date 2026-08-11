package com.saariuslystoned.mbux.broker.status

import com.saariuslystoned.mbux.domain.status.SessionStatusFeedState
import org.junit.Assert.assertEquals
import org.junit.Test

class UnavailableSessionStatusFeedTest {
    @Test
    fun `default feed fails closed without fixture or optimistic status`() {
        assertEquals(
            SessionStatusFeedState.NotEnrolled,
            UnavailableSessionStatusFeed.state.value,
        )
    }
}
