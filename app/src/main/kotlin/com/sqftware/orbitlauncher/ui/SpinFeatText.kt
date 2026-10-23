package com.sqftware.orbitlauncher.ui

import com.sqftware.orbitlauncher.domain.Milestone
import com.sqftware.orbitlauncher.domain.SpinFeats
import com.sqftware.orbitlauncher.domain.SpinRecord
import com.sqftware.orbitlauncher.domain.durationText
import java.util.Locale

val SpinRecord.label
    get() = when (this) {
        SpinRecord.MostTurns -> "Most in one spin"
        SpinRecord.TopRpm -> "Top speed"
        SpinRecord.LongestCoast -> "Longest coast"
        SpinRecord.LongestStreak -> "Longest streak"
        SpinRecord.BusiestDay -> "Busiest day"
    }

/** The [best] for this record as the spin stats show it. */
fun SpinRecord.bestText(best: Double, locale: Locale): String = when (this) {
    SpinRecord.MostTurns -> "${String.format(locale, "%,.1f", best)} turns"
    SpinRecord.TopRpm -> "${String.format(locale, "%,.0f", best)} rpm"
    SpinRecord.LongestCoast -> durationText(best, locale)
    SpinRecord.LongestStreak -> counted(best.toInt(), "day")
    SpinRecord.BusiestDay -> counted(best.toInt(), "spin")
}

/**
 * [feats] as a notice, a line for the records and one for each milestone, or null if there are none. A record is
 * news only when it shows higher: 120.4 rpm over 120.2 still shows as 120. Several records at once are only named, as
 * one fling often sets three and a notice is up for a moment.
 */
fun spinFeatsText(feats: SpinFeats, locale: Locale): String? {
    val records = feats.records.filter { it.record.bestText(it.now, locale) != it.record.bestText(it.was, locale) }
    val recordLine = when (records.size) {
        0 -> null
        1 -> records.single().let { "New record: ${it.record.label.lowercase()}, ${it.record.bestText(it.now, locale)}" }
        else -> records.map { it.record.label.lowercase() }.let { "New records: ${it.dropLast(1).joinToString(", ")} and ${it.last()}" }
    }
    val milestoneLines = feats.milestones.map {
        when (it) {
            is Milestone.Spins -> "Milestone: ${String.format(locale, "%,d", it.spins)} spins"
            is Milestone.Distance -> with(it.mark) {
                val article = if (one.first().lowercaseChar() in "aeiou") "an" else "a"
                "You've spun the length of ${if (count == 1L) "$article $one" else "${String.format(locale, "%,d", count)} $many"}"
            }
        }
    }
    return (listOfNotNull(recordLine) + milestoneLines).takeIf { it.isNotEmpty() }?.joinToString("\n")
}
