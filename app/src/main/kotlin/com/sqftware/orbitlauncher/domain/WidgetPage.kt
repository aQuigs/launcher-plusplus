package com.sqftware.orbitlauncher.domain

import kotlin.math.ceil
import kotlin.math.roundToInt

/** The columns across the widget page. */
const val WIDGET_COLUMNS = 4

/** The least height of a row of the widget page, in dp: the rows stretch to share the page's height. */
const val WIDGET_ROW_HEIGHT_DP = 80

/** The space between two cells of the widget page, in dp, which a widget spanning both covers too. */
const val WIDGET_GAP_DP = 8

/** The id the ring of a ring page goes by among the widgets it holds: one the system never gives a widget. */
const val RING_ID = 0

/**
 * A widget on the page: the id the system knows it by, the row and column of its top-left cell, and the rows and
 * columns it spans.
 */
data class HostedWidget(val id: Int, val row: Int, val column: Int, val rows: Int, val columns: Int) {
    val bottom: Int get() = row + rows

    val end: Int get() = column + columns

    fun overlaps(other: HostedWidget): Boolean = row < other.bottom && other.row < bottom && column < other.end && other.column < end
}

/**
 * The widgets on a page, each in cells of its own, top to bottom and then left to right. Cells no widget takes stay
 * empty, so a widget keeps its place when another leaves or moves. On a ring page the ring takes cells too, across the
 * page, as [RING_ID].
 */
data class WidgetPage(val widgets: List<HostedWidget> = emptyList()) {
    val isEmpty: Boolean get() = ids.isEmpty()

    /** The ids of the widgets the system hosts, the ring's left out. */
    val ids: Set<Int> get() = widgets.mapNotNullTo(mutableSetOf()) { it.id.takeIf { id -> id != RING_ID } }

    val ring: HostedWidget? get() = widgets.find { it.id == RING_ID }

    /** The rows down to the bottom of the lowest widget. */
    val rows: Int get() = widgets.maxOfOrNull { it.bottom } ?: 0

    /**
     * The page with the widget [id] in the first free cells, from the top and then the left, that hold [rows] by
     * [columns], within the first [pageRows] rows; unchanged when there are none.
     */
    fun add(id: Int, rows: Int, columns: Int, pageRows: Int = Int.MAX_VALUE): WidgetPage {
        val width = columns.coerceIn(1, WIDGET_COLUMNS)
        val height = rows.coerceAtLeast(1)
        val spot = generateSequence(0) { it + 1 }
            .takeWhile { it <= pageRows - height }
            .flatMap { row -> (0..WIDGET_COLUMNS - width).asSequence().map { HostedWidget(id, row, it, height, width) } }
            .firstOrNull { new -> widgets.none(new::overlaps) } ?: return this
        return WidgetPage((widgets + spot).sortedWith(PLACE))
    }

    fun remove(id: Int): WidgetPage = WidgetPage(widgets.filter { it.id != id })

    /**
     * The widget [id] made [rows] by [columns], no wider than the page leaves it from its column. The widgets it now
     * covers move down out of its way.
     */
    fun resize(id: Int, rows: Int, columns: Int): WidgetPage =
        change(id) { it.copy(rows = rows.coerceAtLeast(1), columns = columns.coerceIn(1, WIDGET_COLUMNS - it.column)) }

    /**
     * The widget [id] with its top-left cell at [row] and [column], or as near as the page's edges let it. The widgets it
     * now covers move down out of its way.
     */
    fun move(id: Int, row: Int, column: Int): WidgetPage =
        change(id) { it.copy(row = row.coerceAtLeast(0), column = column.coerceIn(0, WIDGET_COLUMNS - it.columns)) }

    private fun change(id: Int, change: (HostedWidget) -> HostedWidget): WidgetPage {
        val changed = widgets.find { it.id == id }?.let(change) ?: return this
        return WidgetPage(listOf(changed) + widgets.filter { it.id != id }).settled()
    }

    /**
     * On a page of [pageRows] rows that never lengthens, the widget [id] with its top-left cell at [row] and [column], or
     * as near as the page's edges let it. The widgets it now covers make way as in a list: up into the room it left when
     * it went down, otherwise down, or the other way where there is no room. Null when one of them fits nowhere.
     */
    fun moveWithin(id: Int, row: Int, column: Int, pageRows: Int): WidgetPage? = changeWithin(id, pageRows) {
        it.copy(row = row.coerceIn(0, maxOf(pageRows - it.rows, 0)), column = column.coerceIn(0, WIDGET_COLUMNS - it.columns))
    }

    /** As [resize], on a page of [pageRows] rows that never lengthens, the widgets in the way making way as in [moveWithin]. */
    fun resizeWithin(id: Int, rows: Int, columns: Int, pageRows: Int): WidgetPage? = changeWithin(id, pageRows) {
        it.copy(rows = rows.coerceIn(1, maxOf(pageRows - it.row, 1)), columns = columns.coerceIn(1, WIDGET_COLUMNS - it.column))
    }

