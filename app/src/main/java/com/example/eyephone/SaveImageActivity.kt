 package com.example.eyephone

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicConvolve3x3
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_save_image.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import kotlin.math.round


 class TempImage(var originalBmp: Bitmap, var finalBmp: Bitmap, var brightnessMod: Float, var contrastMod: Float, var sharpnessMod: Float);

 class SaveImageActivity : AppCompatActivity() {
    lateinit var imageUri: Uri

    //initialize tempImage class with default values
    var myTempImage = TempImage(Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888), Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888), 0f, 1f, 0f)
    private val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')

     override fun onCreateOptionsMenu(menu: Menu?): Boolean {
         menuInflater.inflate(R.menu.app_toolbar_savephoto_menu, menu);
         return true
     }
     override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
             R.id.action_save -> {
                 // User chose the "Info" item, show the app Info UI...

                 circleProgress.show()
                 println("Debug: Launching Progress Indicator in ${Thread.currentThread().name}")
                 CoroutineScope(IO).launch{
                     save()
                 }
                 // returnToHome()
//                 circleProgress.hide()

                 true
             }

             else -> {
                 // If we got here, the user's action was not recognized.
                 // Invoke the superclass to handle it.
                 super.onOptionsItemSelected(item)
             }
     }

     private fun stringToList(s: String): List<String> {
         var newString = s.substring(1, s.length -1)
         newString = newString.replace("ImageLabel", "")
         newString = newString.substring(1, newString.length -1)
         var newList = newString.trim().splitToSequence(',').toList()
         var evaluation = newList.get(0)
         var confidenceLvl = newList.get(1)
         evaluation = evaluation.substring(evaluation.indexOf('=')+1, evaluation.length)
         confidenceLvl = confidenceLvl.substring(confidenceLvl.indexOf('=')+1, confidenceLvl.length)
         var finalList = listOf(evaluation, confidenceLvl)
         return finalList
     }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_image)
