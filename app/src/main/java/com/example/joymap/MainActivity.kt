package com.example.joymap

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.RadioButton
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
    // Инициализация переменных для работы с картой и зонами
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapObjectCollection: MapObjectCollection
    private var currentCircle: CircleMapObject? = null
    private var startPoint: Point? = null
    private var zoneName: String = ""
    private var zoneColor: Int = Color.argb(128, 0, 0, 255)
    private var zoneRadius: Float = 1.0f // Радиус по умолчанию в километрах
    private var isCreatingZone: Boolean = false
    private val safeZones = mutableListOf<CircleMapObject>()
    private var selectedZone: CircleMapObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setApiKey(savedInstanceState)
        MapKitFactory.initialize(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        moveToStartLocation()
        binding.mapview.map.addCameraListener(this)
        mapObjectCollection = binding.mapview.map.mapObjects

        // Настройка кнопки для добавления безопасной зоны
        val addSafeZoneButton: Button = findViewById(R.id.add_safe_zone_button)
        addSafeZoneButton.setOnClickListener {
            isCreatingZone = true
            showZoneDialog(null)
        }

        // Настройка кнопки для отображения списка зон
        val showZonesButton: Button = findViewById(R.id.btn_show_zones)
        showZonesButton.setOnClickListener {
            showZonesListDialog()
        }

        // Добавление слушателей для взаимодействия с картой
        binding.mapview.map.addInputListener(inputListener)
        binding.mapview.map.addTapListener(geoObjectTapListener)
    }

    // Слушатель для обработки нажатий на карту
    private val inputListener = object : InputListener {
        override fun onMapTap(map: Map, point: Point) {
            if (isCreatingZone && startPoint == null) {
                startPoint = point
                drawCircle(startPoint!!, zoneRadius * 1000) // Преобразование километров в метры
                startPoint = null
                isCreatingZone = false
            }
        }

        override fun onMapLongTap(map: Map, point: Point) {}
    }

    // Слушатель для обработки нажатий на объекты карты
    private val geoObjectTapListener = GeoObjectTapListener { geoObjectTapEvent ->
        val mapObject = geoObjectTapEvent.geoObject
        if (mapObject is CircleMapObject) {
            showDeleteZoneDialog(mapObject)
        }
        true
    }

    // Функция для рисования зоны на карте
    private fun drawCircle(center: Point, radius: Float) {
        val circle = Circle(center, radius)
        currentCircle = mapObjectCollection.addCircle(
            circle,
            zoneColor,
            2f,
            zoneColor
        )
        currentCircle?.userData = zoneName
        safeZones.add(currentCircle!!)
    }

    // Функция для отображения диалога ввода параметров зоны
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
            radiusInput.setText((circle.geometry.radius / 1000).toString()) // Преобразование метров в километры
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

    // Функция для отображения диалога подтверждения удаления зоны
    private fun showDeleteZoneDialog(circle: CircleMapObject) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Удалить зону")
        builder.setMessage("Вы действительно хотите удалить эту зону?")
        builder.setPositiveButton("Удалить") { _, _ ->
            safeZones.remove(circle)
            mapObjectCollection.remove(circle)
        }
        builder.setNegativeButton("Отмена") { _, _ -> }
        builder.show()
    }

    // Функция для обновления параметров зоны
    private fun updateCircle(circle: CircleMapObject, name: String, color: Int, radius: Float) {
        circle.geometry = Circle(circle.geometry.center, radius * 1000) // Преобразование километров в метры
        circle.strokeColor = color
        circle.fillColor = color
        circle.userData = name
    }

    // Функция для перемещения камеры на начальную позицию
    private fun moveToStartLocation() {
        val startLocation = Point(59.9402, 30.315)
        val zoomValue: Float = 16.5f
        binding.mapview.map.move(
            CameraPosition(startLocation, zoomValue, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 2f),
            null
        )
    }

    // Функция для установки API-ключа
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

    // Функция для отображения диалога со списком зон
    private fun showZonesListDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Список безопасных зон")

        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_safe_zones_list, null)
        builder.setView(dialogLayout)

        val listView: ListView = dialogLayout.findViewById(R.id.list_safe_zones)
        val adapter = SafeZoneAdapter(this, safeZones)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, view, position, _ ->
            val radioButton: RadioButton = view.findViewById(R.id.radio_button_zone)
            radioButton.isChecked = true
            selectedZone = safeZones[position]
        }

        builder.setPositiveButton("OK") { _, _ -> }
        builder.setNeutralButton("УДАЛИТЬ") { _, _ ->
            selectedZone?.let {
                safeZones.remove(it)
                mapObjectCollection.remove(it)
                selectedZone = null
                adapter.notifyDataSetChanged()
            }
        }
        builder.show()
    }

    companion object {
        const val MAPKIT_API_KEY = "6ed44a4b-6543-4064-bebd-3029ebe6a1b9"
    }
}
