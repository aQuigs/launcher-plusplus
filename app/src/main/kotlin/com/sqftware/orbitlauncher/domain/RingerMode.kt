package com.sqftware.orbitlauncher.domain

/** The phone's ringer, in the order the home page's switch steps through it. */
enum class RingerMode {
    Normal,
    Vibrate,
    Silent,
    ;

    /** The mode after this one, back round to [Normal] after [Silent]. */
    fun next(): RingerMode = entries[(ordinal + 1) % entries.size]
}
