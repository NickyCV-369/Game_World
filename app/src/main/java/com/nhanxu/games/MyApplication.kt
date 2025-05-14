package com.nhanxu.games

import android.app.Application

class MyApplication : Application() {

    lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        appOpenAdManager = AppOpenAdManager(this, "ca-app-pub-9218673410350376/4957949643")
    }
}
