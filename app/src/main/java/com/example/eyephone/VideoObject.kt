package com.example.eyephone

import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.Glide
import java.io.File

class VideoObject(val fileName: String? = null, val fileDate: String? = null, val filePath: String? = null, val thumbnail: Bitmap? = null) {
    fun printContents() {
        println("The file name of the video is ${fileName}")
        println("The file date of the video is ${fileDate}")
        println("The file path of the video is ${filePath}")
    }
//    fun generateThumbnail() {
//        //generating the thumbnail for the video
//        var thumbnailbmp : Bitmap
//        Glide
//            .with(this)
//            .asBitmap()
//            .load(Uri.fromFile(File(files[i].path)))
//            .into(thumbnailbmp)
//    }
}