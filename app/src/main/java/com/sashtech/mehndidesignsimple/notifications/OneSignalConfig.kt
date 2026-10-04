package com.sashtech.mehndidesignsimple.notifications

import com.sashtech.mehndidesignsimple.BuildConfig

/**
 * Configuration for OneSignal Push Notifications.
 *
 * You can set your OneSignal App ID in either:
 * 1. The root `.env` file:
 *    ONESIGNAL_APP_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
 * 2. Or directly replace [ONESIGNAL_APP_ID_FALLBACK] below.
 */
object OneSignalConfig {

    /**
     * REPLACE THIS PLACEHOLDER with your actual OneSignal App ID from:
     * OneSignal Dashboard -> Your App -> Settings -> Keys & IDs -> OneSignal App ID
     */
    const val ONESIGNAL_APP_ID_FALLBACK = "YOUR_ONESIGNAL_APP_ID_HERE"

    /**
     * Resolves the active OneSignal App ID, checking BuildConfig (.env) first,
     * then falling back to [ONESIGNAL_APP_ID_FALLBACK].
     */
    fun getAppId(): String {
        val fromBuildConfig = try {
            BuildConfig.ONESIGNAL_APP_ID.trim()
        } catch (_: Throwable) {
            ""
        }

        return when {
            fromBuildConfig.isNotBlank() && !fromBuildConfig.contains("YOUR_ONESIGNAL", ignoreCase = true) -> fromBuildConfig
            ONESIGNAL_APP_ID_FALLBACK.isNotBlank() && !ONESIGNAL_APP_ID_FALLBACK.contains("YOUR_ONESIGNAL", ignoreCase = true) -> ONESIGNAL_APP_ID_FALLBACK.trim()
            else -> ""
        }
    }

    /**
     * Checks if a valid non-placeholder App ID has been provided.
     */
    fun isConfigured(): Boolean {
        return getAppId().isNotBlank()
    }
}
