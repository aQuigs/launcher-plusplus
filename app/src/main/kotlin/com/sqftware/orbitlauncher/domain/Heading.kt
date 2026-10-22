package com.sqftware.orbitlauncher.domain

import kotlin.math.abs

/** The shortest turn from heading [from] to heading [to], in degrees, clockwise positive, never more than half a circle. */
fun turnBetween(from: Float, to: Float): Float = (to - from + 180f).mod(360f) - 180f

/** A heading [share] of the shortest way from [from] to [to], within a circle. */
fun easeHeading(from: Float, to: Float, share: Float): Float = (from + turnBetween(from, to) * share).mod(360f)

/** Whether a heading moved from [from] to [to] has turned at least [step] degrees, either way. */
fun turnedPast(from: Float, to: Float, step: Float): Boolean = abs(turnBetween(from, to)) >= step
