package com.sashtech.mehndidesignsimple.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sashtech.mehndidesignsimple.data.model.AdsConfig
import com.sashtech.mehndidesignsimple.data.model.BannerAdConfig
import com.sashtech.mehndidesignsimple.data.model.InterstitialAdConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages AdMob lifecycle, Firebase Realtime Database remote configuration,
 * and Interstitial preloading / presentation frequency.
 */
class AdManager private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "AdMobManager"
        private const val NODE_ADS = "ads"

        @Volatile
        private var INSTANCE: AdManager? = null

        fun getInstance(context: Context): AdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _adsConfig = MutableStateFlow(AdsConfig())
    val adsConfig: StateFlow<AdsConfig> = _adsConfig.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private val isInterstitialLoading = AtomicBoolean(false)
    private val actionCounter = AtomicInteger(0)

    init {
        listenToRemoteAdConfig()
    }

    /**
     * Attaches a real-time listener to Firebase Realtime Database 'ads' node.
     */
    private fun listenToRemoteAdConfig() {
        if (AdMobConstants.isRunningOnEmulator()) {
            Log.d(TAG, "Running on emulator: disabling AdMob to avoid Mesa & AdServices system failures")
            _adsConfig.value = AdsConfig(
                enabled = false,
                banner = BannerAdConfig(enabled = false),
                interstitial = InterstitialAdConfig(enabled = false)
            )
            return
        }

        try {
            val db = FirebaseDatabase.getInstance()
            val adsRef = db.getReference(NODE_ADS)

            adsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "Ad configuration unavailable on Firebase; applying defaults (enabled=true)")
                        preloadInterstitial(appContext)
                        return
                    }

                    try {
                        val allEnabled = snapshot.child("enabled").getValue(Boolean::class.java) ?: true

                        // Banner config
                        val bannerSnapshot = snapshot.child("banner")
                        val bannerEnabled = bannerSnapshot.child("enabled").getValue(Boolean::class.java) ?: true
                        val bannerAdUnitIdRaw = bannerSnapshot.child("adUnitId").getValue(String::class.java)
                        val bannerAdUnitId = AdMobConstants.sanitizeBannerAdUnitId(bannerAdUnitIdRaw)

                        // Interstitial config
                        val interstitialSnapshot = snapshot.child("interstitial")
                        val interstitialEnabled = interstitialSnapshot.child("enabled").getValue(Boolean::class.java) ?: true
                        val interstitialAdUnitIdRaw = interstitialSnapshot.child("adUnitId").getValue(String::class.java)
                        val interstitialAdUnitId = AdMobConstants.sanitizeInterstitialAdUnitId(interstitialAdUnitIdRaw)
                        val showAfterClicks = interstitialSnapshot.child("showAfterClicks").getValue(Int::class.java)
                            ?: interstitialSnapshot.child("frequency").getValue(Int::class.java)
                            ?: 3

                        val newConfig = AdsConfig(
                            enabled = allEnabled,
                            banner = BannerAdConfig(
                                enabled = bannerEnabled,
                                adUnitId = bannerAdUnitId
                            ),
                            interstitial = InterstitialAdConfig(
                                enabled = interstitialEnabled,
                                adUnitId = interstitialAdUnitId,
                                showAfterClicks = if (showAfterClicks > 0) showAfterClicks else 3
                            )
                        )

                        _adsConfig.value = newConfig
                        Log.d(TAG, "Firebase ad configuration loaded: overall_enabled=$allEnabled, " +
                                "banner_enabled=$bannerEnabled, interstitial_enabled=$interstitialEnabled, " +
                                "showAfterClicks=${newConfig.interstitial.showAfterClicks}")
                        Log.d(TAG, "Banner enabled/disabled: $bannerEnabled")
                        Log.d(TAG, "Interstitial enabled/disabled: $interstitialEnabled")

                        if (allEnabled && interstitialEnabled) {
                            preloadInterstitial(appContext)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing remote ad configuration: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Firebase ad configuration listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Firebase database instance unavailable for Ads: ${e.message}")
            preloadInterstitial(appContext)
        }
    }

    /**
     * Preloads an AdMob Interstitial Ad in background.
     */
    fun preloadInterstitial(context: Context) {
        if (AdMobConstants.isRunningOnEmulator()) {
            return
        }

        val config = _adsConfig.value
        if (!config.enabled || !config.interstitial.enabled) {
            Log.d(TAG, "Interstitial ads disabled in configuration; skipping preload")
            return
        }

        if (interstitialAd != null) {
            Log.d(TAG, "Interstitial ad already preloaded and ready")
            return
        }

        if (isInterstitialLoading.getAndSet(true)) {
            Log.d(TAG, "Interstitial ad currently loading...")
            return
        }

        val adUnitId = config.interstitial.adUnitId
        Log.d(TAG, "Preloading Interstitial Ad with unit ID: $adUnitId")

        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        isInterstitialLoading.set(false)
                        interstitialAd = ad
                        Log.d(TAG, "Interstitial loaded successfully")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isInterstitialLoading.set(false)
                        interstitialAd = null
                        Log.w(TAG, "Interstitial failed to load: code=${loadAdError.code}, message=${loadAdError.message}")
                    }
                }
            )
        } catch (e: Exception) {
            isInterstitialLoading.set(false)
            Log.w(TAG, "Exception while initiating interstitial preload: ${e.message}")
        }
    }

    /**
     * Called when a user performs a relevant design opening action.
     * Increments the action counter and displays an interstitial ad if the configured
     * frequency (showAfterClicks) is reached.
     *
     * In all failure/skip scenarios, [onActionComplete] is invoked immediately so user
     * navigation is never blocked.
     */
    fun onDesignAction(activity: Activity?, onActionComplete: () -> Unit) {
        val config = _adsConfig.value

        // If ads or interstitials are globally or individually disabled, proceed directly
        if (!config.enabled || !config.interstitial.enabled || activity == null || activity.isFinishing || activity.isDestroyed) {
            onActionComplete()
            return
        }

        val currentCount = actionCounter.incrementAndGet()
        val threshold = config.interstitial.showAfterClicks
        Log.d(TAG, "Design action registered: currentCount=$currentCount / threshold=$threshold")

        if (currentCount >= threshold) {
            val ad = interstitialAd
            if (ad != null) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Interstitial dismissed by user")
                        interstitialAd = null
                        actionCounter.set(0)
                        preloadInterstitial(activity.applicationContext)
                        onActionComplete()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                        interstitialAd = null
                        actionCounter.set(0)
                        preloadInterstitial(activity.applicationContext)
                        onActionComplete()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial shown on screen")
                        interstitialAd = null
                    }
                }

                try {
                    ad.show(activity)
                } catch (e: Exception) {
                    Log.w(TAG, "Exception showing interstitial ad: ${e.message}")
                    actionCounter.set(0)
                    interstitialAd = null
                    preloadInterstitial(activity.applicationContext)
                    onActionComplete()
                }
            } else {
                Log.d(TAG, "Interstitial reached threshold ($threshold) but ad not yet ready. Proceeding smoothly.")
                // Attempt preload if missed
                preloadInterstitial(activity.applicationContext)
                onActionComplete()
            }
        } else {
            // Not yet reached frequency threshold
            onActionComplete()
        }
    }
}
