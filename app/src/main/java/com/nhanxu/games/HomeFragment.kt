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

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var webView: WebView
    private var isInIframe = false
    private lateinit var callback: OnBackPressedCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webView)

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

        webView.loadUrl("https://nhanxu.com/app/games")
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
                }
            }
        }, "androidBridge")
    }

    private fun showImmersiveMode() {
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
    }
}