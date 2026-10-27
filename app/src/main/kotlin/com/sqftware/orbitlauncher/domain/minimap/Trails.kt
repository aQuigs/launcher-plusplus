package com.sqftware.orbitlauncher.domain.minimap

/** A path the user logged, the fixes in the order they came, from where they started it to where they stopped it. */
typealias Trail = List<LatLon>

/** The trails the user has logged, oldest first; while [logging], the last is the one still growing. */
data class Trails(val all: List<Trail> = emptyList(), val logging: Boolean = false) {
    /** The trails done with: all but the one growing. */
    val finished get() = if (logging) all.dropLast(1) else all

    /** The trail being logged, if one is. */
    val growing get() = all.lastOrNull()?.takeIf { logging }

    /** A new trail begun, or these unchanged if one is already being logged. */
    fun started() = if (logging) this else Trails(all + listOf(emptyList()), logging = true)

    /** The trail being logged gone on to [fix], or these unchanged if none is. */
    fun logged(fix: LatLon) = if (logging) copy(all = all.dropLast(1) + listOf(all.last() + fix)) else this

    /** The trail being logged ended, dropped if it never went anywhere. */
    fun stopped() = if (logging) Trails(all.filter { it.goesAnywhere() }, logging = false) else this

    companion object {
        private const val TRAIL_SEPARATOR = ";"
        private const val POINT_SEPARATOR = " "

        // A millionth of a degree is a tenth of a meter, finer than any fix, and keeps a long run's trail short.
        private const val PLACES = 1_000_000.0

        /** [trails] as text for storage, which [parse] reads back. */
        fun encode(trails: List<Trail>) = trails.joinToString(TRAIL_SEPARATOR) { trail ->
            trail.joinToString(POINT_SEPARATOR) { "${it.lat.toPlaces()},${it.lon.toPlaces()}" }
        }

        /** The trails [encode] wrote, leaving out anything unreadable and any trail that went nowhere. */
        fun parse(text: String): List<Trail> =
            text.split(TRAIL_SEPARATOR).map { trail -> trail.split(POINT_SEPARATOR).mapNotNull(::parsePoint) }.filter { it.goesAnywhere() }

        private fun parsePoint(text: String): LatLon? {
            val (lat, lon) = text.split(",").takeIf { it.size == 2 } ?: return null
            return LatLon(lat.toDoubleOrNull() ?: return null, lon.toDoubleOrNull() ?: return null)
        }

        private fun Double.toPlaces() = Math.round(this * PLACES) / PLACES

        private fun Trail.goesAnywhere() = size > 1
    }
}
