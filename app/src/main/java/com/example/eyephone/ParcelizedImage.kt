package com.example.eyephone

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ParcelizedImage(val timestamp :String, val imgTitle: String, val imgType: String, val filePath: String, val alias: String) :Parcelable {
}