package com.sqftware.orbitlauncher.domain

import kotlin.math.ceil
import kotlin.math.roundToInt

/** A widget on the page: the id the system knows it by and how many rows of the page it takes. */
data class HostedWidget(val id: Int, val rows: Int)

/** The widgets on the widget page, top to bottom. */
data class WidgetPage(val widgets: List<HostedWidget> = emptyList()) {
    val isEmpty: Boolean get() = widgets.isEmpty()

    val ids: Set<Int> get() = widgets.mapTo(mutableSetOf()) { it.id }

    fun add(widget: HostedWidget): WidgetPage = WidgetPage(widgets + widget)

    fun remove(id: Int): WidgetPage = WidgetPage(widgets.filter { it.id != id })

    fun resize(id: Int, rows: Int): WidgetPage = WidgetPage(widgets.map { if (it.id == id) it.copy(rows = rows) else it })

    /** The page with the widget [id] taken out and put back at [to], the others closing up around it. */
    fun move(id: Int, to: Int): WidgetPage = WidgetPage(widgets.reordered(widgets.indexOfFirst { it.id == id }, to, ReorderMode.Insert))
}

/** The height of one row of the widget page, in dp. */
const val WIDGET_ROW_HEIGHT_DP = 80

/**
 * The rows a widget takes when its provider asks for at least [minHeightDp]: whole rows of [rowHeightDp], at least one,
 * and no more than the [pageRows] the page shows at once.
 */
fun widgetRows(minHeightDp: Int, pageRows: Int, rowHeightDp: Int = WIDGET_ROW_HEIGHT_DP): Int =
    ceil(minHeightDp / rowHeightDp.toDouble()).toInt().coerceIn(1, pageRows.coerceAtLeast(1))

/**
 * How a widget's provider lets it be resized, in dp: whether it stretches up and down at all, its minimum height, the
 * lower one it may be resized to (0 when unset), and the most it may be resized to (null when unset).
 */
data class WidgetSizing(
    val vertical: Boolean = true,
    val minHeightDp: Int = 0,
    val minResizeHeightDp: Int = 0,
    val maxResizeHeightDp: Int? = null,
)

/**
 * The rows a widget [rows] tall may be resized to: from the whole rows its minimum needs, at least one, up to the rows
 * that fit under its maximum and the [pageRows] the page shows at once. A minimum resize height only lowers the
 * minimum, as in the platform launcher. Its own rows are always in reach, so a widget stored taller than the page now
 * allows, or outside its provider's limits, does not jump when its handle is taken.
 */
fun WidgetSizing.rowRange(rows: Int, pageRows: Int): IntRange {
    val min = widgetRows(minResizeHeightDp.takeIf { it in 1..minHeightDp } ?: minHeightDp, pageRows)
    val max = maxResizeHeightDp?.let { minOf(it / WIDGET_ROW_HEIGHT_DP, pageRows) } ?: pageRows
    return minOf(min, rows)..maxOf(max.coerceAtLeast(min), rows)
}

/**
 * A drag of [dragDp] on the handle of a widget [rows] tall, held at the ends of [range], so a finger that overshoots
 * one moves the widget again as soon as it turns back.
 */
fun heldDrag(rows: Int, dragDp: Float, range: IntRange): Float =
    dragDp.coerceIn((range.first - rows) * WIDGET_ROW_HEIGHT_DP.toFloat(), (range.last - rows) * WIDGET_ROW_HEIGHT_DP.toFloat())

/** The whole rows nearest a handle dragged [dragDp] down, or up when negative, from a widget [rows] tall. */
fun resizedRows(rows: Int, dragDp: Float): Int = rows + (dragDp / WIDGET_ROW_HEIGHT_DP).roundToInt()

/** The page as text, one line per widget: its id and its rows, tab-separated. */
fun WidgetPage.encode(): String = widgets.joinToString(LINE) { "${it.id}$FIELD${it.rows}" }

/**
 * A line that is not two positive whole numbers is skipped, and so is a second line for the same id, so a damaged file
 * loses that widget and keeps the rest.
 */
fun decodeWidgetPage(text: String): WidgetPage = WidgetPage(
    text.nonEmptyLines()
        .mapNotNull { line ->
            val fields = line.split(FIELD).map { it.toIntOrNull()?.takeIf { n -> n > 0 } }
            if (fields.size == 2 && null !in fields) HostedWidget(fields[0]!!, fields[1]!!) else null
        }
        .distinctBy { it.id },
)
