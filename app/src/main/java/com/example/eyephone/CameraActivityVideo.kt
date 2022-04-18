package com.example.eyephone

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.SensorManager
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.fotoapparat.parameter.Flash
import kotlinx.android.synthetic.main.activity_camera.cameraTip
import kotlinx.android.synthetic.main.activity_camera.cameraTipContainer
import kotlinx.android.synthetic.main.activity_camera.camera_flash_button
import kotlinx.android.synthetic.main.activity_camera.camera_flip_button
import kotlinx.android.synthetic.main.activity_camera.focusFrame
import kotlinx.android.synthetic.main.activity_camera.focusImg
import kotlinx.android.synthetic.main.activity_camera.myToolbar
import kotlinx.android.synthetic.main.activity_camera.overlay
import kotlinx.android.synthetic.main.activity_camera.viewFinder
import kotlinx.android.synthetic.main.activity_camera.zoomSlider
import kotlinx.android.synthetic.main.activity_camera_video.*
import java.io.File
import java.net.URISyntaxException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivityVideo : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private val INITIAL_ZOOM = 0f
    private lateinit var camera: Camera
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var lensFace = CameraSelector.DEFAULT_FRONT_CAMERA
    private var hasFlash: Boolean = true
    private var activityCreated : Boolean = false
    private var showOverlay: Boolean = true
    private var currOrientation = 0
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var myOrientationEventListener: OrientationEventListener

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_info -> {
            // User chose the "Info" item, show the app Info UI...
            showTutorial()
            true
        }
        R.id.action_overlay -> {
            if (!showOverlay) {
                overlay.visibility = View.VISIBLE
                cameraTip.text = getString(R.string.lookInstruction)
                showOverlay = true
            } else {
                overlay.visibility = View.INVISIBLE
                cameraTip.text = ""
                showOverlay = false
            }
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_video)
//        supportActionBar?.hide()
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setSupportActionBar(myToolbar)

        window.navigationBarColor = resources.getColor(R.color.black)
        overlay.setImageResource(R.drawable.overlay_both)
        focusImg.setImageResource(R.drawable.focus)


        //setup shared preferences
        val sharedPrefs = this.getSharedPreferences(Constants().SHARED_PREFS_TITLE, Context.MODE_PRIVATE)
        val sharedPrefsEditor = sharedPrefs.edit()
        val isFirstTime = sharedPrefs.getBoolean(Constants().SP_FIRSTTIMEKEY, true)
        if(isFirstTime){
            showTutorial()
            sharedPrefsEditor.putBoolean(Constants().SP_FIRSTTIMEKEY, false)
            sharedPrefsEditor.apply()
            sharedPrefsEditor.commit()
        }

        myOrientationEventListener = object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
