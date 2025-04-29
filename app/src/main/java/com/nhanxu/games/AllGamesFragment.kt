package com.nhanxu.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
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

        loadGames()
    }

    private fun loadGames() {
        val url = "https://nhanxu.com/api/games/"

        // Gửi yêu cầu API
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                val games = response.getJSONArray("games")
                val gameNames = ArrayList<String>()
                val gameUrls = ArrayList<String>()

                for (i in 0 until games.length()) {
                    val game: JSONObject = games.getJSONObject(i)
                    gameNames.add(game.getString("name"))
                    gameUrls.add(game.getString("url"))
                }

                // Hiển thị danh sách game
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, gameNames)
                gamesListView.adapter = adapter

                // Xử lý khi người dùng chọn game
                gamesListView.setOnItemClickListener { _, _, position, _ ->
                    val gameUrl = gameUrls[position]
                    gameWebView.webViewClient = WebViewClient()
                    val webSettings: WebSettings = gameWebView.settings
                    webSettings.javaScriptEnabled = true
                    gameWebView.loadUrl(gameUrl)
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace() // Xử lý lỗi
            })

        Volley.newRequestQueue(requireContext()).add(jsonObjectRequest)
    }
}
