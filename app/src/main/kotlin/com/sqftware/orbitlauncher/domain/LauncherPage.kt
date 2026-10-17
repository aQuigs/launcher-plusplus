package com.sqftware.orbitlauncher.domain

import java.util.UUID
import kotlin.math.abs

/** What a page shows, and what such a page is called. */
enum class PageKind(val title: String) {
    Ring("Ring"),
    Collections("Collections"),
    Widgets("Widgets"),

    /** A plain launcher's grid of apps, folders and widgets. */
    Grid("Boring grid"),
}

/** A page of the launcher. Its [id] keys what the page holds, so two pages of one kind each keep their own. */
data class LauncherPage(val id: String, val kind: PageKind) {
    companion object {
        /** The ring page with the clock and the dock, which no layout is without; the launcher's home unless another is chosen. */
        val Home = LauncherPage("home", PageKind.Ring)
        val Widgets = LauncherPage("widgets", PageKind.Widgets)
        val Collections = LauncherPage("collections", PageKind.Collections)
    }
}

/** The most pages the launcher holds. */
const val MAX_PAGES = 9

/**
 * Pages in swipe order, start to end (mirrored in RTL locales). The launcher starts on, and HOME returns to, the page the
 * user chose as home ([homeIndex]), [LauncherPage.Home] unless another was chosen.
 */
data class PageLayout(
    val pages: List<LauncherPage> = listOf(LauncherPage.Widgets, LauncherPage.Home, LauncherPage.Collections),
    private val homeId: String = LauncherPage.Home.id,
) {
    val homeIndex: Int = pages.indexOfFirst { it.id == homeId }.takeIf { it >= 0 } ?: pages.indexOf(LauncherPage.Home)

    /** The page the launcher starts on and HOME returns to. */
    val home: LauncherPage get() = pages[homeIndex]

    init {
        require(LauncherPage.Home in pages) { "A page layout needs a Home page" }
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
        return PageLayout(if (atStart) listOf(page) + pages else pages + page, home.id)
    }

    /** The layout without [page]; [LauncherPage.Home] stays, and becomes home again if [page] was. */
    fun remove(page: LauncherPage): PageLayout = if (page == LauncherPage.Home) this else PageLayout(pages - page, home.id)

    /** The layout with the page at [from] moved to [to]; a position off the layout changes nothing. */
    fun move(from: Int, to: Int): PageLayout = PageLayout(pages.reordered(from, to, ReorderMode.Insert), home.id)

    /** The layout with [page] as home; one it does not hold changes nothing. */
    fun withHome(page: LauncherPage): PageLayout = if (page in pages) PageLayout(pages, page.id) else this

    /** What [page] is called: Home, or its kind, numbered from the second page of that kind on, home being the first ring. */
    fun label(page: LauncherPage): String {
        if (page == LauncherPage.Home) return "Home"
        val others = pages.filter { it.kind == page.kind && it != LauncherPage.Home }
        val number = others.indexOf(page) + if (page.kind == PageKind.Ring) 2 else 1
        return if (number == 1) page.kind.title else "${page.kind.title} $number"
    }

    /** The ids of these pages, which what each holds is kept by. */
    val ids: Set<String> get() = pages.mapTo(HashSet()) { it.id }

    /** The page at [index], or the nearest one: a pager catches up with a layout that lost pages a frame later. */
    fun pageAt(index: Int): LauncherPage = pages[index.coerceIn(pages.indices)]
}

/** The layout as text, one page per line: its id and its kind, tab-separated, and on home's line a third field saying so. */
fun PageLayout.encode(): String =
    pages.joinToString(LINE) { "${it.id}$FIELD${it.kind.name}" + if (it == home && it != LauncherPage.Home) "$FIELD$HOME_FIELD" else "" }

private const val HOME_FIELD = "home"

/** A line that is not a page, or a page seen before, is skipped; a layout left without home is the default one. */
fun decodePageLayout(text: String): PageLayout {
    var home = LauncherPage.Home.id
    val pages = text.nonEmptyLines().mapNotNull { line ->
        val fields = line.split(FIELD)
        val kind = PageKind.entries.find { it.name == fields.getOrNull(1) } ?: return@mapNotNull null
        LauncherPage(fields[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null, kind)
            .also { if (fields.getOrNull(2) == HOME_FIELD) home = it.id }
    }.distinctBy { it.id }
    return if (LauncherPage.Home in pages) PageLayout(pages, home) else PageLayout()
}
