package com.example.eyephone

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_save_image.*
import kotlinx.android.synthetic.main.activity_save_image.circleProgress
import kotlinx.android.synthetic.main.activity_share.*
import kotlinx.android.synthetic.main.video_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

class SaveVideoActivity : AppCompatActivity() {
    lateinit var videoUri: Uri
    lateinit var filepath: String
    var myTempImage = TempImage(Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888), Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888), 0f, 1f, 0f)

    private val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')


    var videoView: VideoView? = null
    var mediaControls: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_preview)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        videoView = findViewById<View>(R.id.videoView) as VideoView
        val extras = intent.extras

        if (mediaControls == null) {
            // creating an object of media controller class
            mediaControls = MediaController(this)

            // set the anchor view for the video view
            mediaControls!!.setAnchorView(this.videoView)
        }

        // set the media controller for video view
        videoView!!.setMediaController(mediaControls)

        // set the absolute path of the video file which is going to be played
//        VideoView!!.setVideoURI(Uri.parse("android.resource://"
//                + packageName + "/" + R.raw.gfgvideo))

        videoView!!.requestFocus()

        // starting the video
        videoView!!.start()

        // display a toast message
        // after the video is completed
        videoView!!.setOnCompletionListener {
            Toast.makeText(applicationContext, "Video completed",
                Toast.LENGTH_LONG).show()
        }

        // display a toast message if any
        // error occurs while playing the video
        videoView!!.setOnErrorListener { mp, what, extra ->
            Toast.makeText(applicationContext, "An Error Occured " +
                    "While Playing Video !!!", Toast.LENGTH_LONG).show()
            false
        }
        if (extras != null) {
            val uriString = extras.getString("video")
            println("Ito ang URI string ${uriString}")
            if (uriString!=null){
                val uri = Uri.parse(uriString)
                Log.d("CameraXBasic", "Received URI in second activity")
                Log.d("CameraXBasic","VideoURI: $uri")
                val vidAsFile = File(uri.path)
                filepath = vidAsFile.path
                vidName.text = vidAsFile.name
                vidDate.text = Date(vidAsFile.lastModified()).toString()
                videoView!!.setVideoURI(uri)
//                videoView!!.setVideoURI(Uri.parse("android.resource://"
//                        + packageName + "/" + R.raw.samplevid))
                videoUri = uri
//                myTempImage.originalBmp = (imageView.drawable as BitmapDrawable).bitmap
//                myTempImage.finalBmp = (imageView.drawable as BitmapDrawable).bitmap
            }
        }
        deleteVidBtn.setOnClickListener{
            println("delete button pressed")
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete Video")
                .setMessage("You are about to delete this video. This action cannot be undone. ")
                .setNegativeButton("Cancel") { dialog, which ->
                    // Respond to negative button press
                }
                .setPositiveButton("Delete") { dialog, which ->
                    // Respond to positive button press
                    val isDeleted = deleteVideo(filepath)
                    println("My Filepath is $filepath")
                    val intent = Intent(this, ViewVideosActivity::class.java)
                    //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    //intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    intent.putExtra("showSnackbar", true)
                    intent.putExtra("action", "delete")
                    intent.putExtra("identity", File(filepath))
                    intent.putExtra("success", isDeleted)
                    if(isDeleted){
                        intent.putExtra("snackbarText", "Your video was successfully deleted.")
                    }else{
                        intent.putExtra("snackbarText", "Error deleting video.")
                    }
                    startActivity(intent)
                    finish()
                }
                .show()
        }
        shareVidBtn.setOnClickListener{
            println("Share button pressed")
            val uri = videoUri
            val myPath = uri.path
            println("MY PATH: $myPath")
            val myFile = File(myPath)
            val photoURI = FileProvider.getUriForFile(this, this.applicationContext.packageName.toString() + ".provider", myFile)
            val bundle = Bundle()
            bundle.putString("uri", uri.toString())
            bundle.putString("filepath", myPath)
            bundle.putParcelable("photoUri", photoURI)
            bundle.putString("title", myFile.name)

            ShareFragment().apply {
                show(supportFragmentManager, tag)
                arguments = bundle
            }



        }
    }

    private fun deleteVideo(filepath: String): Boolean{
        val file = File(filepath)
        if (file.exists()){
            println("FILE EXISTS!")
        }else{
            println("FILE NOT FOUND!")
        }
        return file.delete()
    }

    private suspend fun returnToVideoList(filename: String) = withContext(Dispatchers.Main){

        println("Debug: Launching returnToVideoList in ${Thread.currentThread().name}")
        val intent = Intent(this@SaveVideoActivity, ViewVideosActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("showSnackbar", true)
        intent.putExtra("snackbarText", "Video saved successfully.")
        intent.putExtra("action", "add")
        intent.putExtra("identity", filename)
        println("Debug: Returning to home screen now. Bye!")
        startActivity(intent);
    }

    private fun generateAlias(): String = List(16) { chars.random() }.joinToString("")

    private fun generateKey(alias: String) {
        println("Debug: Launching generateKey in ${Thread.currentThread().name}")
        //generate random key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun encrypt(data: ByteArray, alias: String): Pair<ByteArray, ByteArray> {
        println("Debug: Launching encrypt in ${Thread.currentThread().name}")
        // get the key

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secretKeyEntry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry.secretKey

        // encrypt the data
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val ivBytes = cipher.iv
        val encryptedBytes = cipher.doFinal(data)

        return Pair(ivBytes, encryptedBytes)
    }

//    private suspend fun save() {
//        withContext(Dispatchers.IO) {
//            println("Debug: Launching Video Save in ${Thread.currentThread().name}")
//            // obtain final bitmap
//            val imageBitmap = myTempImage.finalBmp
//            // embed L|R on image
//            val overlay = createOverlay(img_type)
//            //convert embedded image to a byte array stream
//            val stream = ByteArrayOutputStream()
//            overlay(imageBitmap, overlay)?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//            val finalImage = stream.toByteArray()
//            // encrypt image byte array using android keystore
//            // generate a new alias and key
//            val alias = generateAlias()
//            generateKey(alias)
//            // encrypt imageByteArray using key alias. Returns a par of IvBytes and Encrypted Bytes
//            val pair: Pair<ByteArray, ByteArray> = encrypt(finalImage, alias)
//            // convert byte arrays to strings and save as json string
//            val jsonString = generateJsonString(
//                pair.first,
//                pair.second,
//                alias,
//                img_title,
//                img_type,
//                Date()
//            )
//            val filename = "IMG_$alias.json"
//            // store data into file (JSON FORMAT)
//            writeJson(filename, jsonString)
//            returnToHome(filename)
//            withContext(Dispatchers.Main) {
//                circleProgress.hide()
//            }
//        }
//    }
}