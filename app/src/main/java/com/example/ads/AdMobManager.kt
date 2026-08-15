package com.example.ads

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdMobConfig(
    val appId: String = "ca-app-pub-3940256099942544~3347511713",
    val bannerAdUnitId: String = "ca-app-pub-3940256099942544/6300978111",
    val interstitialAdUnitId: String = "ca-app-pub-3940256099942544/1033173712",
    val rewardedAdUnitId: String = "ca-app-pub-3940256099942544/5224354917",
    val isTestMode: Boolean = true
)

object AdMobManager {
    private const val PREFS_NAME = "admob_config_prefs"
    private const val KEY_APP_ID = "admob_app_id"
    private const val KEY_BANNER_ID = "admob_banner_id"
    private const val KEY_INTERSTITIAL_ID = "admob_interstitial_id"
    private const val KEY_REWARDED_ID = "admob_rewarded_id"
    private const val KEY_TEST_MODE = "admob_test_mode"

    private var actionCount = 0
    private const val INTERSTITIAL_INTERVAL = 3 // Show every 3 actions for reliable test ad display

    private val _config = MutableStateFlow(AdMobConfig())
    val config: StateFlow<AdMobConfig> = _config.asStateFlow()

    private val _adDialogVisible = MutableStateFlow<String?>(null)
    val adDialogVisible: StateFlow<String?> = _adDialogVisible.asStateFlow()

    private val _rewardGranted = MutableStateFlow<Boolean?>(null)
    val rewardGranted: StateFlow<Boolean?> = _rewardGranted.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loadedConfig = AdMobConfig(
            appId = prefs.getString(KEY_APP_ID, "ca-app-pub-3940256099942544~3347511713") ?: "ca-app-pub-3940256099942544~3347511713",
            bannerAdUnitId = prefs.getString(KEY_BANNER_ID, "ca-app-pub-3940256099942544/6300978111") ?: "ca-app-pub-3940256099942544/6300978111",
            interstitialAdUnitId = prefs.getString(KEY_INTERSTITIAL_ID, "ca-app-pub-3940256099942544/1033173712") ?: "ca-app-pub-3940256099942544/1033173712",
            rewardedAdUnitId = prefs.getString(KEY_REWARDED_ID, "ca-app-pub-3940256099942544/5224354917") ?: "ca-app-pub-3940256099942544/5224354917",
            isTestMode = prefs.getBoolean(KEY_TEST_MODE, true)
        )
        _config.value = loadedConfig
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
    }

    /**
     * Records a user action. If interval threshold is reached, triggers simulated interstitial ad.
     */
    fun checkAndShowInterstitial(onAdClosed: () -> Unit = {}): Boolean {
        actionCount++
        if (actionCount >= INTERSTITIAL_INTERVAL) {
            actionCount = 0
            _adDialogVisible.value = "INTERSTITIAL"
            return true
        }
        return false
    }

    /**
     * Instantly shows a test interstitial ad on demand.
     */
    fun showTestInterstitial() {
        _adDialogVisible.value = "INTERSTITIAL"
    }

    /**
     * Triggers a rewarded ad for special actions (e.g. Save All or Cleaner)
     */
    fun showRewardedAd(reason: String, onRewardGranted: () -> Unit = {}) {
        _adDialogVisible.value = "REWARDED:$reason"
    }

    /**
     * Instantly shows a test rewarded ad on demand.
     */
    fun showTestRewarded(reason: String = "Test Bonus Feature") {
        _adDialogVisible.value = "REWARDED:$reason"
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
