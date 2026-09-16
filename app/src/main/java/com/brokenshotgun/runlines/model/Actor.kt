package com.brokenshotgun.runlines.model

import android.os.Parcel
import android.os.Parcelable

data class Actor(val name: String) : Parcelable {
    override fun toString(): String = name

    constructor(parcel: Parcel) : this(parcel.readString() ?: "")

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
    }

    override fun describeContents(): Int = 0

    companion object {
        const val ACTION_NAME = "ACTION"
        val ACTION = Actor(ACTION_NAME)

        @JvmField
        val CREATOR: Parcelable.Creator<Actor> = object : Parcelable.Creator<Actor> {
            override fun createFromParcel(parcel: Parcel): Actor = Actor(parcel)
            override fun newArray(size: Int): Array<Actor?> = arrayOfNulls(size)
        }
    }
}
