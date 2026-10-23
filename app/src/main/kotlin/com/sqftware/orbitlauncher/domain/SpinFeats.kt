package com.sqftware.orbitlauncher.domain

/** Spins before this many set some record nearly every time, too often for any to be news. */
private const val RECORDS_FROM = 10

/** The fewest spins that make a milestone. */
private const val FIRST_SPINS_MILESTONE = 10

/** The bests the spin stats keep, in the order they show them. */
enum class SpinRecord { MostTurns, TopRpm, LongestCoast, LongestStreak, BusiestDay }

fun SpinStats.best(record: SpinRecord): Double = when (record) {
    SpinRecord.MostTurns -> mostTurns
    SpinRecord.TopRpm -> topRpm
    SpinRecord.LongestCoast -> longestCoast
    SpinRecord.LongestStreak -> longestStreak.toDouble()
    SpinRecord.BusiestDay -> busiestDay.toDouble()
}

/** A [record] one spin beat, from the best it [was] to the best it is [now]. */
data class RecordBeaten(val record: SpinRecord, val was: Double, val now: Double)

sealed interface Milestone {
    /** The [spins]th spin, a round number. */
    data class Spins(val spins: Int) : Milestone

    /** The rim has now travelled the length of [mark]. */
    data class Distance(val mark: YardstickMark) : Milestone
}

/** What one spin did worth a word. */
data class SpinFeats(val records: List<RecordBeaten> = emptyList(), val milestones: List<Milestone> = emptyList())

/**
 * What the spin that turned [before] into these stats did worth a word, or nothing unless these are the stats one spin
 * on, as a reset is not a spin.
 */
fun SpinStats.feats(before: SpinStats): SpinFeats {
    if (spins != before.spins + 1) return SpinFeats()

    val records = if (before.spins < RECORDS_FROM) emptyList() else SpinRecord.entries.filter {
        // Every spin on a day that already holds the record beats it again; only the one that takes it from the days
        // before is news.
        if (it == SpinRecord.BusiestDay) busiestEarlierDay > 0 && lastDaySpins == busiestEarlierDay + 1 else best(it) > before.best(it)
    }.map { RecordBeaten(it, was = before.best(it), now = best(it)) }
    val milestones = listOfNotNull(
        Milestone.Spins(spins).takeIf { spins >= FIRST_SPINS_MILESTONE && oneTwoFive(spins.toLong()) == spins.toLong() },
        yardstickMark(distance)?.takeIf { it != yardstickMark(before.distance) }?.let(Milestone::Distance),
    )
    return SpinFeats(records, milestones)
}