//                Log.v("ORIENTATION", "Orientation changed to $orientation")
                if(orientation >= 316 && orientation <= 45) {
                    // upright
                    camera_flash_button.rotation = 0F
                    camera_flip_button.rotation = 0F
                    zoomSlider.rotation = 0F
                    currOrientation = 0

                } else if (orientation in 46..135) {
                    // left edge up
                    camera_flash_button.rotation = 270F
                    camera_flip_button.rotation = 270F
                    zoomSlider.rotation = 180F
                    currOrientation = 90

                    val constraintLayout = findViewById<ConstraintLayout>(R.id.parentLayout)
                    val constraintSet = ConstraintSet()
                    constraintSet.clone(constraintLayout)
                    constraintSet.clear(R.id.cameraTipContainer, ConstraintSet.END)
                    constraintSet.clear(R.id.cameraTipContainer, ConstraintSet.TOP)
                    constraintSet.clear(R.id.cameraTipContainer, ConstraintSet.BOTTOM)
                    constraintSet.clear(R.id.cameraTipContainer, ConstraintSet.START)
                    constraintSet.connect(R.id.cameraTipContainer, ConstraintSet.START, R.id.viewFinder, ConstraintSet.START, 0)
                    constraintSet.connect(R.id.cameraTipContainer, ConstraintSet.TOP, R.id.viewFinder, ConstraintSet.TOP, 0)
                    constraintSet.connect(R.id.cameraTipContainer, ConstraintSet.BOTTOM, R.id.viewFinder, ConstraintSet.BOTTOM, 0)

                    constraintSet.applyTo(constraintLayout)

                    cameraTipContainer.rotation = 180F
                } else if (orientation in 136..225) {
                    // upside down
                    camera_flash_button.rotation = 180F
                    camera_flip_button.rotation = 180F
                    zoomSlider.rotation = 180F
                    currOrientation = 180

                } else if (orientation in 226..315) {
                    // right edge up
                    camera_flash_button.rotation = 90F
                    camera_flip_button.rotation = 90F
                    zoomSlider.rotation = 0F
                    currOrientation = 270
                    val constraintLayout = findViewById<ConstraintLayout>(R.id.parentLayout)
                    val constraintSet = ConstraintSet()
                    constraintSet.clone(constraintLayout)
                    constraintSet.clear(R.id.cameraTipContainer, ConstraintSet.END)
                    constraintSet.clear(R.id.cameraTipContainer, ConstraintSet.TOP)
                    constraintSet.clear(R.id.cameraTipContainer, ConstraintSet.BOTTOM)
                    constraintSet.clear(R.id.cameraTipContainer, ConstraintSet.START)

                    constraintSet.connect(R.id.cameraTipContainer, ConstraintSet.END, R.id.viewFinder, ConstraintSet.END, 0)
                    constraintSet.connect(R.id.cameraTipContainer, ConstraintSet.TOP, R.id.viewFinder, ConstraintSet.TOP, 0)
                    constraintSet.connect(R.id.cameraTipContainer, ConstraintSet.BOTTOM, R.id.viewFinder, ConstraintSet.BOTTOM, 0)

                    constraintSet.applyTo(constraintLayout)

                    cameraTipContainer.rotation = 0F
                } else{
                    // set default
                    camera_flash_button.rotation = 0F
                    camera_flip_button.rotation = 0F
                    zoomSlider.rotation = 0F
                    currOrientation = 0

                }
            }
        }
        if (myOrientationEventListener.canDetectOrientation()) {
            Log.v("ORIENTATION", "Can detect orientation")
            myOrientationEventListener.enable()
        } else {
            Log.v("ORIENTATION", "Cannot detect orientation")
            myOrientationEventListener.disable()
        }


        // Request camera permissions
        if (allPermissionsGranted()) {
            startVideoCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // set up zoom slider
        setUpZoomSlider()

        // Set up the listener for camera buttons
        camera_record_button.setOnClickListener { captureVideo() }

        camera_flash_button.setOnClickListener { toggleFlash() }

        camera_flip_button.setOnClickListener{ toggleFlip() }

//        overlayToggle.setOnCheckedChangeListener { buttonView, isChecked ->
//            if(!isChecked){
//                overlay.visibility = View.VISIBLE
//                cameraTip.text = "Please align your eyes with the guide."
//            }else{
//                overlay.visibility = View.INVISIBLE
//                cameraTip.text = ""
//            }
//        }

        //Set up listener for tap to focus
        viewFinder.setOnTouchListener { v, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener true
            }

            val meteringPointFactory = viewFinder.meteringPointFactory
            val meteringPoint = meteringPointFactory.createPoint(event.x, event.y)
            val focusAction = FocusMeteringAction.Builder(meteringPoint).build()
            camera.cameraControl.startFocusAndMetering(focusAction)
            v.performClick()

            val displayMetrics: DisplayMetrics = this.resources.displayMetrics
            val frameHeight = focusFrame.height
            val frameWidth = focusFrame.width
            val offset = ((Constants().CAMERA_FOCUS_OVERLAY_DIMEN/2) * displayMetrics.density).toInt()
            var topMargin = (frameHeight * meteringPoint.x).toInt() - offset
            var leftMargin = (frameWidth - (frameWidth * meteringPoint.y).toInt()) - offset

            if((topMargin + (offset*2)) > frameHeight){
                topMargin = frameHeight - (offset*2)
            }
            if (topMargin < 0){
                topMargin = 0
            }
            if((leftMargin + (offset*2)) > frameWidth){
                leftMargin = frameWidth - (offset*2)
            }
            if(leftMargin <0) {
                leftMargin = 0
            }


            val marginParams = ViewGroup.MarginLayoutParams(focusImg.layoutParams)
            marginParams.setMargins(leftMargin, topMargin, 0, 0)
            val layoutParams = RelativeLayout.LayoutParams(marginParams)
            focusImg.layoutParams = layoutParams
            focusImg.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                focusImg.visibility = View.INVISIBLE
            }, 1500)

            Log.w("Click", "$offset $frameHeight $frameWidth ${event.x} ${event.y} ${topMargin} ${leftMargin}")
            return@setOnTouchListener true
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        activityCreated = true

    }

    override fun onResume() {
        super.onResume()
        if (activityCreated) {
            startVideoCamera()
        }

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_toolbar_camera_menu, menu)
        return true
    }

    private fun showTutorial(){
        val tutorialDialog = VideoCameraTutorialFragment()
        tutorialDialog.show(supportFragmentManager, "tutorialDialog")
    }

    private fun showSavePreview(uri: Uri){
        val intent = Intent(this, SaveVideoActivity::class.java)
        intent.putExtra("video", uri.toString())
        startActivity(intent)
    }
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap{
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    }
    private fun setUpZoomSlider() {
        zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.w("ZOOM", "ZOOM: $progress")
                camera.cameraControl.setLinearZoom(progress / 100.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    private fun toggleFlip() {
        Log.w("Flip", "Flipped Camera")
        when (lensFace) {
            CameraSelector.DEFAULT_BACK_CAMERA -> {
                lensFace = CameraSelector.DEFAULT_FRONT_CAMERA
            }
            CameraSelector.DEFAULT_FRONT_CAMERA -> {
                lensFace = CameraSelector.DEFAULT_BACK_CAMERA
            }
        }
        try{
            startVideoCamera()
        }catch (exc: Exception){
            Log.e("Error", "Failed to restart camera bro.")
        }
    }
    private fun toggleFlash() {
        //val videoCapture = videoCapture ?: return
        if (!hasFlash) {
            Log.w("Log", "Flash NOT Toggled")
            return
        }
        Log.w("Log", "Flash Toggled")
        when (camera.cameraInfo.torchState.value) {
            TorchState.OFF -> {
                camera_flash_button.setBackgroundResource(R.drawable.camera_flash_on)
                camera.cameraControl.enableTorch(true)
            }
            TorchState.ON -> {
                // "AUTO" means Torch ON
                camera_flash_button.setBackgroundResource(R.drawable.camera_flash_off)
                camera.cameraControl.enableTorch(false)
            }
        }
    }
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int) :Bitmap {
        val matrix = Matrix()
        matrix.preRotate(degrees.toFloat())
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

    }
    private fun startVideoCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            camera_flash_button.setBackgroundResource(R.drawable.camera_flash_on)

            // Select back camera as a default
            val cameraSelector = lensFace

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )
                //set camera to be NOT initially zoomed in
                camera.cameraControl.setLinearZoom(0f)

                hasFlash = camera.cameraInfo.hasFlashUnit()
                preview.setSurfaceProvider(viewFinder.surfaceProvider)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun generateVidTitle(): String {
        val simpleDateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss")
        val date = simpleDateFormat.format(Date())
        return "Recording - $date"
    }

    private fun startVideoTimer() {
        Handler(Looper.getMainLooper()).postDelayed({
            captureVideo()//Do something after 100ms
        }, 11000)
    }

    private fun captureVideo() {
        val videoCapture = videoCapture ?: return
//        open_video_mode_button.isEnabled = false
        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            camera_record_button.setBackgroundResource(R.drawable.video_capture)
            camera_record_button.isEnabled = true
            camera_record_button.isVisible = true
            recording_prompt.isVisible = false
            return
        }
        startVideoTimer()
        camera_record_button.isEnabled = false
        camera_record_button.setBackgroundResource(R.drawable.camera_stop_recording)
        camera_record_button.isVisible = false
        recording_prompt.isVisible = true
        // create and start a new recording session
