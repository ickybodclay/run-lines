package com.brokenshotgun.runlines.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.brokenshotgun.runlines.R

object Intents {
    /**
     * Attempt to launch the supplied [Intent]. Queries on-device packages before launching and
     * will display a simple message if none are available to handle it.
     */
    fun maybeStartActivity(context: Context, intent: Intent): Boolean {
        return maybeStartActivity(context, intent, false)
    }

    fun maybeStartActivityForResult(activity: Activity, intent: Intent, resultCode: Int): Boolean {
        return maybeStartActivityForResult(activity, intent, false, resultCode)
    }

    /**
     * Attempt to launch Android's chooser for the supplied [Intent]. Queries on-device
     * packages before launching and will display a simple message if none are available to handle
     * it.
     */
    fun maybeStartChooser(context: Context, intent: Intent): Boolean {
        return maybeStartActivity(context, intent, true)
    }

    fun maybeStartChooserForResult(activity: Activity, intent: Intent, resultCode: Int): Boolean {
        return maybeStartActivityForResult(activity, intent, true, resultCode)
    }

    private fun maybeStartActivity(context: Context, intent: Intent, chooser: Boolean): Boolean {
        return try {
            val finalIntent = if (chooser) {
                Intent.createChooser(intent, null)
            } else {
                intent
            }
            context.startActivity(finalIntent)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_intent_handler, Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun maybeStartActivityForResult(activity: Activity, intent: Intent, chooser: Boolean, resultCode: Int): Boolean {
        return try {
            val finalIntent = if (chooser) {
                Intent.createChooser(intent, null)
            } else {
                intent
            }
            activity.startActivityForResult(finalIntent, resultCode)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.no_intent_handler, Toast.LENGTH_LONG).show()
            false
        }
    }
}
