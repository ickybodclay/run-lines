package com.brokenshotgun.runlines.ui.review

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.review.ReviewManagerFactory

internal data class ReviewPromptProgress(
    val completedScenes: Int = 0,
    val requestAttempted: Boolean = false
)

internal object ReviewPromptPolicy {
    const val COMPLETED_SCENE_THRESHOLD = 5

    fun isCompletedSceneRead(
        completedLineIndex: Int,
        lineCount: Int,
        startedAtBeginning: Boolean
    ): Boolean =
        startedAtBeginning &&
            lineCount > 0 &&
            completedLineIndex == lineCount - 1

    fun recordCompletedScene(progress: ReviewPromptProgress): ReviewPromptProgress =
        if (progress.requestAttempted) {
            progress
        } else {
            progress.copy(
                completedScenes = (progress.completedScenes + 1)
                    .coerceAtMost(COMPLETED_SCENE_THRESHOLD)
            )
        }

    fun isEligible(progress: ReviewPromptProgress): Boolean =
        !progress.requestAttempted &&
            progress.completedScenes >= COMPLETED_SCENE_THRESHOLD

    fun markRequestAttempted(progress: ReviewPromptProgress): ReviewPromptProgress =
        progress.copy(requestAttempted = true)
}

object InAppReviewCoordinator {
    private const val TAG = "InAppReviewCoordinator"
    private const val PREFERENCES_NAME = "in_app_review"
    private const val KEY_COMPLETED_SCENES = "completed_scenes"
    private const val KEY_REQUEST_ATTEMPTED = "request_attempted"
    private val lock = Any()

    fun recordCompletedScene(context: Context) {
        synchronized(lock) {
            val preferences = context.applicationContext.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            val progress = loadProgress(preferences)
            val updatedProgress = ReviewPromptPolicy.recordCompletedScene(progress)
            if (updatedProgress != progress) {
                saveProgress(preferences, updatedProgress)
            }
        }
    }

    fun requestIfEligible(context: Context) {
        val activity = context.findActivity()
        if (activity == null) {
            Log.w(TAG, "Cannot launch the in-app review flow without an Activity")
            return
        }
        val lifecycleOwner = activity as? LifecycleOwner
        if (lifecycleOwner == null) {
            Log.w(TAG, "Cannot launch the in-app review flow without a lifecycle-aware Activity")
            return
        }
        if (
            activity.isFinishing ||
            activity.isDestroyed ||
            !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
            return
        }

        val preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
        synchronized(lock) {
            val progress = loadProgress(preferences)
            if (!ReviewPromptPolicy.isEligible(progress)) return
            saveProgress(preferences, ReviewPromptPolicy.markRequestAttempted(progress))
        }

        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful) {
                Log.e(TAG, "Could not request the in-app review flow", request.exception)
                return@addOnCompleteListener
            }
            if (
                activity.isFinishing ||
                activity.isDestroyed ||
                !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            ) {
                return@addOnCompleteListener
            }

            manager.launchReviewFlow(activity, request.result).addOnCompleteListener { flow ->
                if (!flow.isSuccessful) {
                    Log.e(TAG, "The in-app review flow failed", flow.exception)
                }
            }
        }
    }

    private fun loadProgress(
        preferences: android.content.SharedPreferences
    ): ReviewPromptProgress =
        ReviewPromptProgress(
            completedScenes = preferences.getInt(KEY_COMPLETED_SCENES, 0)
                .coerceIn(0, ReviewPromptPolicy.COMPLETED_SCENE_THRESHOLD),
            requestAttempted = preferences.getBoolean(KEY_REQUEST_ATTEMPTED, false)
        )

    private fun saveProgress(
        preferences: android.content.SharedPreferences,
        progress: ReviewPromptProgress
    ) {
        preferences.edit()
            .putInt(KEY_COMPLETED_SCENES, progress.completedScenes)
            .putBoolean(KEY_REQUEST_ATTEMPTED, progress.requestAttempted)
            .apply()
    }

    private fun Context.findActivity(): Activity? {
        var current = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            val base = current.baseContext
            if (base === current) return null
            current = base
        }
        return null
    }
}
