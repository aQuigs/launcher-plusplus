package com.sqftware.orbitlauncher.ui

import com.sqftware.orbitlauncher.domain.Milestone
import com.sqftware.orbitlauncher.domain.SpinFeats
import com.sqftware.orbitlauncher.domain.SpinRecord
import com.sqftware.orbitlauncher.domain.durationText
import com.sqftware.orbitlauncher.domain.yardstickMarkText
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

/** A notice for each of [feats]. A record is news only when it shows higher: 120.4 rpm over 120.2 still shows as 120. */
fun spinFeatTexts(feats: SpinFeats, locale: Locale): List<String> {
    val records = feats.records.mapNotNull { (record, was, now) ->
        val best = record.bestText(now, locale)
        "New record: ${record.label.lowercase()}, $best".takeIf { best != record.bestText(was, locale) }
    }
    val milestones = feats.milestones.map {
        when (it) {
            is Milestone.Spins -> "Milestone: ${String.format(locale, "%,d", it.spins)} spins"
            is Milestone.Distance -> "You've spun the length of ${yardstickMarkText(it.mark, locale)}"
        }
    }
    return records + milestones
}
