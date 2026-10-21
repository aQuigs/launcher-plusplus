package com.sqftware.orbitlauncher.domain

/** [degrees] wrapped into 0 until 360. */
fun wrapDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

/**
 * A compass heading moved [share] of the way from [from] to [to], both in degrees clockwise from north, the short way
 * round, so a needle settles rather than jitters with each reading and never swings the long way past north.
 */
fun easeHeading(from: Float, to: Float, share: Float): Float {
    val turn = wrapDegrees(to - from + 180f) - 180f
    return wrapDegrees(from + turn * share)
}
