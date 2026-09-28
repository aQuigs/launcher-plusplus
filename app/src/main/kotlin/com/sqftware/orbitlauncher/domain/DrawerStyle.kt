package com.sqftware.orbitlauncher.domain

/** How the drawer lays out its apps. */
enum class DrawerLayout(val label: String) {
    /** A row per app, its name beside its icon. */
    List("List"),

    /** Rows of icons, each named underneath. */
    Grid("Grid"),
}

/** The order the drawer lists its apps in. */
enum class DrawerOrder(val label: String) {
    /** By name, with a rail of their initials to jump by. */
    Alphabetical("A to Z"),

    /** The longest in front lately first. */
    MostUsed("Most used"),

    /** The most recently installed first. */
    Newest("Date added"),
}

/** How the drawer shows the apps: its [layout], its [order], and whether a row of the [mostUsedRow] heads the list. */
data class DrawerStyle(
    val layout: DrawerLayout = DrawerLayout.List,
    val order: DrawerOrder = DrawerOrder.Alphabetical,
    val mostUsedRow: Boolean = false,
) {
    /** Whether it needs to know how long apps are in front, which takes usage access. */
    val readsUsage: Boolean get() = mostUsedRow || order == DrawerOrder.MostUsed
}

/**
 * These apps, sorted by name, in [order]; [time] ranks them for [DrawerOrder.MostUsed]. Ties, and apps never in front,
 * keep their order by name.
 */
fun List<AppEntry>.inOrder(order: DrawerOrder, time: ForegroundTime?): List<AppEntry> = when (order) {
    DrawerOrder.Alphabetical -> this
    DrawerOrder.MostUsed -> sortedByDescending { time?.byPackage?.get(it.packageName) ?: 0L }
    DrawerOrder.Newest -> sortedByDescending { it.installedAt }
}
