package com.nhanxu.games

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.fragment.app.Fragment
import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import com.google.android.material.appbar.MaterialToolbar
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog

interface WebViewListener {
    fun onUrlChanged(url: String)
}

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var webView: WebView
    private lateinit var topAppBar: MaterialToolbar
    private var isInIframe = false

    private var webViewListener: WebViewListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is WebViewListener) {
            webViewListener = context
        } else {
            throw RuntimeException("$context must implement WebViewListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        topAppBar = requireActivity().findViewById(R.id.topAppBar)
        webView = view.findViewById(R.id.webView)

        val callback = object : OnBackPressedCallback(true) {
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

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_notifications -> {
                    showWelcomeDialog()
                    true
                }
                else -> false
            }
        }

        initWebView()
    }

    private fun showWelcomeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Welcome to Game World - Instant Games!")
            .setMessage("""
            There are many games for you to choose from and play immediately without wasting time on installation!
            All your progress will be saved, and you can continue playing later!

            If you feel that there are too few games or the games are not exciting enough and you need more, don't hesitate to contact us in the Settings!

            Sometimes ads may be uncomfortable, but please support us!
            Feel free to share your feedback so we can improve!
        """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
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

                url?.let {
                    webViewListener?.onUrlChanged(it)
                }

                view?.evaluateJavascript(
                    """
        (function() {
            var iframes = document.getElementsByTagName('iframe');
            if (iframes.length === 0) {
                window.androidBridge.iframeRemoved();  
            } else {
                for (var i = 0; i < iframes.length; i++) {
                    var src = iframes[i].src;
                    if (src.includes('html5.gamemonetize.co')) {
                        window.androidBridge.iframeDetected(); 
                        break; 
                    }
                }
            }
        })();
        """.trimIndent(), null
                )
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

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
        topAppBar.visibility = View.GONE
        (activity as? MainActivity)?.hideBottomNav()

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
        topAppBar.visibility = View.VISIBLE
        (activity as? MainActivity)?.showBottomNav()

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
}