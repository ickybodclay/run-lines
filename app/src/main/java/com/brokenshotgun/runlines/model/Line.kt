package com.brokenshotgun.runlines.model

import android.os.Parcel
import android.os.Parcelable
import java.util.regex.Pattern
import kotlin.jvm.JvmOverloads

data class Line @JvmOverloads constructor(
    var actor: Actor,
    var line: String,
    var order: Int = 0,
    val characterExtensions: MutableList<String> = mutableListOf()
) : Parcelable {
    var enabled: Boolean = true
        private set

    private var lineHtml: String? = null

    companion object {
        private val UNDERSCORE_PATTERN = Pattern.compile("_([^_]+)_")
        private val ITALICIZE_PATTERN = Pattern.compile("\\*([^*]+)\\*")
        private val BOLD_PATTERN = Pattern.compile("\\*\\*([^*{2}]+)\\*\\*")

        @JvmField
        val CREATOR: Parcelable.Creator<Line> = object : Parcelable.Creator<Line> {
            override fun createFromParcel(parcel: Parcel): Line = Line(parcel)
            override fun newArray(size: Int): Array<Line?> = arrayOfNulls(size)
        }
    }

    fun addDialogue(newLine: String) {
        if (line.isEmpty()) {
            this.line = newLine
        } else {
            this.line += "\n" + newLine
        }
    }

    fun getLineHtml(): String {
        if (lineHtml == null) {
            var current = line

            val boldBuffer = StringBuffer()
            val boldMatcher = BOLD_PATTERN.matcher(current)
            while (boldMatcher.find()) {
                boldMatcher.appendReplacement(boldBuffer, "<b>${boldMatcher.group(1)}</b>")
            }
            boldMatcher.appendTail(boldBuffer)
            current = boldBuffer.toString()

            val italicsBuffer = StringBuffer()
            val italicsMatcher = ITALICIZE_PATTERN.matcher(current)
            while (italicsMatcher.find()) {
                italicsMatcher.appendReplacement(italicsBuffer, "<i>${italicsMatcher.group(1)}</i>")
            }
            italicsMatcher.appendTail(italicsBuffer)
            current = italicsBuffer.toString()

            val underlineBuffer = StringBuffer()
            val underlineMatcher = UNDERSCORE_PATTERN.matcher(current)
            while (underlineMatcher.find()) {
                underlineMatcher.appendReplacement(underlineBuffer, "<u>${underlineMatcher.group(1)}</u>")
            }
            underlineMatcher.appendTail(underlineBuffer)
            current = underlineBuffer.toString()

            lineHtml = current.replace("\n", "<br>")
        }
        return lineHtml!!
    }

    override fun toString(): String = "Line{line='$line', characterExtensions=$characterExtensions}"

    constructor(parcel: Parcel) : this(
        actor = Actor(parcel),
        line = parcel.readString() ?: "",
        order = parcel.readInt(),
        characterExtensions = parcel.createStringArrayList() ?: mutableListOf()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(actor, flags)
        parcel.writeString(line)
        parcel.writeInt(order)
        parcel.writeStringList(characterExtensions)
    }

    override fun describeContents(): Int = 0
}