    /**
     * The page with the ring across it, [rows] tall, where it was or else in the middle of the [pageRows] rows, the
     * widgets in its way making way as in [moveWithin]. Where one fits nowhere, as when the screen has fewer rows than
     * it had, they move down out of its way, past the page's last row.
     */
    fun withRing(rows: Int, pageRows: Int): WidgetPage {
        val ring = ring?.copy(rows = rows) ?: HostedWidget(RING_ID, (pageRows - rows) / 2, 0, rows, WIDGET_COLUMNS)
        val ringed = WidgetPage(listOf(ring) + widgets.filter { it.id != RING_ID })
        return ringed.moveWithin(RING_ID, ring.row, 0, pageRows) ?: ringed.settled()
    }

    private fun changeWithin(id: Int, pageRows: Int, change: (HostedWidget) -> HostedWidget): WidgetPage? {
        val before = widgets.find { it.id == id } ?: return this
        val after = change(before)
        if (after.bottom > pageRows) return null
        val (inWay, clear) = widgets.filter { it.id != id }.partition(after::overlaps)
        val placed = (clear + after).toMutableList()
        val upFirst = after.row > before.row
        // Nearest the moved one first, so those that make way keep their order.
        inWay.sortedBy { if (upFirst) -it.row else it.row }.forEach { widget ->
            val up = (widget.row downTo 0).asSequence()
            val down = (widget.row + 1..pageRows - widget.rows).asSequence()
            placed += (if (upFirst) up + down else down + up)
                .map { widget.copy(row = it) }
                .firstOrNull { at -> at.bottom <= pageRows && placed.none(at::overlaps) } ?: return null
        }
        return WidgetPage(placed.sortedWith(PLACE))
    }

    /**
     * The page with each widget, in turn, where it is or, when an earlier one is there already, moved down to the first
     * row where it fits.
     */
    internal fun settled(): WidgetPage {
        val placed = mutableListOf<HostedWidget>()
        widgets.forEach { widget ->
            var at = widget
            while (placed.any(at::overlaps)) at = at.copy(row = at.row + 1)
            placed += at
        }
        return WidgetPage(placed.sortedWith(PLACE))
    }

    internal companion object {
        val PLACE = compareBy<HostedWidget>({ it.row }, { it.column })
    }
}

/** The widgets on every widget page, by page id. A widget's id is the system's, so it is on one page at most. */
data class WidgetPages(val pages: Map<String, WidgetPage> = emptyMap()) {
    operator fun get(page: String): WidgetPage = pages[page] ?: WidgetPage()

    val ids: Set<Int> get() = pages.values.flatMapTo(mutableSetOf()) { it.ids }

    /** These pages with [page] as [change] leaves it. */
    fun change(page: String, change: WidgetPage.() -> WidgetPage): WidgetPages = WidgetPages(pages + (page to this[page].change()))

    /** These pages with the one holding the widget [id], if any, as [change] leaves it. */
    fun changeHolding(id: Int, change: WidgetPage.() -> WidgetPage): WidgetPages =
        pages.entries.find { id in it.value.ids }?.let { change(it.key, change) } ?: this

    /** These pages with only the widgets of the pages in [ids]. */
    fun keepingPages(ids: Set<String>): WidgetPages = WidgetPages(pages.filterKeys { it in ids })

    /** These pages with only the widgets whose ids are in [held], and their rings. */
    fun keeping(held: Set<Int>): WidgetPages =
        WidgetPages(pages.mapValues { (_, page) -> WidgetPage(page.widgets.filter { it.id in held || it.id == RING_ID }) })
}

/**
 * The cells, [cellDp] each with a gap between two, that a widget needs to be at least [minDp] long: at least one, and
 * no more than [most].
 */
fun widgetCells(minDp: Int, cellDp: Float, most: Int): Int =
    ceil((minDp + WIDGET_GAP_DP) / (cellDp + WIDGET_GAP_DP)).toInt().coerceIn(1, most.coerceAtLeast(1))

/** The most cells, [cellDp] each with a gap between two, that fit in [dp]. */
fun cellsWithin(dp: Float, cellDp: Float): Int = ((dp + WIDGET_GAP_DP) / (cellDp + WIDGET_GAP_DP)).toInt()

/**
 * How hard a finger [at] a point of a view [length] long pulls it towards an edge, from -1 at the start to 1 at the end:
 * not at all until it is within [zone] of one, then more the nearer it gets.
 */
fun edgePull(at: Float, length: Float, zone: Float): Float = when {
    at > length - zone -> (at - length + zone) / zone
    at < zone -> (at - zone) / zone
    else -> 0f
}.coerceIn(-1f, 1f)

