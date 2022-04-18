package com.example.eyephone

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CaptureAdapter(val imageList: ArrayList<DecryptedImage>, val itemClickListener: OnItemClickListener, val context: Context, val type: Int) : RecyclerView.Adapter<CaptureAdapter.ViewHolder>(){

    val TYPE_HEADER: Int = 0
    val TYPE_ITEM: Int = 1

    fun isHeader(position: Int): Boolean {
        return position == 0
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        if(viewType == TYPE_HEADER){
            val v = LayoutInflater.from(parent.context).inflate(R.layout.capture_item_header, parent, false)
            return ViewHolder(v)
        }

        val v = when(type){
            Constants().HOME_ADAPTER_GRID -> LayoutInflater.from(parent.context).inflate(R.layout.capture_item_grid, parent, false)
            Constants().HOME_ADAPTER_LIST -> LayoutInflater.from(parent.context).inflate(R.layout.capture_item_list, parent, false)
            else -> LayoutInflater.from(parent.context).inflate(R.layout.capture_item_grid, parent, false) // defaults to grid
        }
        return ViewHolder(v)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isHeader(position)) TYPE_HEADER else TYPE_ITEM
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (isHeader(position)) {
            if (imageList.size == 0){
                holder.title.visibility = View.INVISIBLE
                holder.desc.visibility = View.INVISIBLE
            }else if(imageList.size == 1){
                holder.title.visibility = View.VISIBLE
                holder.desc.visibility = View.VISIBLE
                val string = "${imageList.size} photo"
                holder.desc.text = string
            } else{
                holder.title.visibility = View.VISIBLE
                holder.desc.visibility = View.VISIBLE
                val string = "${imageList.size} photos"
                holder.desc.text = string
            }
            return
        }
        val image = imageList.get(position-1)
        holder.title.text = image.imgTitle
//        holder.img.setImageBitmap(image.toBitmap())
        Glide.with(context).load(image.byteArray).into(holder.img)
        holder.desc.text = image.createTimestamp()
        holder.bind(image, itemClickListener)
    }

    override fun getItemCount(): Int { 
        return imageList.size + 1 //add one for header
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.capture_title)
        val img : ImageView = itemView.findViewById(R.id.capture_img)
        val desc: TextView = itemView.findViewById(R.id.capture_desc)

        fun bind(image: DecryptedImage, clickListener: OnItemClickListener) {
            itemView.setOnClickListener { clickListener.onItemClicked(image) }
        }
    }
}

interface OnItemClickListener{
    fun onItemClicked(image: DecryptedImage)
}