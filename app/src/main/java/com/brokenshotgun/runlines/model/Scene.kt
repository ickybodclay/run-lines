package com.brokenshotgun.runlines.model

import android.os.Parcel
import android.os.Parcelable
import kotlin.jvm.JvmOverloads

data class Scene @JvmOverloads constructor(
    var name: String? = null,
    var number: Int = 0,
    val lines: MutableList<Line> = mutableListOf()
) : Parcelable {

    fun addLine(line: Line) {
        lines.add(line)
    }

    fun addAction(action: String) {
        lines.add(Line(Actor.ACTION, action))
    }

    fun replaceActor(actor: Actor, replacement: Actor) {
        for (line in lines) {
            if (line.actor == actor) {
                line.actor = replacement
            }
        }
    }

    override fun toString(): String = "Scene{name='$name', number=$number, lines=$lines}"

    constructor(parcel: Parcel) : this(
        name = parcel.readString(),
        number = parcel.readInt(),
        lines = parcel.createTypedArrayList(Line.CREATOR) ?: mutableListOf()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(number)
        parcel.writeTypedList(lines)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Scene> = object : Parcelable.Creator<Scene> {
            override fun createFromParcel(parcel: Parcel): Scene = Scene(parcel)
            override fun newArray(size: Int): Array<Scene?> = arrayOfNulls(size)
        }
    }
}
