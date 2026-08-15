package com.example

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Bundle
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.ads.AdMobManager

/**
 * Main Application class for StatusVault.
 * Configures modern Coil ImageLoader with hardware GPU bitmaps,
 * responsive memory cache trimming, and disk cache.
 */
class StatusVaultApplication : Application(), ImageLoaderFactory {

    private var imageLoaderInstance: ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        AdMobManager.init(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                AdMobManager.currentActivity = activity
            }
            override fun onActivityPaused(activity: Activity) {
                if (AdMobManager.currentActivity === activity) {
                    AdMobManager.currentActivity = null
                }
            }
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (AdMobManager.currentActivity === activity) {
                    AdMobManager.currentActivity = null
                }
            }
        })
    }

    override fun newImageLoader(): ImageLoader {
        return imageLoaderInstance ?: ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .crossfade(true)
            .build().also {
                imageLoaderInstance = it
            }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            imageLoaderInstance?.memoryCache?.clear()
        }
    }
}
