package com.sashtech.mehndidesignsimple.viewmodel

/**
 * Represents the distinct states during app initialization & startup flow.
 */
sealed interface StartupState {
    /**
     * App is actively checking local cache, establishing connection, or loading Firebase data.
     */
    data class Loading(val message: String = "Loading inspiration...") : StartupState

    /**
     * Firebase data has been loaded and Home screen content is fully prepared.
     */
    data object HomeReady : StartupState

    /**
     * Device is offline (or remote timed out), but valid cached data exists locally.
     */
    data object OfflineCached : StartupState

    /**
     * First app launch with no internet and no local cache available.
     * Requires the user to connect to the internet and retry.
     */
    data class NoInternetNoCache(
        val message: String = "Connect to the internet to load the latest designs."
    ) : StartupState
}
