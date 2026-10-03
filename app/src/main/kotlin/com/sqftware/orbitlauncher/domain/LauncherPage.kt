package com.sqftware.orbitlauncher.domain

import java.util.UUID
import kotlin.math.abs

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

/** The most pages the launcher holds. */
const val MAX_PAGES = 9

/** Pages in swipe order, start to end (mirrored in RTL locales). The launcher starts on, and HOME returns to, [homeIndex]. */
data class PageLayout(val pages: List<LauncherPage> = listOf(LauncherPage.Widgets, LauncherPage.Home, LauncherPage.Collections)) {
    val homeIndex: Int = pages.indexOf(LauncherPage.Home)

    init {
        require(homeIndex >= 0) { "A page layout needs a Home page" }
        require(pages.distinctBy { it.id }.size == pages.size) { "A page id may appear only once: $pages" }
    }

    /** The first page of [kind], if there is one. */
    fun first(kind: PageKind): LauncherPage? = pages.firstOrNull { it.kind == kind }

    val isFull: Boolean get() = pages.size >= MAX_PAGES

    /** Whether a page is two swipes or more from home, so that knowing where you are takes the dots. */
    val showsDots: Boolean get() = pages.indices.any { abs(it - homeIndex) > 1 }

    /**
     * The layout with a new page of [kind] at the start or the end. Its [id] is one no page has had, so nothing a page
     * once held can come back on it. Nothing changes once the layout [isFull].
     */
    fun add(kind: PageKind, atStart: Boolean, id: String = "${kind.name.lowercase()}-${UUID.randomUUID()}"): PageLayout {
        if (isFull) return this
        val page = LauncherPage(id, kind)
        return PageLayout(if (atStart) listOf(page) + pages else pages + page)
    }

    /** The layout without [page]; the home page stays. */
    fun remove(page: LauncherPage): PageLayout = if (page == LauncherPage.Home) this else PageLayout(pages - page)

    /** The layout with the page at [from] moved to [to]; a position off the layout changes nothing. */
    fun move(from: Int, to: Int): PageLayout = PageLayout(pages.reordered(from, to, ReorderMode.Insert))

    /** What [page] is called: Home, or its kind, numbered from the second page of that kind on, home being the first ring. */
    fun label(page: LauncherPage): String {
        if (page == LauncherPage.Home) return "Home"
        val others = pages.filter { it.kind == page.kind && it != LauncherPage.Home }
        val number = others.indexOf(page) + if (page.kind == PageKind.Ring) 2 else 1
        return if (number == 1) page.kind.name else "${page.kind.name} $number"
    }

    /** The ids of these pages, which what each holds is kept by. */
    val ids: Set<String> get() = pages.mapTo(HashSet()) { it.id }

    /** The page at [index], or the nearest one: a pager catches up with a layout that lost pages a frame later. */
    fun pageAt(index: Int): LauncherPage = pages[index.coerceIn(pages.indices)]
}

/** The layout as text, one page per line: its id and its kind, tab-separated. */
fun PageLayout.encode(): String = pages.joinToString(LINE) { "${it.id}$FIELD${it.kind.name}" }

/** A line that is not a page, or a page seen before, is skipped; a layout left without home is the default one. */
fun decodePageLayout(text: String): PageLayout {
    val pages = text.nonEmptyLines().mapNotNull { line ->
        val fields = line.split(FIELD)
        val kind = PageKind.entries.find { it.name == fields.getOrNull(1) } ?: return@mapNotNull null
        LauncherPage(fields[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null, kind)
    }.distinctBy { it.id }
    return if (LauncherPage.Home in pages) PageLayout(pages) else PageLayout()
}