//        supportActionBar?.hide()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val extras = intent.extras
        if (extras != null) {
            val uriString = extras.getString("picture")
            if (uriString!=null){
                val uri = Uri.parse(uriString)
                println("Received URI in second activity")
                imageView.setImageURI(uri)
                imageUri = uri
                myTempImage.originalBmp = (imageView.drawable as BitmapDrawable).bitmap
                myTempImage.finalBmp = (imageView.drawable as BitmapDrawable).bitmap
            }
            val labelString : String? = extras.getString("labels")
            val labelList = labelString?.let { stringToList(it) }
            val blurriness = extras.getString("blur")
            if (labelList!=null && blurriness != null) {
                println("Received labels in second activity")
                //labelView.text = "Evaluation: " + labelList.get(0) + "\n" + "Confidence: " + String.format("%.3f", labelList.get(1).toDouble()*100) + "%\n" + "Blur Score (over 200 good): " + blurriness
                if (blurriness.toDouble() >= 100 && (labelList.get(1).toDouble()*100) >= 99.60) {
                    labelView.text = "CAPTURED IMAGE IS OF GOOD QUALITY!"
                }
                else {
                    labelView.text = "CAPTURED IMAGE IS BLURRY/UNCLEAR; PLEASE RETAKE"
                }
                val pic_score = "Evaluation: " + labelList.get(0) + "\n" + "Confidence: " + String.format("%.3f", labelList.get(1).toDouble()*100) + "%\n" + "Blur Score (over 100 good): " + blurriness
                println(pic_score)
            }
        }

        // Initialize adapter array for eye type
        val adapter = ArrayAdapter(this, R.layout.list_item, Constants().IMAGE_TYPES)
        (dropdown.editText as? AutoCompleteTextView)?.setAdapter(adapter)

        // Initialize Placeholder Title
        imgTitleContainer.editText?.setText(generateTitle())
        imgTitleContainer.setEndIconOnClickListener {
            imgTitleContainer.editText?.setText("")
        }

        // initialize sharpen seekbar
        sharpenSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                // write custom code for progress is changed

                progressIndicator.setText(progress.toString())
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
                myTempImage.sharpnessMod = seek.progress.toFloat() / 100
                editPhoto(myTempImage)
                imageView.setImageBitmap(myTempImage.finalBmp)
            }
        })
        // initialize brightness seekbar
        brightnessSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                // write custom code for progress is changed
                progressIndicator.setText(progress.toString())
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped

                //NOTE: Progress range: [0,510]. Default brightness is 255. Subtract progress by 255 to get a range of [-255,255]
                myTempImage.brightnessMod = seek.progress.toFloat() - 255
                editPhoto(myTempImage)
                imageView.setImageBitmap(myTempImage.finalBmp)

            }
        })
        // initialize contrast seekbar
        contrastSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                // write custom code for progress is changed
                progressIndicator.setText(progress.toString())
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped

                //NOTE: Progress range: [0,200]. Default Contrast is 100. Divide progress by 100 to get a range of [0,10]
                myTempImage.contrastMod = seek.progress.toFloat() / 100
                editPhoto(myTempImage)
                imageView.setImageBitmap(myTempImage.finalBmp)

            }
        })
    }

    @Throws(IOException::class)
    private fun readBytes(context: Context, uri: Uri): ByteArray? =
        context.contentResolver.openInputStream(uri)?.buffered()?.use { it.readBytes() }

    private suspend fun validate(img_title: String, img_type: String) : Boolean = withContext(Main){
            println("Debug: Launching Validate in ${Thread.currentThread().name}")
            var validated = true
            if (img_title == "") {
                imgTitleContainer.error = "Please input a valid image title."
                validated = false
            }
            if (img_type == "") {
                dropdown.error = "Please select a valid image type."
                validated = false
            }
            validated
        }


    private suspend fun save() {
        withContext(IO){

            println("Debug: Launching Save in ${Thread.currentThread().name}")

            val img_title = imgTitleContainer.editText?.text.toString()
            val img_type = dropdown.editText?.text.toString()

            if (validate(img_title, img_type)) {
// obtain final bitmap
                val imageBitmap = myTempImage.finalBmp
                // embed L|R on image
                val overlay = createOverlay(img_type)
                //convert embedded image to a byte array stream
                val stream = ByteArrayOutputStream()
                overlay(imageBitmap, overlay)?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val finalImage = stream.toByteArray()
                // encrypt image byte array using android keystore
                // generate a new alias and key
                val alias = generateAlias()
                generateKey(alias)
                // encrypt imageByteArray using key alias. Returns a par of IvBytes and Encrypted Bytes
                val pair: Pair<ByteArray, ByteArray> = encrypt(finalImage, alias)
                // convert byte arrays to strings and save as json string
                val jsonString = generateJsonString(
                    pair.first,
                    pair.second,
                    alias,
                    img_title,
                    img_type,
                    Date()
                )
                val filename = "IMG_$alias.json"

                // store data into file (JSON FORMAT)
                writeJson(filename, jsonString)
                returnToHome(filename)
            }
            withContext(Main){
                circleProgress.hide()
            }


        }

     }

    private fun createOverlay(imgType: String): Bitmap{
         if (imgType == Constants().LEFT_EYE) {
             return BitmapFactory.decodeResource(this.resources, R.drawable.overlay_left_eye)}
         else if (imgType == Constants().RIGHT_EYE) {
             return BitmapFactory.decodeResource(applicationContext.resources, R.drawable.overlay_right_eye)}
         return BitmapFactory.decodeResource(applicationContext.resources, R.drawable.overlay_both_eyes_flipped)
     }

    private fun overlay(bmp1: Bitmap, bmp2: Bitmap): Bitmap? {
        println("Debug: Launching Overlay in ${Thread.currentThread().name}")
         val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, bmp1.config)
         val canvas = Canvas(bmOverlay)
         canvas.drawBitmap(bmp1, Matrix(), null)
         canvas.drawBitmap(bmp2, 30f, 30f, null)
         bmp1.recycle()
         bmp2.recycle()
         return bmOverlay
    }
    fun editPhoto(image: TempImage){
         val sharpenedImage = modifySharpness(image.originalBmp, image.sharpnessMod)
         val brightenedImage = changeBitmapContrastBrightness(sharpenedImage, image.contrastMod, image.brightnessMod)
         image.finalBmp = brightenedImage
     }

     /**
      * @param bmp input bitmap
      * @param contrast 0..2 1 is default
      * @param brightness -255..255 0 is default
      * @return new bitmap
     */
    fun changeBitmapContrastBrightness(bmp: Bitmap, contrast: Float, brightness: Float): Bitmap {

//         val t = (1.0f - contrast)/2.0f
         val t = 0f
         val cm = ColorMatrix(floatArrayOf(
                 contrast, 0f, 0f, 0f, brightness,
                 0f, contrast, 0f, 0f, brightness,
                 0f, 0f, contrast, 0f, brightness,
                 t, t, t, 1f, 0f))
         val ret = Bitmap.createBitmap(bmp.width, bmp.height, bmp.config)
         val canvas = Canvas(ret)
         val paint = Paint()
         paint.setColorFilter(ColorMatrixColorFilter(cm))
         canvas.drawBitmap(bmp, 0f, 0f, paint)
         return ret
     }
     /**
      * @param bmp input bitmap
      * @param multiplier 0..1 0 is default
      * @return new bitmap
     */
    fun modifySharpness(bitmap: Bitmap, multiplier: Float): Bitmap {
         val sharp = floatArrayOf(0f, (-multiplier).toFloat(), 0f, (-multiplier).toFloat(), 1 + 4f * multiplier, (-multiplier).toFloat(), 0f, (-multiplier).toFloat(), 0f)
         val newBitmap = Bitmap.createBitmap(
                 bitmap.getWidth(), bitmap.getHeight(),
                 Bitmap.Config.ARGB_8888)

         val rs = RenderScript.create(this)

         val allocIn = Allocation.createFromBitmap(rs, bitmap)
         val allocOut = Allocation.createFromBitmap(rs, newBitmap)

         val convolution = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
         convolution.setInput(allocIn)
         convolution.setCoefficients(sharp)
         convolution.forEach(allocOut)

         allocOut.copyTo(newBitmap)
         rs.destroy()

         return newBitmap
     }
    private fun generateAlias(): String = List(16) { chars.random() }.joinToString("")
    private fun generateTitle(): String {
        val simpleDateFormat = SimpleDateFormat("dd MMM yyyy")
        val date = simpleDateFormat.format(Date())
        return "Capture - $date"
    }
    private fun generateJsonString(
            ivBytes: ByteArray,
            encryptedBytes: ByteArray,
            alias: String,
            title: String,
            type: String,
            date: Date
    ): String {
        val ivBase64Str = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
        val encryptedBase64Str = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val encryptedImage = EncryptedImage(
                ivBase64Str,
                encryptedBase64Str,
                alias,
                title,
                type,
                date
        )
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(encryptedImage)
    }

    private fun writeJson(filename: String, data: String): Boolean {
        println("Debug: Launching writeJson in ${Thread.currentThread().name}")
        val path = this.filesDir.absolutePath
        println("From writeJson" + this.filesDir.absolutePath)
        val dir = File("$path/MyCaptures")
        if (!dir.exists()){
            dir.mkdir()
        }
        println("PATH: $dir")
        return try{
            val file = File("$dir/$filename")
            file.writeText(data)
            true

        }catch (exc: IOException){
            exc.printStackTrace()
            Toast.makeText(applicationContext, "Error saving image.", Toast.LENGTH_SHORT).show()
            false
        }

    }

     private suspend fun returnToHome(filename: String) = withContext(Main){

         println("Debug: Launching returnToHome in ${Thread.currentThread().name}")
         val intent = Intent(this@SaveImageActivity, MainActivity::class.java)
         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
         intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
         intent.putExtra("showSnackbar", true)
         intent.putExtra("snackbarText", "Image saved successfully.")
         intent.putExtra("action", "add")
         intent.putExtra("identity", filename)
         println("Debug: Returning to home screen now. Bye!")
         startActivity(intent);
     }

    private fun generateKey(alias: String){
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
    private fun encrypt(data: ByteArray, alias: String): Pair<ByteArray, ByteArray>{
        println("Debug: Launching encrypt in ${Thread.currentThread().name}")

        // get the key

        //get the instance of the keystore
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        //load null as initialization
        keyStore.load(null)
        //get the secret key itself (called the secretKeyEntry) by its alias
        val secretKeyEntry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
        //extract the secretKey attribute from the secretKeyEntry object
        val secretKey = secretKeyEntry.secretKey

        // encrypt the data

        //call an instance of Cipher with configurations AES, GCM, and no padding
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        //initialize the cipher in encrypt mode, using the secret key
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        //get the initialization vector of the resulting cipher
        val ivBytes = cipher.iv
        //do the actual encrypting part on the data with type ByteArray using the doFinal method
        val encryptedBytes = cipher.doFinal(data)
        //return ivBytes and encryptedBytes as a tuple
        return Pair(ivBytes, encryptedBytes)
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}