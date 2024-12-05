package com.example.joymap

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.joymap.databinding.ActivityMainBinding
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.GeoObjectTapEvent
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.CircleMapObject
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection

class MainActivity : AppCompatActivity(), CameraListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapObjectCollection: MapObjectCollection
    private var currentCircle: CircleMapObject? = null
    private var startPoint: Point? = null
    private var zoneName: String = ""
    private var zoneColor: Int = Color.argb(128, 0, 0, 255)
    private var zoneRadius: Float = 1.0f // Default radius in kilometers
    private var isCreatingZone: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setApiKey(savedInstanceState)
        MapKitFactory.initialize(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        moveToStartLocation()
        binding.mapview.map.addCameraListener(this)
        mapObjectCollection = binding.mapview.map.mapObjects

        val addSafeZoneButton: Button = findViewById(R.id.add_safe_zone_button)
        addSafeZoneButton.setOnClickListener {
            isCreatingZone = true
            showZoneDialog(null)
        }

        binding.mapview.map.addInputListener(inputListener)
        binding.mapview.map.addTapListener(geoObjectTapListener)
    }

    private val inputListener = object : InputListener {
        override fun onMapTap(map: Map, point: Point) {
            if (isCreatingZone && startPoint == null) {
                startPoint = point
                drawCircle(startPoint!!, zoneRadius * 1000) // Convert kilometers to meters
                startPoint = null
                isCreatingZone = false
            }
        }

        override fun onMapLongTap(map: Map, point: Point) {}
    }

    private val geoObjectTapListener = GeoObjectTapListener { geoObjectTapEvent ->
        val mapObject = geoObjectTapEvent.geoObject
        if (mapObject is CircleMapObject) {
            showZoneInfoDialog(mapObject)
        }
        true
    }

    private fun drawCircle(center: Point, radius: Float) {
        val circle = Circle(center, radius)
        currentCircle = mapObjectCollection.addCircle(
            circle,
            zoneColor,
            2f,
            zoneColor
        )
        currentCircle?.userData = zoneName
    }

    private fun showZoneDialog(circle: CircleMapObject?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Введите параметры зоны")

        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_safe_zone, null)
        builder.setView(dialogLayout)

        val nameInput: EditText = dialogLayout.findViewById(R.id.zone_name)
        val radiusInput: EditText = dialogLayout.findViewById(R.id.zone_radius)
        val colorSpinner: Spinner = dialogLayout.findViewById(R.id.zone_color)

        val colors = arrayOf("Красный", "Синий", "Зеленый")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorSpinner.adapter = adapter

        if (circle != null) {
            nameInput.setText(circle.userData as? String ?: "")
            radiusInput.setText((circle.geometry.radius / 1000).toString()) // Convert meters to kilometers
            colorSpinner.setSelection(when (circle.strokeColor) {
                Color.argb(128, 255, 0, 0) -> 0
                Color.argb(128, 0, 0, 255) -> 1
                Color.argb(128, 0, 255, 0) -> 2
                else -> 0
            })
        }

        builder.setPositiveButton("OK") { _, _ ->
            zoneName = nameInput.text.toString()
            zoneRadius = radiusInput.text.toString().toFloat()
            zoneColor = when (colorSpinner.selectedItemPosition) {
                0 -> Color.argb(128, 255, 0, 0)
                1 -> Color.argb(128, 0, 0, 255)
                2 -> Color.argb(128, 0, 255, 0)
                else -> Color.argb(128, 0, 0, 255)
            }
            if (circle == null) {
                Toast.makeText(this, "Выберите центр зоны на карте", Toast.LENGTH_SHORT).show()
            } else {
                updateCircle(circle, zoneName, zoneColor, zoneRadius)
            }
        }
        builder.setNegativeButton("Отмена") { _, _ ->
            if (circle == null) {
                currentCircle?.let { mapObjectCollection.remove(it) }
            }
        }

        builder.show()
    }

    private fun showZoneInfoDialog(circle: CircleMapObject) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Информация о зоне")

        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_safe_zone_info, null)
        builder.setView(dialogLayout)

        val nameText: TextView = dialogLayout.findViewById(R.id.zone_name_info)
        val radiusText: TextView = dialogLayout.findViewById(R.id.zone_radius_info)
        val colorText: TextView = dialogLayout.findViewById(R.id.zone_color_info)

        nameText.text = circle.userData as? String ?: ""
        radiusText.text = (circle.geometry.radius / 1000).toString() // Convert meters to kilometers
        colorText.text = when (circle.strokeColor) {
            Color.argb(128, 255, 0, 0) -> "Красный"
            Color.argb(128, 0, 0, 255) -> "Синий"
            Color.argb(128, 0, 255, 0) -> "Зеленый"
            else -> "Неизвестный"
        }

        builder.setPositiveButton("Изменить") { _, _ ->
            showZoneDialog(circle)
        }
        builder.setNegativeButton("Отмена") { _, _ -> }

        builder.show()
    }

    private fun updateCircle(circle: CircleMapObject, name: String, color: Int, radius: Float) {
        circle.geometry = Circle(circle.geometry.center, radius * 1000) // Convert kilometers to meters
        circle.strokeColor = color
        circle.fillColor = color
        circle.userData = name
    }

    private fun moveToStartLocation() {
        val startLocation = Point(59.9402, 30.315)
        val zoomValue: Float = 16.5f
        binding.mapview.map.move(
            CameraPosition(startLocation, zoomValue, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 2f),
            null
        )
    }

    private fun setApiKey(savedInstanceState: Bundle?) {
        val haveApiKey = savedInstanceState?.getBoolean("haveApiKey") ?: false
        if (!haveApiKey) {
            MapKitFactory.setApiKey(MAPKIT_API_KEY)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("haveApiKey", true)
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapview.onStart()
    }

    override fun onStop() {
        binding.mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onCameraPositionChanged(
        map: Map,
        cameraPosition: CameraPosition,
        cameraUpdateReason: CameraUpdateReason,
        finished: Boolean
    ) {
        // Обработка изменения позиции камеры
    }

    companion object {
        const val MAPKIT_API_KEY = "6ed44a4b-6543-4064-bebd-3029ebe6a1b9"
    }
}
