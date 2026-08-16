package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdMobConfig(
    val appId: String = "ca-app-pub-8212461864193378~9176143246",
    val bannerAdUnitId: String = "ca-app-pub-8212461864193378/2175620622",
    val interstitialAdUnitId: String = "ca-app-pub-8212461864193378/2151569892",
    val rewardedAdUnitId: String = "ca-app-pub-8212461864193378/1821085763",
    val isTestMode: Boolean = false
)

object AdMobManager {
    private const val TAG = "AdMobManager"
    private const val PREFS_NAME = "admob_config_prefs"
    private const val KEY_APP_ID = "admob_app_id"
    private const val KEY_BANNER_ID = "admob_banner_id"
    private const val KEY_INTERSTITIAL_ID = "admob_interstitial_id"
    private const val KEY_REWARDED_ID = "admob_rewarded_id"
    private const val KEY_TEST_MODE = "admob_test_mode"
    private const val KEY_LAST_EXIT_AD_TIME = "admob_last_exit_ad_time"

    var currentActivity: Activity? = null

    private var actionCount = 0
    private const val INTERSTITIAL_INTERVAL = 3
    private const val EXIT_AD_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes in milliseconds

    private val _config = MutableStateFlow(AdMobConfig())
    val config: StateFlow<AdMobConfig> = _config.asStateFlow()

    private val _adDialogVisible = MutableStateFlow<String?>(null)
    val adDialogVisible: StateFlow<String?> = _adDialogVisible.asStateFlow()

    private val _rewardGranted = MutableStateFlow<Boolean?>(null)
    val rewardGranted: StateFlow<Boolean?> = _rewardGranted.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private val pendingInterstitialCallbacks = mutableListOf<(InterstitialAd) -> Unit>()

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false
    private val pendingRewardedCallbacks = mutableListOf<(RewardedAd) -> Unit>()

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loadedConfig = AdMobConfig(
            appId = prefs.getString(KEY_APP_ID, "ca-app-pub-8212461864193378~9176143246") ?: "ca-app-pub-8212461864193378~9176143246",
            bannerAdUnitId = prefs.getString(KEY_BANNER_ID, "ca-app-pub-8212461864193378/2175620622") ?: "ca-app-pub-8212461864193378/2175620622",
            interstitialAdUnitId = prefs.getString(KEY_INTERSTITIAL_ID, "ca-app-pub-8212461864193378/2151569892") ?: "ca-app-pub-8212461864193378/2151569892",
            rewardedAdUnitId = prefs.getString(KEY_REWARDED_ID, "ca-app-pub-8212461864193378/1821085763") ?: "ca-app-pub-8212461864193378/1821085763",
            isTestMode = prefs.getBoolean(KEY_TEST_MODE, false)
        )
        _config.value = loadedConfig

        try {
            val testDeviceIds = listOf(
                AdRequest.DEVICE_ID_EMULATOR
            )
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)

            MobileAds.initialize(context) { status ->
                Log.d(TAG, "Google MobileAds initialized: $status")
                loadInterstitial(context)
                loadRewarded(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Google MobileAds", e)
        }
    }

