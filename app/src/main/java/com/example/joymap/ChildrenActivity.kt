package com.example.joymap

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.joymap.databinding.ActivityChildrenBinding

class ChildrenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChildrenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildrenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScanQR.setOnClickListener {
            val firstPart = binding.editTextFirstPart.text.toString()
            val lastPart = binding.editTextLastPart.text.toString()
            if (firstPart.isNotEmpty() && lastPart.isNotEmpty()) {
                // Здесь будет реализован функционал для подключения ребенка
                Toast.makeText(this, "Ребенок подключен", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Введите обе части UUID", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
