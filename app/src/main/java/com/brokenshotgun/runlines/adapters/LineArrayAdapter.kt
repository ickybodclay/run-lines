package com.brokenshotgun.runlines.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.brokenshotgun.runlines.R
import com.brokenshotgun.runlines.model.Actor
import com.brokenshotgun.runlines.model.Line
import java.util.Random

class LineArrayAdapter(context: Context, lines: List<Line>) : ArrayAdapter<Line>(context, R.layout.item_line, lines) {
    private var selectedItem = -1
    private val random = Random()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(getContext()).inflate(R.layout.item_line, parent, false).apply {
            tag = ViewHolder().apply {
                nameText = findViewById(R.id.name)
                lineText = findViewById(R.id.line)
            }
        }

        val viewHolder = view.tag as ViewHolder
        val line = getItem(position)

        var enabled = false
        if (line != null) {
            val name = StringBuilder(line.actor.name).apply {
                if (line.characterExtensions.isNotEmpty()) {
                    append(" ")
                    line.characterExtensions.forEach { append(it) }
                }
            }.toString()

            if (Actor.ACTION_NAME == name) {
                viewHolder.nameText.visibility = View.GONE
            } else {
                viewHolder.nameText.visibility = View.VISIBLE
            }

            viewHolder.nameText.text = name
            viewHolder.lineText.text = Html.fromHtml(line.getLineHtml(), 0)
            enabled = line.enabled
        }

        highlightItem(position, enabled, view)

        return view
    }

    private fun highlightItem(position: Int, enabled: Boolean, result: View) {
        if (!enabled) {
            setItemBackground(result, ContextCompat.getDrawable(getContext(), R.drawable.hidden_line_background))
        } else if (position == selectedItem) {
            val line = getItem(position)
            if (line != null) {
                result.setBackgroundColor(colorFromUsername(line.actor.name))
            }
        } else {
            setItemBackground(result, null)
        }
    }

    private fun setItemBackground(result: View, drawable: Drawable?) {
        result.background = drawable
    }

    private fun colorFromUsername(name: String): Int {
        random.setSeed(name.hashCode().toLong())
        val r = (random.nextInt(100) + 128)
        val g = (random.nextInt(100) + 128)
        val b = (random.nextInt(100) + 128)
        return Color.rgb(r, g, b)
    }

    fun setSelectedItem(selectedItem: Int) {
        this.selectedItem = selectedItem
    }

    private class ViewHolder {
        lateinit var nameText: TextView
        lateinit var lineText: TextView
    }
}
