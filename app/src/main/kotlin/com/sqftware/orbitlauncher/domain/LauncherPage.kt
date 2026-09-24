package com.sqftware.orbitlauncher.domain

/** Declaration order is the default page order, start to end (mirrored in RTL locales). */
enum class LauncherPage { Widgets, Home, Collections }

/** Pages in swipe order. The launcher starts on, and the HOME key returns to, [homeIndex]. */
data class PageLayout(val pages: List<LauncherPage> = LauncherPage.entries) {
    val homeIndex: Int = pages.indexOf(LauncherPage.Home)

    init {
        require(homeIndex >= 0) { "A page layout needs a Home page" }
        require(pages.distinct().size == pages.size) { "A page may appear only once: $pages" }
    }
}
