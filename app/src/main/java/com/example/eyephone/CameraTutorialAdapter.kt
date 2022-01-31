package com.example.eyephone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_camera_tutorial_data.view.*

class CameraTutorialAdapter(
        val data : List<String>,
        val imgs : List<Int>,
        val titles: List<String>
) : RecyclerView.Adapter<CameraTutorialAdapter.ViewPagerViewHolder>() {
    inner class ViewPagerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_camera_tutorial_data, parent, false)
        return ViewPagerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
        //TODO fix how the data is connected to layout elements
        val curString = data[position]
        val curImg = imgs[position]
        val curTitle = titles[position]
        holder.itemView.descText.setText(curString)
        holder.itemView.titleText.setText(curTitle)
        holder.itemView.imageView2.setImageResource(curImg)
    }

    override fun getItemCount(): Int {
        return data.size
    }
}