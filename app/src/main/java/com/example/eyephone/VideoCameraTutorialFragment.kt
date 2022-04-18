package com.example.eyephone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_camera_tutorial_container.*

class VideoCameraTutorialFragment : DialogFragment() {
    override fun getTheme() = R.style.RoundedCornersDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView: View = inflater.inflate(R.layout.fragment_camera_tutorial_container, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //TODO make sure data sent to adapter is proper
        val data = listOf<String>(
            "Hold your device in portrait mode when recording.",
            "Use the dual-eye outline to properly align your eyes.",
            "If possible, you may ask help from a friend, family member, or caregiver using the back camera.",
            "A recording lasts 10 seconds each, and it is automatically stopped at that time limit.",
            "To show eye motility, move your eyes to look at each of the four corners of your vision, and also to the cardinal directions (up, down, left, right)",
            "Review your video to ensure the video captured is clear. Retake your video if necessary."
        )
        val titles = listOf<String>(
            "Portrait Mode",
            "Dual-Eye Outline",
            "Ask Help From A Friend",
            "Automated Recordings",
            "Eye Test",
            "Review Your Video"
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