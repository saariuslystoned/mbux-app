package com.saariuslystoned.mbux.domain.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLaunchRouteTest {
    @Test
    fun `declared dispatch view route opens Dispatch`() {
        assertEquals(
            AppLaunchDestination.DISPATCH,
            AppLaunchRoute.resolve(
                action = AppLaunchRoute.ACTION_VIEW,
                scheme = AppLaunchRoute.DISPATCH_SCHEME,
                host = AppLaunchRoute.DISPATCH_HOST,
            ),
        )
    }

    @Test
    fun `normal launch remains on default destination`() {
        assertEquals(
            AppLaunchDestination.DEFAULT,
            AppLaunchRoute.resolve(
                action = "android.intent.action.MAIN",
                scheme = null,
                host = null,
            ),
        )
    }

    @Test
    fun `unknown view route fails closed to default destination`() {
        assertEquals(
            AppLaunchDestination.DEFAULT,
            AppLaunchRoute.resolve(
                action = AppLaunchRoute.ACTION_VIEW,
                scheme = "https",
                host = AppLaunchRoute.DISPATCH_HOST,
            ),
        )
    }
}
