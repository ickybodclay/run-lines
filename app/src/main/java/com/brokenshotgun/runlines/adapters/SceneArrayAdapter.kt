package com.brokenshotgun.runlines.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.brokenshotgun.runlines.R
import com.brokenshotgun.runlines.model.Scene

class SceneArrayAdapter(context: Context, objects: List<Scene>) : ArrayAdapter<Scene>(context, R.layout.item_script, objects) {
    private val context: Context = context

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(getContext()).inflate(R.layout.item_scene, parent, false).apply {
            tag = ViewHolder().apply {
                nameText = findViewById(R.id.name)
            }
        }

        val viewHolder = view.tag as ViewHolder
        val scene = getItem(position)

        if (scene != null) {
            viewHolder.nameText.text = if (scene.name.isNullOrEmpty()) context.getString(R.string.label_no_scene_name) else scene.name
        }

        return view
    }

    private class ViewHolder {
        lateinit var nameText: TextView
    }
}
