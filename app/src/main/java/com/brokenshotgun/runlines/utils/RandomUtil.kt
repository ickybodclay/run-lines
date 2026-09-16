package com.brokenshotgun.runlines.utils

import java.util.Random

object RandomUtil {
    private val random = Random().apply {
        setSeed(System.currentTimeMillis())
    }

    /**
     * Returns a pseudorandom, uniformly distributed int value between min (inclusive) and max (exclusive), drawn from this random number generator's sequence.
     * @param min the lower bound (inclusive).  Must be positive.
     * @param max the upper bound (exclusive).  Must be positive.
     * @return the next pseudorandom, uniformly distributed int value between min (inclusive) and max (exclusive), drawn from this random number generator's sequence
     */
    fun range(min: Int, max: Int): Int {
        return min + random.nextInt(max - min)
    }
}
