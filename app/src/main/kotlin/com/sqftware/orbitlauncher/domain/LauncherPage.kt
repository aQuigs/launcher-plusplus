package com.sqftware.orbitlauncher.domain

/** What a page shows. */
enum class PageKind { Ring, Collections, Widgets }

/** A page of the launcher. Its [id] keys what the page holds, so two pages of one kind each keep their own. */
data class LauncherPage(val id: String, val kind: PageKind) {
    companion object {
        /** The ring page the launcher starts on and HOME returns to; the only one with the clock and the dock. */
        val Home = LauncherPage("home", PageKind.Ring)
        val Widgets = LauncherPage("widgets", PageKind.Widgets)
        val Collections = LauncherPage("collections", PageKind.Collections)
    }
}

/** Pages in swipe order, start to end (mirrored in RTL locales). The launcher starts on, and HOME returns to, [homeIndex]. */
data class PageLayout(val pages: List<LauncherPage> = listOf(LauncherPage.Widgets, LauncherPage.Home, LauncherPage.Collections)) {
    val homeIndex: Int = pages.indexOf(LauncherPage.Home)

    init {
        require(homeIndex >= 0) { "A page layout needs a Home page" }
        require(pages.distinctBy { it.id }.size == pages.size) { "A page id may appear only once: $pages" }
    }

    /** The first page of [kind], if there is one. */
    fun first(kind: PageKind): LauncherPage? = pages.firstOrNull { it.kind == kind }
}
