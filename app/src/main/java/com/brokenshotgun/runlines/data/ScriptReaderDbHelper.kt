package com.brokenshotgun.runlines.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.brokenshotgun.runlines.model.Script
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.brokenshotgun.runlines.data.ScriptReaderContract.ScriptEntry

class ScriptReaderDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "ScriptReader.db"
        private const val TEXT_TYPE = " TEXT"
        private const val COMMA_SEP = ","

        private val SQL_CREATE_SCRIPT_TABLE =
            "CREATE TABLE ${ScriptEntry.TABLE_NAME} (" +
                    "${android.provider.BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${ScriptEntry.COLUMN_NAME_SCRIPT_JSON}$TEXT_TYPE$COMMA_SEP" +
                    "${ScriptEntry.COLUMN_NAME_CREATE_DATE}$TEXT_TYPE" +
                    " )"

        private val SQL_DELETE_SCRIPT_TABLE =
            "DROP TABLE IF EXISTS ${ScriptEntry.TABLE_NAME}"
    }

    private val gson: Gson = GsonBuilder().create()

    fun insertScript(script: Script) {
        val db = writableDatabase

        val sValues = ContentValues().apply {
            put(ScriptEntry.COLUMN_NAME_SCRIPT_JSON, serialize(script))
            put(ScriptEntry.COLUMN_NAME_CREATE_DATE, System.currentTimeMillis())
        }

        val newScriptId = db.insert(
            ScriptEntry.TABLE_NAME,
            null,
            sValues
        )

        script.id = newScriptId
    }

    fun updateScript(script: Script) {
        val db = readableDatabase

        val values = ContentValues().apply {
            put(ScriptEntry.COLUMN_NAME_SCRIPT_JSON, serialize(script))
        }

        val selection = "${android.provider.BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(script.id.toString())

        db.update(
            ScriptEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
    }

    fun deleteScript(script: Script) {
        val db = writableDatabase

        val selection = "${android.provider.BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(script.id.toString())

        db.delete(
            ScriptEntry.TABLE_NAME,
            selection,
            selectionArgs
        )
    }

    fun getScripts(): List<Script> {
        val results = mutableListOf<Script>()

        val db = readableDatabase

        val projection = arrayOf(
            android.provider.BaseColumns._ID,
            ScriptEntry.COLUMN_NAME_SCRIPT_JSON,
        )

        val sortOrder = "${ScriptEntry.COLUMN_NAME_CREATE_DATE} DESC"

        db.query(
            ScriptEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            sortOrder
        ).use { c ->
            while (c.moveToNext()) {
                val scriptId = c.getLong(0)
                val scriptJson = c.getString(1)
                val script = deserialize(scriptJson)
                script.id = scriptId
                results.add(script)
            }
        }

        return results
    }

    private fun serialize(script: Script): String {
        return gson.toJson(script)
    }

    private fun deserialize(json: String): Script {
        return try {
            gson.fromJson(json, Script::class.java)
        } catch (e: Exception) {
            Log.e(ScriptReaderDbHelper::class.java.name, e.message ?: "Unknown error", e)
            Script("Error")
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_SCRIPT_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(ScriptReaderDbHelper::class.java.name, ">>> onUpgrade!")
        db.execSQL(SQL_DELETE_SCRIPT_TABLE)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(ScriptReaderDbHelper::class.java.name, ">>> onDowngrade!")
        System.exit(-1)
    }
}
