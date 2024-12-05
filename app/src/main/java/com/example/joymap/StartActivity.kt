package com.example.joymap

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.joymap.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnMap.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.btnChildren.setOnClickListener {
            startActivity(Intent(this, ChildrenActivity::class.java))
        }
    }
}
