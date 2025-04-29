package com.nhanxu.games

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var webView: WebView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webView)

        webView.webViewClient = WebViewClient()
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webView.loadUrl("https://nhanxu.com/app/games")
    }
}
