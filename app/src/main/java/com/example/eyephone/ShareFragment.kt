package com.example.eyephone

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.share_bottom_sheet.*
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.OutputStream


class ShareFragment: BottomSheetDialogFragment() {
    lateinit var uriString: String
    lateinit var filepath: String
    lateinit var photoUri: Uri
    lateinit var imgTitle: String
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.share_bottom_sheet, container, false)
    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val uri = arguments?.getString("uri")
        val path = arguments?.getString("filepath")
        val myPhotoUri = arguments?.getParcelable<Uri>("photoUri")
        val title = arguments?.getString("title")
        if (uri != null) {
            uriString = uri
        }
        if (path != null) {
            filepath = path
        }
        if (myPhotoUri != null) {
            photoUri = myPhotoUri
        }
        if (title != null) {
            imgTitle = title
        }
        img_title.text = imgTitle
        imgPreview.setImageURI(Uri.parse(uriString))


        protonmailBtn.setOnClickListener {
            val appPackageName = "ch.protonmail.android" // getPackageName() from Context or Activity object
            val isInstalled = activity?.let { it1 -> isPackageInstalled(appPackageName, it1.packageManager) }
            if (isInstalled == true){
                val intent = Intent(Intent.ACTION_SEND).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, photoUri)
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
                    type = "image/jpeg"
                    setPackage(appPackageName)
                }
                startActivity(intent)
            }else{
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
                } catch (e: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                }
            }
        }
        tutonotaBtn.setOnClickListener {
            val appPackageName = "de.tutao.tutanota" // getPackageName() from Context or Activity object
            val isInstalled = activity?.let { it1 -> isPackageInstalled(appPackageName, it1.packageManager) }
            if (isInstalled == true){
                val intent = Intent(Intent.ACTION_SEND).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, photoUri)
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
                    type = "image/jpeg"
                    setPackage(appPackageName)
                }
                startActivity(intent)
            }else{
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
                } catch (e: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                }
            }
        }
        signalBtn.setOnClickListener {
            val appPackageName = "org.thoughtcrime.securesms" // getPackageName() from Context or Activity object
            val isInstalled = activity?.let { it1 -> isPackageInstalled(appPackageName, it1.packageManager) }
            if (isInstalled == true) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, photoUri)
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
                    type = "image/jpeg"
                    setPackage(appPackageName)
                }
                startActivity(intent)
            }else {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
                } catch (e: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                }
            }
        }

        saveToGalleryBtn.setOnClickListener{
            val bitmap = BitmapFactory.decodeFile(filepath)
            context?.let { it1 -> bitmap.saveImage(it1) }
            dismiss()
            val content = activity?.findViewById<View>(android.R.id.content)
            if (content != null) {
                Snackbar.make(content, "Image exported to gallery.", Snackbar.LENGTH_SHORT)
                        .show()
            }
        }

       emailBtn.setOnClickListener {
           val emailIntent = Intent(Intent.ACTION_SENDTO)
           val intent = Intent(Intent.ACTION_SEND)

           intent.putExtra(Intent.EXTRA_STREAM, photoUri)
           emailIntent.data = Uri.parse("mailto:")
           intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_email_subject))
           intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text))
           intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
           intent.selector = emailIntent
           try {
               startActivity(Intent.createChooser(intent, "Send via..."))
           } catch (e: java.lang.Exception) {
           }

       }
        moreBtn.setOnClickListener{
            val intent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, photoUri)
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
                type = "image/jpeg"
            }
            val chooser = (Intent.createChooser(intent, "Share Image..."))
            startActivity(chooser)
        }
    }

    fun Bitmap.saveImage(context: Context): Uri? {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eyephone_pictures")
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            values.put(MediaStore.Images.Media.DISPLAY_NAME, imgTitle)

            val uri: Uri? =
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                saveImageToStream(this, context.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                context.contentResolver.update(uri, values, null, null)
                return uri
            }
        } else {
            val directory =
                    File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + separator + "test_pictures")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName =  "img_${SystemClock.uptimeMillis()}"+ ".jpeg"
            val file = File(directory, fileName)
            saveImageToStream(this, FileOutputStream(file))
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            // .DATA is deprecated in API 29
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            return Uri.fromFile(file)
        }
        return null
    }
    fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}