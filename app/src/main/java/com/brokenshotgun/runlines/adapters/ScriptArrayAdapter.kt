package com.brokenshotgun.runlines.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.brokenshotgun.runlines.R
import com.brokenshotgun.runlines.model.Script

class ScriptArrayAdapter(context: Context, objects: List<Script>) : ArrayAdapter<Script>(context, R.layout.item_script, objects) {
    private val context: Context = context

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(getContext()).inflate(R.layout.item_script, parent, false).apply {
            tag = ViewHolder().apply {
                nameText = findViewById(R.id.name)
            }
        }

        val viewHolder = view.tag as ViewHolder
        val script = getItem(position)

        if (script != null) {
            val name = if (script.name.isEmpty()) "Untitled script" else script.name
            viewHolder.nameText.text = name
        }

        return view
    }

    private class ViewHolder {
        lateinit var nameText: TextView
    }
}
