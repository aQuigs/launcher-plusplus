package com.sqftware.orbitlauncher.domain.minimap

/** A path the user logged, the fixes in the order they came, from where they started it to where they stopped it. */
data class Trail(val points: List<LatLon>)

/** The trails the user has logged, oldest first; while [logging], the last is the one still growing. */
data class Trails(val trails: List<Trail> = emptyList(), val logging: Boolean = false) {
    /** A new trail begun, or these unchanged if one is already being logged. */
    fun started() = if (logging) this else Trails(trails + Trail(emptyList()), logging = true)

    /** The trail being logged gone on to [fix], or these unchanged if none is. */
    fun logged(fix: LatLon) = if (logging) copy(trails = trails.dropLast(1) + Trail(trails.last().points + fix)) else this

    /** The trail being logged ended, dropped if it never went anywhere. */
    fun stopped() = if (logging) Trails(trails.filter { it.goesAnywhere() }, logging = false) else this

    /** These as text for storage, which [parse] reads back; whether one is being logged is not kept. */
    fun encode() = trails.joinToString(TRAIL_SEPARATOR) { trail -> trail.points.joinToString(POINT_SEPARATOR) { "${it.lat},${it.lon}" } }

    companion object {
        private const val TRAIL_SEPARATOR = ";"
        private const val POINT_SEPARATOR = " "

        /**
         * The trails [encode] wrote, none being logged: one logging when the launcher was ended stops where it got to.
         * Anything unreadable is left out.
         */
        fun parse(text: String) = Trails(
            text.split(TRAIL_SEPARATOR)
                .map { trail -> Trail(trail.split(POINT_SEPARATOR).mapNotNull(::parsePoint)) }
                .filter { it.goesAnywhere() },
        )

        private fun parsePoint(text: String): LatLon? {
            val (lat, lon) = text.split(",").takeIf { it.size == 2 } ?: return null
            return LatLon(lat.toDoubleOrNull() ?: return null, lon.toDoubleOrNull() ?: return null)
        }

        private fun Trail.goesAnywhere() = points.size > 1
    }
}
