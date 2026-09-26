package com.sqftware.orbitlauncher.domain

import kotlin.math.ceil
import kotlin.math.roundToInt

/** The columns across the widget page. */
const val WIDGET_COLUMNS = 4

/** The least height of a row of the widget page, in dp: the rows stretch to share the page's height. */
const val WIDGET_ROW_HEIGHT_DP = 80

/** The space between two cells of the widget page, in dp, which a widget spanning both covers too. */
const val WIDGET_GAP_DP = 8

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
 * The widgets on the widget page, each in cells of its own, top to bottom and then left to right. Cells no widget takes
 * stay empty, so a widget keeps its place when another leaves or moves.
 */
data class WidgetPage(val widgets: List<HostedWidget> = emptyList()) {
    val isEmpty: Boolean get() = widgets.isEmpty()

    val ids: Set<Int> get() = widgets.mapTo(mutableSetOf()) { it.id }

    /** The rows down to the bottom of the lowest widget. */
    val rows: Int get() = widgets.maxOfOrNull { it.bottom } ?: 0

    /** The page with the widget [id] in the first free cells, from the top and then the left, that hold [rows] by [columns]. */
    fun add(id: Int, rows: Int, columns: Int): WidgetPage {
        val width = columns.coerceIn(1, WIDGET_COLUMNS)
        val spot = generateSequence(0) { it + 1 }
            .flatMap { row -> (0..WIDGET_COLUMNS - width).asSequence().map { HostedWidget(id, row, it, rows.coerceAtLeast(1), width) } }
            .first { new -> widgets.none(new::overlaps) }
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

/**
 * The cells, [cellDp] each with a gap between two, that a widget needs to be at least [minDp] long: at least one, and
 * no more than [most].
 */
fun widgetCells(minDp: Int, cellDp: Float, most: Int): Int =
    ceil((minDp + WIDGET_GAP_DP) / (cellDp + WIDGET_GAP_DP)).toInt().coerceIn(1, most.coerceAtLeast(1))

/** The most cells, [cellDp] each with a gap between two, that fit in [dp]. */
fun cellsWithin(dp: Float, cellDp: Float): Int = ((dp + WIDGET_GAP_DP) / (cellDp + WIDGET_GAP_DP)).toInt()

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

/**
 * [cells], and the whole cells nearest a drag of [dragDp] on from there, or back when negative: where a widget's edge
 * or corner lands. A cell and its gap are [pitchDp].
 */
fun nearestCells(cells: Int, dragDp: Float, pitchDp: Float): Int = cells + (dragDp / pitchDp).roundToInt()

/** The page as text, one line per widget: its id, row, column, rows and columns, tab-separated. */
fun WidgetPage.encode(): String = widgets.joinToString(LINE) { listOf(it.id, it.row, it.column, it.rows, it.columns).joinToString(FIELD) }

/**
 * A line that is not a widget in the page's columns is skipped, and so is a second line for the same id, so a damaged
 * file loses that widget and keeps the rest; widgets that overlap move down out of each other's way. A line of just an
 * id and rows is from before widgets had places: those take the page's width at the top, so they stack down it in the
 * order they come.
 */
fun decodeWidgetPage(text: String): WidgetPage {
    val widgets = text.nonEmptyLines()
        .mapNotNull { line ->
            val fields = line.split(FIELD).map { it.toIntOrNull()?.takeIf { n -> n >= 0 } ?: return@mapNotNull null }
            when (fields.size) {
                2 -> HostedWidget(fields[0], 0, 0, fields[1], WIDGET_COLUMNS)
                5 -> HostedWidget(fields[0], fields[1], fields[2], fields[3], fields[4])
                else -> null
            }?.takeIf { it.id > 0 && it.rows > 0 && it.columns > 0 && it.end <= WIDGET_COLUMNS }
        }
        .distinctBy { it.id }
    return WidgetPage(widgets.sortedWith(WidgetPage.PLACE)).settled()
}
