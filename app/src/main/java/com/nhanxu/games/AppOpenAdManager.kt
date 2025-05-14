package com.nhanxu.games

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.FullScreenContentCallback

class AppOpenAdManager(private val application: Application, private val adUnitId: String) : DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var currentActivity: Activity? = null

    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            currentActivity = activity
        }
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            if (activity == currentActivity) {
                currentActivity = null
            }
        }
    }

    init {
        // Lắng nghe lifecycle của app
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // Đăng ký callback activity lifecycle
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        loadAd() // Bắt đầu load quảng cáo
    }

    // Load quảng cáo App Open — **bỏ param orientation**
    private fun loadAd() {
        if (appOpenAd != null) return // Đã load rồi

        val request = AdRequest.Builder().build()
        AppOpenAd.load(application, adUnitId, request, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
            }

            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                appOpenAd = null
            }
        })
    }

    // Khi app vào foreground (gọi tự động)
    override fun onStart(owner: LifecycleOwner) {
        showAdIfAvailable()
    }

    // Hiển thị quảng cáo nếu có và chưa đang show
    fun showAdIfAvailable() {
        if (isShowingAd) return
        if (appOpenAd == null) {
            loadAd()
            return
        }
        if (currentActivity == null) return

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowingAd = false
                appOpenAd = null
                loadAd() // Load lại quảng cáo mới
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                isShowingAd = false
                appOpenAd = null
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }
        appOpenAd?.show(currentActivity!!)
    }
}