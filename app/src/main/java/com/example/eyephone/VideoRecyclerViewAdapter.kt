package com.example.eyephone

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.opencv.video.Video
import java.io.File
//relevant video for setting up the Recycler View Interface: https://www.youtube.com/watch?v=7GPUpvcU1FE
class VideoRecyclerViewAdapter(val context: Context, val videoList : ArrayList<VideoObject>, val recyclerViewInterface : VideoRecyclerViewInterface) : RecyclerView.Adapter<VideoRecyclerViewAdapter.MyViewHolder>() {
    fun isHeader(position: Int): Boolean {
        return position == 0
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VideoRecyclerViewAdapter.MyViewHolder {
        //This is where we inflate the layout and give a look to our rows
        val v = LayoutInflater.from(context).inflate(R.layout.video_recyclerview_row, parent, false)
        return MyViewHolder(v, recyclerViewInterface)
    }

    override fun onBindViewHolder(holder: VideoRecyclerViewAdapter.MyViewHolder, position: Int) {
        //This is where we assign values to each of our rows as they come back on the screen
        //assigning values to the views we created in the video_recyclerview_row layout file
        //based on the position of the recycler view
        holder.tvName.setText(videoList.get(position).fileName)
        holder.tvDate.setText(videoList.get(position).fileDate)
        //Glide functionality is used to create a thumbnail for a video
        Glide
            .with(context)
            .asBitmap()
            .load(Uri.fromFile(File(videoList.get(position).filePath.toString()))) //the .toString call is to change filePath type from String? to String
            .into(holder.tnVid)
        holder.bind(videoList.get(position), recyclerViewInterface)
    }

    override fun getItemCount(): Int {
        //The recycler view wants to know the number of items you want displayed
        return videoList.size
    }
    class MyViewHolder(itemView: View, recyclerViewInterface: VideoRecyclerViewInterface) : RecyclerView.ViewHolder(itemView) {
        //grabbing the views from our video_recyclerview_row layout file
        //kinda like the onCreate method of this
        val tvName : TextView = itemView.findViewById(R.id.textview_name)
        val tvDate : TextView = itemView.findViewById(R.id.textview_date)
        val tnVid : ImageView = itemView.findViewById(R.id.thumbnail)
        fun bind(video : VideoObject, recyclerViewInterface: VideoRecyclerViewInterface) {
            itemView.setOnClickListener {
                if (recyclerViewInterface != null) {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        recyclerViewInterface.onItemClick(pos)
                    }
                }
            }
        }
    }
}