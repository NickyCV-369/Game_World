package com.nhanxu.games

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.fragment.app.Fragment
import android.annotation.SuppressLint
import androidx.core.net.toUri
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback

class AllGamesFragment : Fragment(R.layout.fragment_all_games){
    private lateinit var webView: WebView
    private var isInIframe = false
    private lateinit var callback: OnBackPressedCallback
    private lateinit var nativeAdView: NativeAdView
    private var nativeAd: NativeAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialAdLoading = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webView)
        nativeAdView = view.findViewById(R.id.native_ad_view)

        loadNativeAd()
        loadInterstitialAd()

        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isInIframe) {
                    showExitGame()
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    showExitConfirmation()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        initWebView()
    }

    private fun loadNativeAd() {
        val adLoader = AdLoader.Builder(requireContext(), "ca-app-pub-9218673410350376/7411355612")
            .forNativeAd { ad: NativeAd ->

                nativeAd?.destroy()
                nativeAd = ad

                bindNativeAdToView(ad, nativeAdView)

                nativeAdView.visibility = View.VISIBLE
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    nativeAdView.visibility = View.GONE
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun loadInterstitialAd() {
        if (isInterstitialAdLoading || interstitialAd != null) return

        isInterstitialAdLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            requireContext(),
            "ca-app-pub-9218673410350376/9212597845",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialAdLoading = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialAdLoading = false
                }
            }
        )
    }

    private fun showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                }
            }
            interstitialAd?.show(requireActivity())
        } else {
            loadInterstitialAd()
        }
    }

    private fun bindNativeAdToView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
        nativeAdView.removeAllViews() // Xoá view cũ nếu có

        val context = nativeAdView.context

        // Container LinearLayout cho nội dung native ad
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Headline TextView
        val headlineView = TextView(context).apply {
            text = nativeAd.headline
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(headlineView)
        nativeAdView.headlineView = headlineView

        // Body TextView (nếu có)
        nativeAd.body?.let {
            val bodyView = TextView(context).apply {
                text = it
                textSize = 10f
                setTextColor(Color.DKGRAY)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 3 }
            }
            container.addView(bodyView)
            nativeAdView.bodyView = bodyView
        }

        // Call to action Button (nếu có)
        nativeAd.callToAction?.let {
            val ctaButton = Button(context).apply {
                text = it
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 6 }
            }
            container.addView(ctaButton)
            nativeAdView.callToActionView = ctaButton
        }

        // Icon ImageView (nếu có)
        nativeAd.icon?.let { icon ->
            val iconView = ImageView(context).apply {
                setImageDrawable(icon.drawable)
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    gravity = Gravity.START
                    topMargin = 6
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            container.addView(iconView, 0) // Đặt lên đầu
            nativeAdView.iconView = iconView
        }

        // Thêm container vào nativeAdView
        nativeAdView.addView(container)

        // Gán nativeAd cho nativeAdView để SDK xử lý
        nativeAdView.setNativeAd(nativeAd)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(false)
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        addJavascriptBridge()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                view?.evaluateJavascript(
                    """
        (function() {
            var iframes = document.getElementsByTagName('iframe');
            if (iframes.length === 0) {
                window.androidBridge.iframeRemoved();  
            } else {
                window.androidBridge.iframeDetected(); 
            }
        })();
        """.trimIndent(), null
                )
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                if (url.contains("gamepix.com") || url.contains("gamemonetize.com") || url.contains("y8.com") || url.contains("kidsgame.io") || url.contains("hyhygames.com") || url.contains("vodogame.com")) {
                    Toast.makeText(requireContext(), "Have fun playing the game!!!", Toast.LENGTH_SHORT).show()
                    return true
                }

                if (!url.contains("nhanxu.com")) {
                    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    return true
                }

                view?.loadUrl(url)
                return true
            }
        }

        webView.loadUrl("https://nhanxu.com/app/all-games")
    }

    @SuppressLint("JavascriptInterface")
    private fun addJavascriptBridge() {
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun iframeDetected() {
                activity?.runOnUiThread {
                    showImmersiveMode()
                    isInIframe = true
                }
            }

            @JavascriptInterface
            fun iframeRemoved() {
                activity?.runOnUiThread {
                    showNormalMode()
                    isInIframe = false
                    showInterstitialAd()
                }
            }
        }, "androidBridge")
    }

    private fun showImmersiveMode() {
        nativeAdView.visibility = View.GONE
        (activity as? MainActivity)?.hideBottomNav()
        (activity as? MainActivity)?.hideAdView()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 trở lên (API level 30)
            val insetsController = requireActivity().window.insetsController
            insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            insetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            requireActivity().window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }

    private fun showNormalMode() {
        nativeAdView.visibility = View.VISIBLE
        (activity as? MainActivity)?.showBottomNav()
        (activity as? MainActivity)?.showAdView()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = requireActivity().window.insetsController
            insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun showExitGame() {
        AlertDialog.Builder(requireContext())
            .setTitle("Exit Game")
            .setMessage("Do you want to exit the game?")
            .setPositiveButton("Yes") { _, _ -> webView.goBack() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ -> requireActivity().finish() }
            .setNegativeButton("No", null)
            .show()
    }

    fun clearWebViewData() {
        webView.clearCache(true)
        webView.clearHistory()

        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        WebStorage.getInstance().deleteAllData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        callback.remove()
        webView.onPause()
        webView.destroy()
        nativeAd?.destroy()
        interstitialAd?.fullScreenContentCallback = null
        interstitialAd = null
    }
}