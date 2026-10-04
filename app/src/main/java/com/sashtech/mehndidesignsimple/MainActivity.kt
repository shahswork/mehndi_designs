package com.sashtech.mehndidesignsimple

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.sashtech.mehndidesignsimple.ads.AdMobConstants
import com.sashtech.mehndidesignsimple.data.local.AppDatabase
import com.sashtech.mehndidesignsimple.data.repository.MehndiRepository
import com.sashtech.mehndidesignsimple.navigation.AppNavigation
import com.sashtech.mehndidesignsimple.notifications.OneSignalConfig
import com.sashtech.mehndidesignsimple.notifications.OneSignalNotificationManager
import com.sashtech.mehndidesignsimple.ui.theme.MehndiDesignTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val repository: MehndiRepository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        MehndiRepository(
            favoriteDao = database.favoriteDao(),
            designDao = database.designDao(),
            tutorialDao = database.tutorialDao(),
            categoryDao = database.categoryDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (_: Exception) {
            // Graceful fallback if google-services.json is not configured yet
        }
        enableEdgeToEdge()

        // Request runtime notification permission on Android 13+ (POST_NOTIFICATIONS)
        if (!AdMobConstants.isRunningOnEmulator() && OneSignalConfig.isConfigured()) {
            OneSignalNotificationManager.promptForPushPermission(fallbackToSettings = false)
        }

        setContent {
            MehndiDesignTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    repository = repository
                )
            }
        }
    }
}
