package com.brokenshotgun.runlines.ui.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptPolicyTest {
    @Test
    fun sceneCountsOnlyAfterItsLastLineCompletesFromTheBeginning() {
        assertFalse(ReviewPromptPolicy.isCompletedSceneRead(3, 5, true))
        assertFalse(ReviewPromptPolicy.isCompletedSceneRead(4, 5, false))
        assertTrue(ReviewPromptPolicy.isCompletedSceneRead(4, 5, true))
        assertFalse(ReviewPromptPolicy.isCompletedSceneRead(0, 0, true))
    }

    @Test
    fun eligibilityStartsAfterFiveCompletedScenes() {
        var progress = ReviewPromptProgress()

        repeat(ReviewPromptPolicy.COMPLETED_SCENE_THRESHOLD - 1) {
            progress = ReviewPromptPolicy.recordCompletedScene(progress)
            assertFalse(ReviewPromptPolicy.isEligible(progress))
        }

        progress = ReviewPromptPolicy.recordCompletedScene(progress)

        assertEquals(5, progress.completedScenes)
        assertTrue(ReviewPromptPolicy.isEligible(progress))
    }

    @Test
    fun completedSceneCountSaturatesAtThreshold() {
        var progress = ReviewPromptProgress()

        repeat(ReviewPromptPolicy.COMPLETED_SCENE_THRESHOLD + 3) {
            progress = ReviewPromptPolicy.recordCompletedScene(progress)
        }

        assertEquals(ReviewPromptPolicy.COMPLETED_SCENE_THRESHOLD, progress.completedScenes)
    }

    @Test
    fun attemptedRequestIsNeverEligibleAndDoesNotAccumulate() {
        val attempted = ReviewPromptPolicy.markRequestAttempted(
            ReviewPromptProgress(completedScenes = ReviewPromptPolicy.COMPLETED_SCENE_THRESHOLD)
        )

        assertFalse(ReviewPromptPolicy.isEligible(attempted))
        assertEquals(attempted, ReviewPromptPolicy.recordCompletedScene(attempted))
    }
}
