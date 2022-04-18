package com.example.eyephone

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeAdapter(private val context: Context, val itemClickListener: OnItemClickListener, val imageList: ArrayList<DecryptedImage>): RecyclerView.Adapter<HomeAdapter.PageHolder>(){
    inner class PageHolder(view: View): RecyclerView.ViewHolder(view){
        val recyclerView : RecyclerView = view.findViewById(R.id.capture_recyclerview)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        return PageHolder(LayoutInflater.from(context).inflate(R.layout.fragment_home_recyclerview, parent, false))
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        if(position == 0){
            val captureAdapter = CaptureAdapter(imageList, itemClickListener, context, Constants().HOME_ADAPTER_GRID)
            val layoutManager = GridLayoutManager(context, 3)
            layoutManager.setSpanSizeLookup(object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (captureAdapter.getItemViewType(position)) {
                        captureAdapter.TYPE_HEADER -> 3
                        captureAdapter.TYPE_ITEM -> 1
                        else -> 1
                    }
                }
            })
            holder.recyclerView.layoutManager = layoutManager
            holder.recyclerView.adapter = captureAdapter
        }else if(position == 1){
            holder.recyclerView.layoutManager = LinearLayoutManager(context)
            val captureAdapter = CaptureAdapter(imageList, itemClickListener, context, Constants().HOME_ADAPTER_LIST)
            holder.recyclerView.adapter = captureAdapter
        }
        else{
            holder.recyclerView.layoutManager = LinearLayoutManager(context)
            val captureAdapter = CaptureAdapter(imageList, itemClickListener, context, Constants().HOME_ADAPTER_LIST)
            holder.recyclerView.adapter = captureAdapter
        }

    }

    override fun getItemCount(): Int {
        return 2 // either grid or list
    }

}