    fun loadInterstitial(context: Context, onLoaded: ((InterstitialAd) -> Unit)? = null) {
        val existing = interstitialAd
        if (existing != null) {
            onLoaded?.invoke(existing)
            return
        }
        if (onLoaded != null) {
            pendingInterstitialCallbacks.add(onLoaded)
        }
        if (isInterstitialLoading) return
        isInterstitialLoading = true

        val adUnitId = _config.value.interstitialAdUnitId.ifBlank { "ca-app-pub-8212461864193378/2151569892" }
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Google Interstitial ad loaded successfully")
                    val callbacks = pendingInterstitialCallbacks.toList()
                    pendingInterstitialCallbacks.clear()
                    callbacks.forEach { it.invoke(ad) }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "Google Interstitial ad failed to load: ${error.message}")
                    pendingInterstitialCallbacks.clear()
                }
            }
        )
    }

    fun loadRewarded(context: Context, onLoaded: ((RewardedAd) -> Unit)? = null) {
        val existing = rewardedAd
        if (existing != null) {
            onLoaded?.invoke(existing)
            return
        }
        if (onLoaded != null) {
            pendingRewardedCallbacks.add(onLoaded)
        }
        if (isRewardedLoading) return
        isRewardedLoading = true

        val adUnitId = _config.value.rewardedAdUnitId.ifBlank { "ca-app-pub-8212461864193378/1821085763" }
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    Log.d(TAG, "Google Rewarded ad loaded successfully")
                    val callbacks = pendingRewardedCallbacks.toList()
                    pendingRewardedCallbacks.clear()
                    callbacks.forEach { it.invoke(ad) }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    Log.w(TAG, "Google Rewarded ad failed to load: ${error.message}")
                    pendingRewardedCallbacks.clear()
                }
            }
        )
    }

    fun saveConfig(context: Context, newConfig: AdMobConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_APP_ID, newConfig.appId)
            putString(KEY_BANNER_ID, newConfig.bannerAdUnitId)
            putString(KEY_INTERSTITIAL_ID, newConfig.interstitialAdUnitId)
            putString(KEY_REWARDED_ID, newConfig.rewardedAdUnitId)
            putBoolean(KEY_TEST_MODE, newConfig.isTestMode)
            apply()
        }
        _config.value = newConfig

        interstitialAd = null
        rewardedAd = null
        loadInterstitial(context)
        loadRewarded(context)
    }

    /**
     * Shows an Interstitial Ad on app exit at most once every 30 minutes.
     * If 30 minutes have elapsed, displays the interstitial ad and completes on dismissal/failure.
     * If within the 30 minute cooldown, immediately proceeds with app exit.
     */
    fun showAppExitInterstitial(activity: Activity? = null, onComplete: () -> Unit) {
        val act = activity ?: currentActivity
        val ctx = appContext ?: act?.applicationContext

        val prefs = ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastExitAdTime = prefs?.getLong(KEY_LAST_EXIT_AD_TIME, 0L) ?: 0L
        val now = System.currentTimeMillis()

        // Check if 30 minutes cooldown has elapsed since the last exit ad
        if (now - lastExitAdTime >= EXIT_AD_INTERVAL_MS) {
            prefs?.edit()?.putLong(KEY_LAST_EXIT_AD_TIME, now)?.apply()
            val ad = interstitialAd
            if (ad != null && act != null && !act.isFinishing && !act.isDestroyed) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        appContext?.let { loadInterstitial(it) }
                        onComplete()
                    }
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        interstitialAd = null
                        appContext?.let { loadInterstitial(it) }
                        onComplete()
                    }
                }
                ad.show(act)
                return
            } else if (act != null && !act.isFinishing && !act.isDestroyed) {
                // Load and show directly if not cached
                loadInterstitial(act) { loadedAd ->
                    loadedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            appContext?.let { loadInterstitial(it) }
                            onComplete()
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            interstitialAd = null
                            appContext?.let { loadInterstitial(it) }
                            onComplete()
                        }
                    }
                    loadedAd.show(act)
                }
                return
            }
        }
        onComplete()
    }

    /**
     * Records a user action. If interval threshold is reached, triggers Google AdMob Interstitial Ad.
     */
    fun checkAndShowInterstitial(activity: Activity? = null, onAdClosed: () -> Unit = {}): Boolean {
        actionCount++
        if (actionCount >= INTERSTITIAL_INTERVAL) {
            actionCount = 0
            showInterstitialAd(activity, onAdClosed)
            return true
        }
        return false
    }

    fun showTestInterstitial(activity: Activity? = null, onAdClosed: () -> Unit = {}) {
        showInterstitialAd(activity, onAdClosed)
    }

    private fun showInterstitialAd(activity: Activity?, onAdClosed: () -> Unit = {}) {
        val act = activity ?: currentActivity
        val ad = interstitialAd

        if (ad != null && act != null && !act.isFinishing && !act.isDestroyed) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    appContext?.let { loadInterstitial(it) }
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    appContext?.let { loadInterstitial(it) }
                    onAdClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Google Interstitial Ad showing fullscreen")
                }
            }
            ad.show(act)
        } else if (act != null && !act.isFinishing && !act.isDestroyed) {
            // Load and show directly once ready
            loadInterstitial(act) { loadedAd ->
                loadedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        appContext?.let { loadInterstitial(it) }
                        onAdClosed()
                    }
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        interstitialAd = null
                        appContext?.let { loadInterstitial(it) }
                        onAdClosed()
                    }
                }
                loadedAd.show(act)
            }
        } else {
            _adDialogVisible.value = "INTERSTITIAL"
            appContext?.let { loadInterstitial(it) }
        }
    }

    fun showRewardedAd(activity: Activity? = null, reason: String, onRewardGranted: () -> Unit = {}) {
        val act = activity ?: currentActivity
        val ad = rewardedAd

        if (ad != null && act != null && !act.isFinishing && !act.isDestroyed) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    appContext?.let { loadRewarded(it) }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    appContext?.let { loadRewarded(it) }
                }
            }
            ad.show(act) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                _rewardGranted.value = true
                onRewardGranted()
            }
        } else if (act != null && !act.isFinishing && !act.isDestroyed) {
            loadRewarded(act) { loadedAd ->
                loadedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedAd = null
                        appContext?.let { loadRewarded(it) }
                    }
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        rewardedAd = null
                        appContext?.let { loadRewarded(it) }
                    }
                }
                loadedAd.show(act) { rewardItem ->
                    _rewardGranted.value = true
                    onRewardGranted()
                }
            }
        } else {
            _adDialogVisible.value = "REWARDED:$reason"
            appContext?.let { loadRewarded(it) }
        }
    }

    fun showTestRewarded(activity: Activity? = null, reason: String = "Test Bonus Feature", onRewardGranted: () -> Unit = {}) {
        showRewardedAd(activity, reason, onRewardGranted)
    }

    fun closeAd(reward: Boolean = false) {
        _adDialogVisible.value = null
        if (reward) {
            _rewardGranted.value = true
        }
    }

    fun resetRewardState() {
        _rewardGranted.value = null
    }
}
