package com.saariuslystoned.mbux.domain.navigation

enum class AppLaunchDestination {
    DEFAULT,
    DISPATCH,
}

object AppLaunchRoute {
    const val ACTION_VIEW = "android.intent.action.VIEW"
    const val DISPATCH_SCHEME = "mbux"
    const val DISPATCH_HOST = "dispatch"

    fun resolve(
        action: String?,
        scheme: String?,
        host: String?,
    ): AppLaunchDestination = if (
        action == ACTION_VIEW &&
        scheme == DISPATCH_SCHEME &&
        host == DISPATCH_HOST
    ) {
        AppLaunchDestination.DISPATCH
    } else {
        AppLaunchDestination.DEFAULT
    }
}
