package com.sqftware.orbitlauncher.domain

import java.time.LocalDate

/** How many steps a day the step reading counts toward. */
const val STEP_GOAL = 10_000

/** One of the phone's readings that a theme can show and a touch can open. */
enum class Vital { Battery, Storage, Steps }

/** Today's steps as the phone counts them, or why there is no count. */
sealed interface Steps {
    /** The phone has no step counter. */
    data object Unavailable : Steps

    /** The user has not let the launcher count steps. */
    data object NotAllowed : Steps

    data class Today(val count: Int) : Steps {
        /** How far toward [STEP_GOAL] they are, out of 100, full once it is reached. */
        val percent: Int get() = minOf(100, (count.toLong() * 100 / STEP_GOAL).toInt())
    }
}

/**
 * What the phone says of itself: how charged the [battery] is and how much of its storage is free, each out of 100, or
 * null until known, and today's [steps].
 */
data class Vitals(val battery: Int? = null, val storageFree: Int? = null, val steps: Steps = Steps.Unavailable)

/**
 * The most steps between two readings of a counter listened to all along. A new day's first reading further on than this
 * was not listened for, and may hold days of steps, so the day counts from it rather than claim them.
 */
const val STEP_JUMP = 500

/**
 * Today's steps from a step counter that counts from each [boot]: [today] is [before] the last boot plus what the
 * counter has gone up since [start], the reading the count began from today, to [last], the latest reading.
 */
data class StepTally(val day: LocalDate, val boot: Int, val start: Long, val last: Long, val before: Long = 0) {
    val today: Int get() = (before + last - start).toInt()

    /**
     * The tally after the counter reads [counter] on [on] in [boot]. A new day counts on from the last reading, or from
     * zero after a restart, unless the counter has jumped past [STEP_JUMP] since, when it counts from this reading. A
     * restart during the day keeps the steps so far and counts on from zero.
     */
    fun read(counter: Long, on: LocalDate, boot: Int): StepTally {
        val restarted = boot != this.boot || counter < last
        val from = if (restarted) 0 else last
        return when {
            on != day -> StepTally(on, boot, start = if (counter - from > STEP_JUMP) counter else from, last = counter)
            restarted -> StepTally(day, boot, start = 0, last = counter, before = today.toLong())
            else -> copy(last = counter)
        }
    }

    companion object {
        /** The first reading ever: no steps yet today. */
        fun first(counter: Long, on: LocalDate, boot: Int) = StepTally(on, boot, start = counter, last = counter)
    }
}
