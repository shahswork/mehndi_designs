package com.sashtech.mehndidesignsimple.ads

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "AdMobBannerView"

/**
 * Adaptive AdMob Banner Composable.
 * Automatically checks Firebase remote configuration.
 * When disabled or failing to load, it cleanly collapses its size to 0dp without blank space.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adManager: AdManager = AdManager.getInstance(LocalContext.current)
) {
    val isInspection = LocalInspectionMode.current
    if (isInspection) return

    val context = LocalContext.current
    val adsConfig by adManager.adsConfig.collectAsState()

    if (!adsConfig.enabled || !adsConfig.banner.enabled) {
        // Ads or Banner specifically disabled remotely via Firebase
        return
    }

    val rawAdUnitId = adsConfig.banner.adUnitId
    val adUnitId = remember(rawAdUnitId) {
        AdMobConstants.sanitizeBannerAdUnitId(rawAdUnitId)
    }
    if (adUnitId.isBlank()) return

    var isAdLoaded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isAdLoaded) {
                    Modifier
                        .wrapContentHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 2.dp)
                } else {
                    Modifier.height(0.dp)
                }
            )
            .testTag("banner_ad_container"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.wrapContentHeight(),
            factory = { ctx ->
                AdView(ctx).apply {
                    val displayMetrics = ctx.resources.displayMetrics
                    val density = displayMetrics.density
                    val widthDp = (displayMetrics.widthPixels / density).toInt().coerceAtLeast(320)
                    val adSize = try {
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp)
                    } catch (_: Exception) {
                        AdSize.BANNER
                    }
                    setAdSize(adSize)
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                            Log.d(TAG, "AdMob Banner loaded successfully for $adUnitId")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                            Log.w(TAG, "AdMob Banner failed to load: code=${error.code}, message=${error.message}")
                        }
                    }
                    try {
                        loadAd(AdRequest.Builder().build())
                    } catch (e: Exception) {
                        Log.w(TAG, "Exception loading banner ad: ${e.message}")
                    }
                }
            },
            update = { adView ->
                if (adView.adUnitId != adUnitId) {
                    adView.adUnitId = adUnitId
                    try {
                        adView.loadAd(AdRequest.Builder().build())
                    } catch (_: Exception) {}
                }
            }
        )
    }
}

