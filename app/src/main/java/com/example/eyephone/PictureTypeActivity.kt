package com.example.eyephone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_picture_type.*

class PictureTypeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_type)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // set on click listeners for buttons

        btn_single.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("Type", Constants().CAPTURE_TYPE_SINGLE)
            startActivity(intent);
        }
        btn_both.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("Type", Constants().CAPTURE_TYPE_BOTH)
            startActivity(intent);
        }
        btn_multi.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("Type", Constants().CAPTURE_TYPE_MULTI)
            startActivity(intent);
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}