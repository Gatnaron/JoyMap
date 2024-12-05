package com.example.joymap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.joymap.databinding.ActivityChildrenBinding

class ChildrenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChildrenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildrenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Здесь будет реализован функционал для сканирования QR-кода и отображения списка детей
    }
}
