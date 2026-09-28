package com.sashtech.mehndidesignsimple.ads

/**
 * Constants and helpers for Google AdMob integration.
 *
 * Official Google test Ad Unit IDs are used by default for safety during development.
 * When ready for production, you can set your real Ad Unit IDs remotely via Firebase
 * Realtime Database under the 'ads' node or directly in the AdConfig defaults.
 */
object AdMobConstants {
    // Official Google AdMob Test Ad Unit IDs
    const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
    const val SAMPLE_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    /**
     * Resolves a banner ad unit ID. If the configured ID is null, empty, or
     * contains placeholder text (such as "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"),
     * it safely falls back to the official Google AdMob test Banner ID.
     */
    fun sanitizeBannerAdUnitId(configuredId: String?): String {
        val trimmed = configuredId?.trim()
        if (trimmed.isNullOrEmpty() ||
            trimmed.contains("XXXXX", ignoreCase = true) ||
            !trimmed.startsWith("ca-app-pub-")
        ) {
            return BANNER_TEST_ID
        }
        return trimmed
    }

    /**
     * Resolves an interstitial ad unit ID. If the configured ID is null, empty, or
     * contains placeholder text (such as "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"),
     * it safely falls back to the official Google AdMob test Interstitial ID.
     */
    fun sanitizeInterstitialAdUnitId(configuredId: String?): String {
        val trimmed = configuredId?.trim()
        if (trimmed.isNullOrEmpty() ||
            trimmed.contains("XXXXX", ignoreCase = true) ||
            !trimmed.startsWith("ca-app-pub-")
        ) {
            return INTERSTITIAL_TEST_ID
        }
        return trimmed
    }
}
