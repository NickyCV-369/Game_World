package com.nhanxu.games

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.core.net.toUri
import android.webkit.CookieManager

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnClearCache = view.findViewById<Button>(R.id.btn_clear_cache)
        val btnAboutUs = view.findViewById<Button>(R.id.btn_about_us)
        val btnPrivacySettings = view.findViewById<Button>(R.id.btn_privacy_settings)
        val tvVersion = view.findViewById<TextView>(R.id.tv_version)

        btnClearCache.setOnClickListener {
            showClearAllDataDialog()
        }

        btnAboutUs.setOnClickListener {
            showAboutUsDialog()
        }

        btnPrivacySettings.setOnClickListener {
            openPrivacyPolicy()
        }

        val version = "9.3.9"
        tvVersion.text = "Version: $version"
    }

    private fun showClearAllDataDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Clear All Data")
        builder.setMessage("This will erase all game progress! Are you sure you want to delete it?")

        builder.setPositiveButton("OK") { _, _ ->
            clearWebViewDataInFragments()
        }

        builder.setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.show()
    }

    private fun showAboutUsDialog() {
        val dialog = AboutUsDialogFragment()
        dialog.show(parentFragmentManager, "about_us_dialog")
    }

    private fun openPrivacyPolicy() {
        val url = "https://nhanxu.com/privacy-policy"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }

    private fun clearWebViewDataInFragments() {
        val allGamesFragment = parentFragmentManager.findFragmentByTag(AllGamesFragment::class.java.simpleName) as? AllGamesFragment
        allGamesFragment?.clearWebViewData()

        val homeFragment = parentFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName) as? HomeFragment
        homeFragment?.clearWebViewData()

        val recentlyPlayedGamesFragment = parentFragmentManager.findFragmentByTag(RecentlyPlayedGamesFragment::class.java.simpleName) as? RecentlyPlayedGamesFragment
        recentlyPlayedGamesFragment?.clearWebViewData()

        Toast.makeText(requireContext(), "Memory released successfully!", Toast.LENGTH_SHORT).show()
    }
}