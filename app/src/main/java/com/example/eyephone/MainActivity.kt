package com.example.eyephone

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Base64
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.myToolbar
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class MainActivity : AppCompatActivity(),OnItemClickListener {
    var imageList = ArrayList<DecryptedImage>()
    override fun onDestroy() {
        // TODO Auto-generated method stub
        println("DESTROY: On Destroy Was Called")
        super.onDestroy()
    }

    override fun onStart() {
        println("On Start Called.....${imageList.size}")

        super.onStart()
    }
    override fun onResume() {
        // TODO Auto-generated method stub
        println("RESUME: On Resume Was Called......${imageList.size}")

        super.onResume()
    }
    override fun onNewIntent(intent: Intent?) {
        // this is called after camera save.
        // read latest saved image, decrypt, then reload ui
        println("onNewIntent: onNewIntent Was Called......${imageList.size}")

        super.onNewIntent(intent)
        // getIntent() should always return the most recent
        setIntent(intent)
        if (intent != null) {
            checkIntent(intent, mainView, fabOpenCamera)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.app_toolbar_home_menu, menu);
        return true
    }
    override fun onItemClicked(image: DecryptedImage) {
        //clear cache
        this.cacheDir.deleteRecursively()
        //create temp file to show image
        val outputDir: File = this.cacheDir // context being the Activity pointer
        val fileName = "img_${SystemClock.uptimeMillis()}.jpg"
        File(outputDir, fileName).writeBitmap(image.toBitmap(), Bitmap.CompressFormat.JPEG, 100)
        val uri = Uri.fromFile(File("$outputDir/${fileName}"))
        val intent = Intent(this, ShareActivity::class.java)
        intent.putExtra("Uri", uri.toString())
        intent.putExtra("image", ParcelizedImage(
                image.createTimestamp(),
                image.imgTitle.toString(),
                image.img_type.toString(),
                image.filePath.toString(),
                image.alias.toString()
        ))

        startActivity(intent)
    }
    private fun checkIntent(intent: Intent, view: View, fab: FloatingActionButton){
        val extras = intent.extras
        if (extras!=null){
            if (extras.getBoolean("showSnackbar")){
                val text = extras.getString("snackbarText")
                if (text != null){
                    Snackbar.make(fab, text, Snackbar.LENGTH_SHORT)
                            .show()
                }
            }

            val action = extras.getString("action").toString()
            if (action =="add" || action == "delete"){
                val identity = extras.getString("identity") //If "add" -> identity=filename, If "delete" -> identity=alias
                CoroutineScope(IO).launch {
                    if (identity != null) {
                        updateHomeScreen(action, identity)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        println("HELLO")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(myToolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)
        checkIntent(intent, mainView, fabOpenCamera)

        fabOpenCamera.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent);
        }

        linearProgressIndicator.show()
        textProgressIndicator.visibility = View.VISIBLE
        CoroutineScope(IO).launch {
            initializeHomeScreen()
        }
    }

    private suspend fun updateHomeScreen(action:String, identity: String) = withContext(IO){

        if(action == "add"){
            val image = readAndDecryptImage(identity)
            imageList.add(image)
            imageList.sortByDescending { it.imgDate }
            println("Debug: Finished adding new file")
        }else if (action == "delete"){
            for(image in imageList){
                if (image.alias == identity){
                    imageList.remove(image)
                    break
                }
            }
            println("Debug: Finished removing file")
        }

        withContext(Main){
            //setup viewpager AND tab layout
            if (imageList.size == 0) {
                home_textview.visibility = View.VISIBLE
            }else{
                home_textview.visibility = View.GONE
            }
            println("Debug: Starting updating adapter")
            println("Debug: Launching Adapter Processes in ${Thread.currentThread().name}")
            viewpager.adapter = HomeAdapter(this@MainActivity, this@MainActivity, imageList)
            val tabIcons = listOf<Int>(
                R.drawable.ic_sharp_grid_on_24,
                R.drawable.ic_baseline_list_alt_24
            )
            TabLayoutMediator(myTabBar, viewpager) { tab, position ->
                viewpager.setCurrentItem(tab.position, true)
                tab.icon = getDrawable(tabIcons[position])
            }.attach()
            println("Debug: Finished updating adapter")
            linearProgressIndicator.hide()
            textProgressIndicator.visibility = View.INVISIBLE
        }
        println("Finished updating home screen")
    }

    private suspend fun initializeHomeScreen() = withContext(IO){
        println("Debug: Launching InitializeHomeScreen in ${Thread.currentThread().name}")
        val path = "${this@MainActivity.filesDir.absolutePath}/MyCaptures"
        val dir = File(path)
        val files = dir.listFiles()

        if(files != null){
            linearProgressIndicator.max = files.size
            println("Debug: Starting going thru each file and decrypting")
            imageList = readAndDecryptImages()
            // sort image list
            imageList.sortByDescending { it.imgDate }

            println("Debug: Finished going thru each file and decrypting")

        }
        withContext(Main){
            //setup viewpager AND tab layout
            if (imageList.size == 0) {
                home_textview.visibility = View.VISIBLE
            }else{
                home_textview.visibility = View.GONE
            }
            println("Debug: Starting Attaching images to adapter")
            println("Debug: Launching Adapter Processes in ${Thread.currentThread().name}")
            viewpager.adapter = HomeAdapter(this@MainActivity, this@MainActivity, imageList)
            val tabIcons = listOf<Int>(
                R.drawable.ic_sharp_grid_on_24,
                R.drawable.ic_baseline_list_alt_24
            )


            TabLayoutMediator(myTabBar, viewpager) { tab, position ->
                viewpager.setCurrentItem(tab.position, true)
                tab.icon = getDrawable(tabIcons[position])
            }.attach()
            println("Debug: Finished Attaching images to adapter")
            linearProgressIndicator.hide()
            textProgressIndicator.visibility = View.INVISIBLE
        }
        println("Debug: HomeScreen fully loaded now! Hello!")
    }

    private suspend fun setTextProgressIndicatorText(str: String) = withContext(Main) {
        textProgressIndicator.text = str
    }
    private suspend fun readAndDecryptImage(filename: String): DecryptedImage = withContext(IO) {
        println("Debug: Launching readAndDecryptImages in ${Thread.currentThread().name}")
        val path = "${this@MainActivity.filesDir.absolutePath}/MyCaptures/${filename}"
        val file = File(path)
        // read json string of file
        val data = file.bufferedReader().use { it.readText() }
        // create encryptedImage object from json string
        val gson = Gson()
        val encryptedImage = gson.fromJson(data, EncryptedImage::class.java)
        // base64decode
        val ivBytes = Base64.decode(encryptedImage.ivString, Base64.NO_WRAP)
        val encryptedBytes = Base64.decode(encryptedImage.encryptedStr, Base64.NO_WRAP)
        val alias = encryptedImage.keyAlias
        val img_title = encryptedImage.imgTitle
        val img_type = encryptedImage.imgType
        val img_date = encryptedImage.dateTaken

        // decrypt
        val decryptedBytes = withContext(Default) {
            decrypt(ivBytes, encryptedBytes, alias)
        }


        // create decryptedImage object
        val filePath = applicationContext.filesDir.absolutePath + "/MyCaptures/IMG_$alias.json"

        val decryptedImage = DecryptedImage(decryptedBytes, alias, filePath, img_title, img_type, img_date)
        decryptedImage
    }
    private suspend fun readAndDecryptImages(): ArrayList<DecryptedImage> = withContext(IO) {
        println("Debug: Launching readAndDecryptImages in ${Thread.currentThread().name}")
        val path = "${this@MainActivity.filesDir.absolutePath}/MyCaptures"
        val dir = File(path)
        val files = dir.listFiles()
        val imageList = ArrayList<DecryptedImage>()
        if(files != null) {
            var fileCtr = 1
            val fileSize = files.size
            for (file in files) {
                setTextProgressIndicatorText("Decrypting... (${fileCtr}/${fileSize})")
                // read json string of file
                val data = file.bufferedReader().use { it.readText() }
                // create encryptedImage object from json string
                val gson = Gson()
                val encryptedImage = gson.fromJson(data, EncryptedImage::class.java)
                // base64decode
                val ivBytes = Base64.decode(encryptedImage.ivString, Base64.NO_WRAP)
                val encryptedBytes = Base64.decode(encryptedImage.encryptedStr, Base64.NO_WRAP)
                val alias = encryptedImage.keyAlias
                val img_title = encryptedImage.imgTitle
                val img_type = encryptedImage.imgType
                val img_date = encryptedImage.dateTaken

                // decrypt
                val decryptedBytes = withContext(Default) {
                        decrypt(ivBytes, encryptedBytes, alias)
                    }


                // create decryptedImage object
                val filePath = this@MainActivity.filesDir.absolutePath + "/MyCaptures/IMG_$alias.json"
                val decryptedImage = DecryptedImage(
                    decryptedBytes,
                    alias,
                    filePath,
                    img_title,
                    img_type,
                    img_date
                )
                imageList.add(decryptedImage)

                linearProgressIndicator.setProgressCompat(fileCtr, true)
                fileCtr++
            }
        }
        imageList
    }

    private fun decrypt(ivBytes: ByteArray, encryptedBytes: ByteArray, alias: String): ByteArray {
        println("Debug: Launching Decrypt in ${Thread.currentThread().name}")
        // get the key

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secretKeyEntry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry.secretKey

        // decrypt the data
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            return cipher.doFinal(encryptedBytes) // this is the call that prints out "keystore operation promise future onFinished

        }catch (exc: Exception){
            println("Exception found. Message: ${exc.message}")
        }
        return ByteArray(0)

    }
    private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }
}