//        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
//            .format(System.currentTimeMillis())
        val name = generateVidTitle()
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ClarifEYE-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
//                        open_video_mode_button.apply {
//                            text = getString(R.string.stop_capture)
//                            isEnabled = true
//                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val uri = recordEvent.outputResults.outputUri
                            val msg = "Video capture succeeded: " +
                                    "${uri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                            //converting the MediaStore URI to a normal URI
                            val f = File(getFilePath(this, uri))
                            val uriFinal = Uri.fromFile(f)
                            showSavePreview(uriFinal)
//                            CoroutineScope(Dispatchers.IO).launch{
//                                save()
//                            }
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
//                        open_video_mode_button.apply {
//                            text = getString(R.string.start_capture)
//                            isEnabled = true
//                        }
                    }
                }
            }
    }


    //the following code is for getting the actual file path (the external storage path) of the MediaStore URI
    @Throws(URISyntaxException::class)
    open fun getFilePath(context: Context, uri: Uri): String? {
        var uri = uri
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(
                context.applicationContext,
                uri
            )
        ) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                uri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
                )
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("image" == type) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                selection = "_id=?"
                selectionArgs = arrayOf(
                    split[1]
                )
            }
        }
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            if (isGooglePhotosUri(uri)) {
                return uri.lastPathSegment
            }
            val projection = arrayOf(
                MediaStore.Images.Media.DATA
            )
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver
                    .query(uri, projection, selection, selectionArgs, null)
                val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startVideoCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            println("COMPRESS DONE....")
            out.flush()

        }
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            captureVideo()
            true
        }  else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            captureVideo()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
}