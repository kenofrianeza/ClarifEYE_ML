package com.example.eyephone

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

class CameraActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private val INITIAL_ZOOM = 0.75f
    private lateinit var camera: Camera
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var lensFace = CameraSelector.DEFAULT_BACK_CAMERA
    private var hasFlash: Boolean = true
    private var activityCreated : Boolean = false
    private var showOverlay: Boolean = true
    private var currOrientation = 0
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
                cameraTip.text = "Please align your eyes with the guide."
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
        setContentView(R.layout.activity_camera)
//        supportActionBar?.hide()
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setSupportActionBar(myToolbar)

        window.navigationBarColor = resources.getColor(R.color.black);
        overlay.setImageResource(R.drawable.overlay_single)
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
            Log.v("ORIENTATION", "Can detect orientation");
            myOrientationEventListener.enable();
        } else {
            Log.v("ORIENTATION", "Cannot detect orientation");
            myOrientationEventListener.disable();
        }


        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // set up zoom slider
        setUpZoomSlider()

        // Set up the listener for camera buttons
        camera_capture_button.setOnClickListener { takePhoto() }

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
            startCamera()
        }

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_toolbar_camera_menu, menu);
        return true
    }

    private fun showTutorial(){
        val tutorialDialog = CameraTutorialFragment()
        tutorialDialog.show(supportFragmentManager, "tutorialDialog")
    }

    private fun showSavePreview(uri: Uri, labels: List<ImageLabel>){
        println("URI: $uri")
        println("These are the labels: $labels")
        val intent = Intent(this, SaveImageActivity::class.java)
        intent.putExtra("picture", uri.toString());
        intent.putExtra("labels", labels.toString())
        startActivity(intent);
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
            startCamera()
        }catch (exc: Exception){
            Log.e("Error", "Failed to restart camera bro.")
        }
    }
    private fun toggleFlash() {
        val imageCapture = imageCapture ?: return
        if (!hasFlash) {
            Log.w("Log", "Flash NOT Toggled")
            return
        }
        Log.w("Log", "Flash Toggled")
        when (imageCapture.flashMode) {
            ImageCapture.FLASH_MODE_OFF -> {
                imageCapture.flashMode = ImageCapture.FLASH_MODE_ON
                camera_flash_button.setBackgroundResource(R.drawable.camera_flash_on)
            }
            ImageCapture.FLASH_MODE_ON -> {
                // "AUTO" means Torch ON
                imageCapture.flashMode = ImageCapture.FLASH_MODE_AUTO
                camera_flash_button.setBackgroundResource(R.drawable.camera_torch)
                camera.cameraControl.enableTorch(true)
            }
            ImageCapture.FLASH_MODE_AUTO -> {
                imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
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

    private fun createImageProcessor() {
        //declaring objects for image labeling
        //builds the model file
        val localModel = LocalModel.Builder()
            .setAssetFilePath("assets/eyemodel.tflite")
            .build()
        //builds ImageLabelerOptions object for configuration for labeling the image, showing only labels with minimum 50% confidence level and 5 max displayed labels
        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.5f)
            .setMaxResultCount(5)
            .build()
        //build ImageLabeler object with previous configurations
        val labeler = ImageLabeling.getClient(customImageLabelerOptions)
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return


        imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageCapturedCallback() {


                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        //deg holds the value of the image's rotation degree
                        var deg: Int
                        //calculates deg if back camera is used
                        if (lensFace == CameraSelector.DEFAULT_BACK_CAMERA) {
                            deg = imageProxy.imageInfo.rotationDegrees + currOrientation
                        //calculates deg if front camera is used
                        } else {
                            deg = when (currOrientation){
                                90 -> imageProxy.imageInfo.rotationDegrees + 270
                                270 -> imageProxy.imageInfo.rotationDegrees + 90
                                else -> {
                                    imageProxy.imageInfo.rotationDegrees + currOrientation
                                }
                            }

                        }

                        println("ROTATION: $deg")
                        //convert image to bitmap
                        val bitmap = imageProxyToBitmap(imageProxy)
                        println("IMAGE PROXY TO BITMAP DONE...")
                        //rotate bitmap
                        val rotatedBitmap = rotateBitmap(bitmap, deg)

                        //convert bitmap to byte array
//                    val stream = ByteArrayOutputStream()
//                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 20, stream)
//                    Log.w("GGGG", "GOT HERE")
//                    val byteArr: ByteArray = stream.toByteArray()

                        //save bitmap in temp directory
                        val outputDir: File =
                                this@CameraActivity.cacheDir // context being the Activity pointer
                        File(outputDir, "temp.jpg").writeBitmap(
                                rotatedBitmap,
                                Bitmap.CompressFormat.JPEG,
                                100
                        )
                        println("FILE WRITE DONE...")

                        //code block for sending image to imagelabeling object
//                        val imgAnalyzer = EyeImageAnalyzer()
//                        imgAnalyzer.analyze(imageProxy)
//                        println("Image is analyzed!")

                        //declaring objects for image labeling
                        //builds the model file
                        val localModel = LocalModel.Builder()
                            .setAssetFilePath("eyemodel2.tflite")
                            .build()
                        //builds ImageLabelerOptions object for configuration for labeling the image, showing only labels with minimum 50% confidence level and 5 max displayed labels
                        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
                            .setConfidenceThreshold(0.5f)
                            .setMaxResultCount(5)
                            .build()
                        //build ImageLabeler object with previous configurations
                        val labeler = ImageLabeling.getClient(customImageLabelerOptions)

                        fun analyze(imageProxy: ImageProxy) {
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                labeler.process(image)
                                    .addOnSuccessListener { labels ->
                                        for (label in labels) {
                                            val text = label.text
                                            val confidence = label.confidence
                                            val index = label.index
                                            println("text: $text")
                                            println("confidence: $confidence")
                                            println("index: $index")
                                        }
                                        // send image to new activity with intent
                                        showSavePreview(Uri.fromFile(File("$outputDir/temp.jpg")), labels)
                                        println("Image processing was successful!")
                                    }
                                    .addOnFailureListener { e ->
                                        println("Image processing failed!")
                                    }
                            }
                        }
                        analyze(imageProxy)
                        //close the image
                        imageProxy.close()
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                })

        // use below if you want to save image immediately after taking photo. works as of 09/11/2020
/*
        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                val msg = "Photo capture succeeded: $savedUri"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, msg)
            }
        })

 */
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()

            imageCapture = ImageCapture.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetResolution(Size(1080, 1440))
                    .setFlashMode(ImageCapture.FLASH_MODE_ON)
                    .build()

            camera_flash_button.setBackgroundResource(R.drawable.camera_flash_on)

            // Select back camera as a default
            val cameraSelector = lensFace

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                )
                //set camera to be initially zoomed in
                camera.cameraControl.setLinearZoom(INITIAL_ZOOM)

                hasFlash = camera.cameraInfo.hasFlashUnit()
                preview.setSurfaceProvider(viewFinder.surfaceProvider)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
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
            takePhoto()
            true
        }  else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            takePhoto()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
}