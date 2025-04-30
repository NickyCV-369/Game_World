package com.nhanxu.games

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class AllGamesFragment : Fragment(R.layout.fragment_all_games) {

    private lateinit var gamesListView: ListView
    private lateinit var gameWebView: WebView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gamesListView = view.findViewById(R.id.gamesListView)
        gameWebView = view.findViewById(R.id.gameWebView)

        // Cấu hình WebView
        gameWebView.webViewClient = WebViewClient()  // Đảm bảo WebView mở trong chính ứng dụng
        val webSettings = gameWebView.settings
        webSettings.javaScriptEnabled = true // Bật JavaScript để trò chơi hoạt động
        webSettings.setSupportZoom(false) // Tắt zoom
        gameWebView.setVisibility(View.INVISIBLE) // Ẩn WebView khi chưa load xong

        loadGames()
    }

    private fun loadGames() {
        val url = "https://nhanxu.com/api/games/"  // URL của API trả về dữ liệu game

        // Gửi yêu cầu API
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                // Kiểm tra dữ liệu trả về và xử lý
                val gameNames = ArrayList<String>()
                val gameUrls = ArrayList<String>()

                for (i in 0 until response.length()) {
                    val game: JSONObject = response.getJSONObject(i)
                    gameNames.add(game.getString("name"))
                    gameUrls.add(game.getString("url"))
                }

                // Hiển thị danh sách game
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, gameNames)
                gamesListView.adapter = adapter

                // Xử lý khi người dùng chọn game
                gamesListView.setOnItemClickListener { _, _, position, _ ->
                    val gameUrl = gameUrls[position]
                    loadGameInWebView(gameUrl)  // Tải game vào WebView
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()  // Xử lý lỗi khi không lấy được dữ liệu
            })

        // Gửi yêu cầu qua Volley
        Volley.newRequestQueue(requireContext()).add(jsonArrayRequest)
    }

    private fun loadGameInWebView(gameUrl: String) {
        gameWebView.setVisibility(View.VISIBLE)  // Hiển thị WebView khi URL đã sẵn sàng
        gameWebView.loadUrl(gameUrl)  // Tải game vào WebView

        // Cấu hình WebView để trò chơi hiển thị ở chế độ toàn màn hình
        gameWebView.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Thực hiện đầy đủ màn hình cho game nếu có yêu cầu
                view?.evaluateJavascript(
                    """
                    (function() {
                        var iframe = document.querySelector('iframe');
                        if (iframe) {
                            iframe.style.width = '100%';
                            iframe.style.height = '100vh';  // Đặt chiều cao toàn màn hình
                            iframe.style.border = 'none';  // Loại bỏ viền
                        }
                    })();
                    """.trimIndent(), null
                )
            }
        })
    }

    private fun setFullScreen() {
        activity?.window?.decorView?.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }

    override fun onResume() {
        super.onResume()
        setFullScreen()  // Khi quay lại fragment, thiết lập chế độ toàn màn hình
    }
}
