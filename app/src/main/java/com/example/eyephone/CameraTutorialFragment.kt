package com.example.eyephone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_camera_tutorial_container.*

class CameraTutorialFragment : DialogFragment() {
    override fun getTheme() = R.style.RoundedCornersDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView: View = inflater.inflate(R.layout.fragment_camera_tutorial_container, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //TODO make sure data sent to adapter is proper
        val data = listOf<String>(
            "Rotate your device 90 degrees to landscape mode",
            "Place your phone in between your eye and the mirror such that you can see your screen using the reflection of the mirror.",
            "If possible, you may ask help from a friend, family member, or caregiver.",
            "Keep your device 4 inches away from your face.",
            "Adjust the zoom so that your eye covers almost the entire screen. Using Flash is recommended to get a clearer image.",
            "Review your photo to ensure the image captured is clear. Retake your image if necessary."
        )
        val titles = listOf<String>(
            "Landscape Mode",
            "Use A Mirror",
            "Ask Help From A friend",
            "Keep Space",
            "Zoom and Flash",
            "Review Your Photo"
        )
        val imgs = listOf<Int>(
            R.drawable.ic_landscape,
            R.drawable.ic_mirror,
            R.drawable.ic_friend,
            R.drawable.ic_selfie,
            R.drawable.ic_zoom,
            R.drawable.ic_selfie2
        )
        val adapter = CameraTutorialAdapter(data, imgs, titles)
        viewPager.adapter = adapter

        indicator.setViewPager(viewPager)
    }
}