package com.sqftware.orbitlauncher.domain

/**
 * Whether a theme's minimap shows where the user is: [Shown], [Hidden] because the user turned it off, or off because
 * location access was [Refused] or lost, which the emblem offers to ask for again.
 */
enum class WhereIAm {
    Shown,
    Hidden,
    Refused,
    ;

    /**
     * What this comes to once the launcher knows whether it has location access: unless the user turned the map off,
     * shown while [granted], as after a grant given in Settings, and refused otherwise.
     */
    fun withGrant(granted: Boolean) = when {
        this == Hidden -> Hidden
        granted -> Shown
        else -> Refused
    }

    /** Whether picking a theme that maps asks for location access: unless the user turned the map off. */
    val asksOnPick: Boolean
        get() = this != Hidden

    companion object {
        /** What the user's answer to the ask for location access makes it. */
        fun answered(granted: Boolean) = if (granted) Shown else Refused
    }
}
