package com.example.joymap

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.TextView
import com.yandex.mapkit.map.CircleMapObject

class SafeZoneAdapter(context: Context, private val safeZones: List<CircleMapObject>) : ArrayAdapter<CircleMapObject>(context, 0, safeZones) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.dialog_safe_zone_item, parent, false)

        val zone = safeZones[position]
        val zoneNameTextView = view.findViewById<TextView>(R.id.zone_name_item)
        val radioButton = view.findViewById<RadioButton>(R.id.radio_button_zone)

        zoneNameTextView?.text = zone.userData as? String ?: ""
        radioButton?.isChecked = false

        return view
    }
}
