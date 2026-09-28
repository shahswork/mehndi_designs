package com.sashtech.mehndidesignsimple.data.model

import com.sashtech.mehndidesignsimple.ads.AdMobConstants

data class BannerAdConfig(
    val enabled: Boolean = true,
    val adUnitId: String = AdMobConstants.BANNER_TEST_ID
)

data class InterstitialAdConfig(
    val enabled: Boolean = true,
    val adUnitId: String = AdMobConstants.INTERSTITIAL_TEST_ID,
    val showAfterClicks: Int = 3
)

data class AdsConfig(
    val enabled: Boolean = true,
    val banner: BannerAdConfig = BannerAdConfig(),
    val interstitial: InterstitialAdConfig = InterstitialAdConfig()
)
