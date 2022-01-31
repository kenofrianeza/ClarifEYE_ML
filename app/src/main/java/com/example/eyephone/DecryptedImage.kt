package com.example.eyephone

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.text.SimpleDateFormat


@Parcelize
class DecryptedImage(val byteArray: ByteArray? = null, val alias: String? = null, val filePath: String? = null, val imgTitle: String? = null, val img_type: String? = null, val imgDate: java.util.Date? = null) : Parcelable{
    fun toBitmap (): Bitmap {
        if (byteArray != null) {
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // null bitmap
    }
    fun createTimestamp(): String{
        val simpleDateFormat = SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss:S z")
        if(imgDate != null){
            return simpleDateFormat.format(imgDate)
        }
        return simpleDateFormat.format(java.util.Date())
    }
}

