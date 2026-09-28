package com.sashtech.mehndidesignsimple

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MehndiApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            // Enable Firebase Realtime Database offline persistence
            val db = FirebaseDatabase.getInstance()
            try {
                db.setPersistenceEnabled(true)
            } catch (_: Exception) {
                // Persistence can only be configured once before any DB queries
            }
        } catch (_: Exception) {
            // Graceful fallback if Firebase configuration is pending
        }

        // Initialize Google Mobile Ads SDK safely in background
        try {
            MobileAds.initialize(this) { status ->
                android.util.Log.d("MehndiApplication", "Google MobileAds initialized: $status")
            }
        } catch (e: Exception) {
            android.util.Log.w("MehndiApplication", "Google MobileAds init exception: ${e.message}")
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .dispatcher(Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 16
            })
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .allowRgb565(false)
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("mehndi_image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(150)
            .build()
    }
}
