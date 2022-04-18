package com.example.eyephone

import android.R.attr
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.video.Video
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.R.attr.path
import android.content.Intent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.myToolbar
import android.media.ThumbnailUtils

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_video_list.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ViewVideosActivity : AppCompatActivity(), VideoRecyclerViewInterface {
    var videoList = ArrayList<VideoObject>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)
        setSupportActionBar(myToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        CoroutineScope(Dispatchers.IO).launch {
//            //initializeHomeScreen()
//        }

        val recyclerView : RecyclerView = findViewById(R.id.videoRecyclerView)
        //checkIntent is for displaying snackbar after sharing or deleting, and then updating the videoList to reflect the deleted item
        checkIntent(intent, videoListView, recyclerView)
        setUpVideoObjects()

        //set up the adapter AFTER setting up the Video Objects (bc we need data first)

        val adapter = VideoRecyclerViewAdapter(this, videoList,this)
        //connect the adapter to the Recycler View

        recyclerView.adapter = adapter

        //set up the LinearLayoutManager
        recyclerView.layoutManager = LinearLayoutManager(this)

    }
    private fun setUpVideoObjects() {
        //dir refers to the path of the external storage where VideoRecorder stores the videos
        val dir = File(Environment.getExternalStorageDirectory().absolutePath + "/Movies/ClarifEYE-Video")
        println("ExtStoragePath is $dir")
        //checking if directory exists
        if (dir.isDirectory) {
            //checking if that directory has files in it
            val files = dir.listFiles()
            if (files != null) {
                for (i in files.indices) {
                    Log.d("Files", "FileName:" + files[i].name + " Filepath:" + files[i].path)
                    val vidContent = VideoObject(files[i].name, Date(files[i].lastModified()).toString(), files[i].path)
                    videoList.add(vidContent)
                }
            }
        }
        //sort the video objects so that the most recently made video pops up on top
        videoList.sortByDescending { it.fileDate }
    }

    private suspend fun updateVideoList(action:String, identity: String) = withContext(Dispatchers.IO) {
        if (action == "add") {
            val dir = File(Environment.getExternalStorageDirectory().absolutePath + "/Movies/ClarifEYE-Video")
            if (dir.isDirectory) {
                //checking if that directory has files in it
                val files = dir.listFiles()
                if (files != null) {
                    for (i in files.indices) {
                        if (files[i].name == identity) {
                            val vidContent = VideoObject(
                                files[i].name,
                                Date(files[i].lastModified()).toString(),
                                files[i].path
                            )
                            videoList.add(vidContent)
                            break
                        }
                    }
                }
            }
            println("Debug: Finished adding new file")
        } else if (action == "delete") {
            for (vid in videoList) {
                if (vid.fileName == identity) {
                    videoList.remove(vid)
                    break
                }
            }
            println("Debug: Finished removing file")
        }
    }

    private fun checkIntent(intent: Intent, view: View, rec : RecyclerView){
        val extras = intent.extras
        println("Reached here!")
        if (extras!=null){
            println("There are extras")
            if (extras.getBoolean("showSnackbar")){
                val text = extras.getString("snackbarText")
                if (text != null){
                    Snackbar.make(rec, text, Snackbar.LENGTH_SHORT)
                        .show()
                }
            }

            val action = extras.getString("action").toString()
            if (action =="add" || action == "delete"){
                val identity = extras.getString("identity") //If "add" -> identity=filename, If "delete" -> identity=alias
                CoroutineScope(Dispatchers.IO).launch {
                    if (identity != null) {
                        println("Identity is $identity")
                        updateVideoList(action, identity)
                    }
                }
            }
        }
    }

    override fun onItemClick(position: Int) {
        //clear cache
        this.cacheDir.deleteRecursively()
        //prepare intent to send to savevideoactivity (view the video)
        val intent = Intent(this, SaveVideoActivity::class.java)
        //val intent = Intent(this, ShareVideoActivity::class.java)
        val uri = Uri.fromFile(File(videoList.get(position).filePath)).toString()
        println("The uri of this video is: $uri")
        intent.putExtra("video", uri)
        startActivity(intent)
    }
}