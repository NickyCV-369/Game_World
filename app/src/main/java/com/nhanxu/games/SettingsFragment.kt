package com.nhanxu.games

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.core.net.toUri

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnAboutUs = view.findViewById<Button>(R.id.btn_about_us)
        val btnPrivacySettings = view.findViewById<Button>(R.id.btn_privacy_settings)
        val tvVersion = view.findViewById<TextView>(R.id.tv_version)

        btnAboutUs.setOnClickListener {
            showAboutUsDialog()
        }

        btnPrivacySettings.setOnClickListener {
            openPrivacyPolicy()
        }

        val version = "6.9.3"
        tvVersion.text = "Version: $version"
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
}