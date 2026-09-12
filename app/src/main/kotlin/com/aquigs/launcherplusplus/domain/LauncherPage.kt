package com.aquigs.launcherplusplus.domain

enum class LauncherPage { Widgets, Home, Collections }

/** Pages in swipe order, left to right. The launcher starts on, and the HOME key returns to, [homeIndex]. */
data class PageLayout(val pages: List<LauncherPage> = LauncherPage.entries) {
    val homeIndex: Int = pages.indexOf(LauncherPage.Home)

    init {
        require(homeIndex >= 0) { "A page layout needs a Home page" }
        require(pages.distinct().size == pages.size) { "A page may appear only once: $pages" }
    }
}
