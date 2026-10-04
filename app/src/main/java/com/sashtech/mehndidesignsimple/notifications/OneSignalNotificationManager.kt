package com.sashtech.mehndidesignsimple.notifications

import android.content.Context
import android.util.Log
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.notifications.INotificationWillDisplayEvent
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import com.sashtech.mehndidesignsimple.ads.AdMobConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles OneSignal SDK initialization, push notification permissions,
 * foreground/background event dispatching, and user identification linking.
 */
object OneSignalNotificationManager {

    private const val TAG = "OneSignalManager"

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _lastClickedNotificationData = MutableStateFlow<Map<String, Any?>?>(null)
    val lastClickedNotificationData: StateFlow<Map<String, Any?>?> = _lastClickedNotificationData.asStateFlow()

    /**
     * Initializes the OneSignal SDK with the resolved App ID.
     * Safe to invoke from Application.onCreate().
     */
    fun initialize(context: Context) {
        if (AdMobConstants.isRunningOnEmulator()) {
            Log.d(TAG, "Running on emulator: skipping OneSignal push initialization to prevent FCM Registration hard failure.")
            return
        }

        val appId = OneSignalConfig.getAppId()

        if (!OneSignalConfig.isConfigured()) {
            Log.w(
                TAG,
                "OneSignal App ID is not configured! Please provide your OneSignal App ID in .env (ONESIGNAL_APP_ID) or in OneSignalConfig.kt (ONESIGNAL_APP_ID_FALLBACK)."
            )
            return
        }

        try {
            Log.i(TAG, "Initializing OneSignal with App ID: ${appId.take(8)}...")

            // 1. Initialize OneSignal SDK with Application Context
            OneSignal.initWithContext(context.applicationContext, appId)

            // 2. Configure Notification Click Listener (Background / Foreground Click)
            OneSignal.Notifications.addClickListener(object : INotificationClickListener {
                override fun onClick(event: INotificationClickEvent) {
                    val notification = event.notification
                    val title = notification.title
                    val body = notification.body
                    val additionalData = notification.additionalData

                    Log.d(TAG, "Notification clicked: title='$title', body='$body', additionalData=$additionalData")

                    val dataMap = mutableMapOf<String, Any?>()
                    dataMap["title"] = title
                    dataMap["body"] = body
                    if (additionalData != null) {
                        val keys = additionalData.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            dataMap[key] = additionalData.opt(key)
                        }
                    }
                    _lastClickedNotificationData.value = dataMap
                }
            })

            // 3. Configure Foreground Notification Display Lifecycle Listener
            OneSignal.Notifications.addForegroundLifecycleListener(object : INotificationLifecycleListener {
                override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                    val notification = event.notification
                    Log.d(TAG, "Foreground notification received: ${notification.title}")
                    // By default, OneSignal displays foreground notifications.
                    // Call event.preventDefault() here only if you wish to suppress notifications when app is open.
                }
            })

            // 4. Observe Push Subscription state changes
            OneSignal.User.pushSubscription.addObserver(object : IPushSubscriptionObserver {
                override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                    val optedIn = state.current.optedIn
                    val pushToken = state.current.token
                    val subscriptionId = state.current.id
                    Log.d(
                        TAG,
                        "OneSignal Push Subscription changed: optedIn=$optedIn, subId=$subscriptionId, hasToken=${!pushToken.isNullOrBlank()}"
                    )
                    _isSubscribed.value = optedIn
                }
            })

            _isSubscribed.value = OneSignal.User.pushSubscription.optedIn
            Log.i(TAG, "OneSignal initialized successfully. Initial subscription optedIn=${_isSubscribed.value}")

        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing OneSignal: ${e.message}", e)
        }
    }

    /**
     * Prompts the user for notification permissions on Android 13+ (POST_NOTIFICATIONS).
     *
     * @param fallbackToSettings If true, navigates the user to system settings if previously permanently denied.
     */
    fun promptForPushPermission(fallbackToSettings: Boolean = true, onResult: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Requesting OneSignal notification permission...")
                val accepted = OneSignal.Notifications.requestPermission(fallbackToSettings)
                Log.d(TAG, "OneSignal notification permission result: accepted=$accepted")
                _isSubscribed.value = OneSignal.User.pushSubscription.optedIn
                onResult?.invoke(accepted)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to request push notification permission: ${e.message}")
                onResult?.invoke(false)
            }
        }
    }

    /**
     * Associates the logged-in Firebase User UID with OneSignal as the External ID.
     * Call this when a user logs in via Firebase Authentication.
     *
     * @param firebaseUid The Firebase user UID (e.g., user.uid), or null on logout.
     */
    fun syncFirebaseUser(firebaseUid: String?) {
        try {
            if (!firebaseUid.isNullOrBlank()) {
                Log.d(TAG, "Linking Firebase UID to OneSignal External ID: $firebaseUid")
                OneSignal.login(firebaseUid)
            } else {
                Log.d(TAG, "Logging out user from OneSignal")
                OneSignal.logout()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error syncing Firebase UID with OneSignal: ${e.message}")
        }
    }

    /**
     * Returns the OneSignal Push Subscription ID for debugging/targeting.
     */
    fun getSubscriptionId(): String? {
        return try {
            OneSignal.User.pushSubscription.id
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Clears the consumed click event data.
     */
    fun clearLastClickedNotification() {
        _lastClickedNotificationData.value = null
    }
}