/** The rows the widget page shows, and how tall each is, in dp. */
data class WidgetRows(val count: Int, val heightDp: Float)

/**
 * The rows of at least [WIDGET_ROW_HEIGHT_DP] that fit in [dp], at least one, each stretched so that with the gaps
 * between them they fill it, and no strip of the page is left over.
 */
fun widgetRowsWithin(dp: Float): WidgetRows {
    val count = cellsWithin(dp, WIDGET_ROW_HEIGHT_DP.toFloat()).coerceAtLeast(1)
    return WidgetRows(count, maxOf((dp + WIDGET_GAP_DP) / count - WIDGET_GAP_DP, WIDGET_ROW_HEIGHT_DP.toFloat()))
}

/**
 * How a widget's provider lets it be resized along one axis, in dp: whether it stretches that way at all, its minimum,
 * the lower one it may be resized to (0 when unset), and the most it may be resized to (null when unset).
 */
data class WidgetResize(
    val resizable: Boolean = true,
    val minDp: Int = 0,
    val minResizeDp: Int = 0,
    val maxResizeDp: Int? = null,
)

/** How a widget's provider lets it be resized up and down, and across. */
data class WidgetSizing(val vertical: WidgetResize = WidgetResize(), val horizontal: WidgetResize = WidgetResize())

/**
 * The cells, [cellDp] each, that a widget [cells] long may be resized to along this axis: from those its minimum needs
 * up to those that fit under its maximum, and no more than [most]. A minimum resize length only lowers the minimum, as
 * in the platform launcher. One that does not stretch this way takes only the cells its minimum needs. Its own cells
 * are always in reach too, so a widget stored larger than the page now allows, or outside its provider's limits, does
 * not jump when its handle is taken, and one stored wider than it may be, as the page before places was, can go back.
 */
fun WidgetResize.cellRange(cells: Int, cellDp: Float, most: Int): IntRange {
    val min = widgetCells(minResizeDp.takeIf { resizable && it in 1..minDp } ?: minDp, cellDp, most)
    val max = if (!resizable) min else maxResizeDp?.let { minOf(cellsWithin(it.toFloat(), cellDp), most) } ?: most
    return minOf(min, cells)..maxOf(max.coerceAtLeast(min), cells)
}

/**
 * A drag of [dragDp] on the handle of a widget [cells] long, a cell and a gap being [pitchDp], held at the ends of
 * [range], so a finger that overshoots one moves the widget again as soon as it turns back.
 */
fun heldDrag(cells: Int, dragDp: Float, range: IntRange, pitchDp: Float): Float =
    dragDp.coerceIn((range.first - cells) * pitchDp, (range.last - cells) * pitchDp)

/** The part of a move of [by] from [at] that stays within [low] to [high], none once [at] is past that end already. */
fun roomFor(by: Float, at: Float, low: Float, high: Float): Float =
    if (by > 0f) by.coerceAtMost(maxOf(high - at, 0f)) else by.coerceAtLeast(minOf(low - at, 0f))

/**
 * [cells], and the whole cells nearest a drag of [dragDp] on from there, or back when negative: where a widget's edge
 * or corner lands. A cell and its gap are [pitchDp].
 */
fun nearestCells(cells: Int, dragDp: Float, pitchDp: Float): Int = cells + (dragDp / pitchDp).roundToInt()

/** The page as text, one line per widget: its id, row, column, rows and columns, tab-separated. */
fun WidgetPage.encode(): String = widgets.joinToString(LINE) { listOf(it.id, it.row, it.column, it.rows, it.columns).joinToString(FIELD) }

/**
 * A line that is not a widget, or the ring, in the page's columns is skipped, and so is a second line for the same id, so a damaged
 * file loses that widget and keeps the rest; widgets that overlap move down out of each other's way. A line of just an
 * id and rows is from before widgets had places: those take the page's width at the top, so they stack down it in the
 * order they come.
 */
fun decodeWidgetPage(text: String): WidgetPage {
    val widgets = text.nonEmptyLines()
        .mapNotNull { line ->
            val fields = line.split(FIELD).map { it.toIntOrNull()?.takeIf { n -> n >= 0 } ?: return@mapNotNull null }
            when (fields.size) {
                2 -> HostedWidget(fields[0], 0, 0, fields[1], WIDGET_COLUMNS).takeIf { it.id != RING_ID }
                5 -> HostedWidget(fields[0], fields[1], fields[2], fields[3], fields[4])
                else -> null
            }?.takeIf { it.id >= 0 && it.rows > 0 && it.columns > 0 && it.end <= WIDGET_COLUMNS }
        }
        .distinctBy { it.id }
    return WidgetPage(widgets.sortedWith(WidgetPage.PLACE)).settled()
}
