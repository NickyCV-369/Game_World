package com.nhanxu.games

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class AboutUsDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_about_us, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvAboutUs = view.findViewById<TextView>(R.id.tv_about_us)
        tvAboutUs.text = """
            Welcome to Game World!

            Game World is an innovative platform that brings exciting games to you without the need for installation. Our goal is to provide you with seamless gaming experiences anytime, anywhere!

            Developer: Nicky CV

            Contact Us:
            Email: cvnicky369@gmail.com
            Website: https://nhanxu.com/

            Thank you for choosing Game World. We are constantly improving our platform to bring more fun and exciting features to you!
        """.trimIndent()

        val closeButton = view.findViewById<Button>(R.id.close_button)
        closeButton.setOnClickListener {
            dismiss()
        }
    }
}

