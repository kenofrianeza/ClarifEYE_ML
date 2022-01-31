package com.example.eyephone

import java.text.SimpleDateFormat
import java.util.*

class EncryptedImage(val ivString: String, val encryptedStr: String, val keyAlias: String, val imgTitle: String, val imgType: String, val dateTaken: Date){
    fun createTimestamp(): String{
        val simpleDateFormat = SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss:S z")
        return simpleDateFormat.format(dateTaken)
    }
}

