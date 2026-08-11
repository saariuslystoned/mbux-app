package com.saariuslystoned.mbux.broker.status

import com.saariuslystoned.mbux.domain.status.SessionStatusFeed
import com.saariuslystoned.mbux.domain.status.SessionStatusFeedState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fail-closed production default while no registered CP-1 status capability or device enrollment
 * exists. It performs no I/O and never substitutes fixture data for remote truth.
 */
object UnavailableSessionStatusFeed : SessionStatusFeed {
    private val unavailable = MutableStateFlow<SessionStatusFeedState>(
        SessionStatusFeedState.NotEnrolled,
    )

    override val state: StateFlow<SessionStatusFeedState> = unavailable.asStateFlow()
}
