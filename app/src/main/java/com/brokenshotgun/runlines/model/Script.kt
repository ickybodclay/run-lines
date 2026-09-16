package com.brokenshotgun.runlines.model

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.brokenshotgun.runlines.data.FNElement
import com.brokenshotgun.runlines.data.FountainSerializer
import kotlin.jvm.JvmOverloads

data class Script @JvmOverloads constructor(
    var name: String,
    var credit: String? = null,
    var author: String? = null,
    var source: String? = null,
    var draftDate: String? = null,
    var contact: String? = null,
    val actors: MutableList<Actor> = mutableListOf(),
    val scenes: MutableList<Scene> = mutableListOf(),
    val allVoices: MutableList<String> = mutableListOf(),
    val actorVoices: MutableMap<String, String> = mutableMapOf(),
    var id: Long = -1L
) : Parcelable {
    var defaultVoice: String? = null

    companion object {
        private const val TAG = "Script"

        @JvmField
        val CREATOR: Parcelable.Creator<Script> = object : Parcelable.Creator<Script> {
            override fun createFromParcel(parcel: Parcel): Script = Script(parcel)
            override fun newArray(size: Int): Array<Script?> = arrayOfNulls(size)
        }

        fun create(name: String): Script {
            return Script(name).apply {
                actors.add(Actor.ACTION)
            }
        }

        fun createFromTokens(titleTokens: Map<String, List<String>>, bodyTokens: Array<FNElement>): Script {
            val script = Script("Untitled script").apply {
                actors.add(Actor.ACTION)
            }
            
            // Parse Title
            titleTokens["title"]?.let { script.name = listToString(it) }
            titleTokens["credit"]?.let { script.credit = listToString(it) }
            titleTokens["authors"]?.let { script.author = listToString(it) }
            titleTokens["source"]?.let { script.source = listToString(it) }
            titleTokens["draft date"]?.let { script.draftDate = listToString(it) }
            titleTokens["contact"]?.let { script.contact = listToString(it) }

            // Parse Body
            var currentScene = Scene()
            val actorMap = mutableMapOf<String, Actor>()
            var currentLine: Line? = null

            for (element in bodyTokens) {
                when (element.elementType) {
                    "Scene Heading" -> {
                        currentScene = Scene(element.elementText)
                        script.scenes.add(currentScene)
                    }
                    "Transition", "Action" -> {
                        currentScene.addAction(element.elementText)
                    }
                    "Character" -> {
                        var actorName = element.elementText
                        val extensions = FountainSerializer.getCharacterExtensions(actorName)
                        if (extensions.isNotEmpty()) {
                            actorName = actorName.replace(FountainSerializer.CHARACTER_EXTENSION_PATTERN.toRegex(), "").trim()
                        }

                        val actor = actorMap.getOrPut(actorName) {
                            val newActor = Actor(actorName)
                            script.addActor(newActor)
                            newActor
                        }

                        currentLine = Line(actor, "").apply {
                            characterExtensions.addAll(extensions)
                        }
                        currentScene.addLine(currentLine!!)
                    }
                    "Parenthetical", "Dialogue" -> {
                        currentLine?.addDialogue(element.elementText)
                    }
                }
            }

            if (currentScene.name == null) {
                currentScene.name = "Untitled scene"
                script.scenes.add(currentScene)
            }

            return script
        }

        private fun listToString(value: List<String>): String {
            return value.joinToString(" ").trim()
        }
    }

    fun addActor(actor: Actor) {
        actors.add(actor)
    }

    fun replaceActor(actor: Actor, replacement: Actor) {
        if (actor == Actor.ACTION) {
            Log.w(TAG, "Cannot remove Action actor")
            return
        }
        actors.remove(actor)
        for (scene in scenes) {
            scene.replaceActor(actor, replacement)
        }
    }

    fun hasActor(currentActor: Actor): Boolean = actors.contains(currentActor)

    fun addScene(newScene: Scene) {
        newScene.number = scenes.size
        scenes.add(newScene)
    }

    fun assignVoice(actor: String, voice: String) {
        actorVoices[actor] = voice
    }

    fun getVoice(actor: String): String? {
        return actorVoices[actor] ?: defaultVoice
    }

    fun addVoice(voice: String) {
        allVoices.add(voice)
    }

    fun getScene(index: Int): Scene {
        return scenes[index]
    }

    override fun toString(): String = "Script{name='$name', credit='$credit', author='$author', source='$source', draftDate='$draftDate', contact='$contact', actors=$actors, scenes=$scenes, allVoices=$allVoices, actorVoices=$actorVoices, defaultVoice='$defaultVoice', id=$id}"

    constructor(parcel: Parcel) : this(
        name = parcel.readString() ?: "Untitled",
        credit = parcel.readString(),
        author = parcel.readString(),
        source = parcel.readString(),
        draftDate = parcel.readString(),
        contact = parcel.readString(),
        actors = parcel.createTypedArrayList(Actor.CREATOR) ?: mutableListOf(),
        scenes = parcel.createTypedArrayList(Scene.CREATOR) ?: mutableListOf(),
        allVoices = parcel.createStringArrayList() ?: mutableListOf(),
        actorVoices = mutableMapOf<String, String>().apply {
            val size = parcel.readInt()
            for (i in 0 until size) {
                put(parcel.readString() ?: "", parcel.readString() ?: "")
            }
        },
        id = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(credit)
        parcel.writeString(author)
        parcel.writeString(source)
        parcel.writeString(draftDate)
        parcel.writeString(contact)
        parcel.writeTypedList(actors)
        parcel.writeTypedList(scenes)
        parcel.writeStringList(allVoices)
        parcel.writeInt(actorVoices.size)
        for ((key, value) in actorVoices) {
            parcel.writeString(key)
            parcel.writeString(value)
        }
        parcel.writeLong(id)
    }

    override fun describeContents(): Int = 0
}
