package com.aquigs.launcherplusplus.domain

import kotlin.math.ceil

/** A widget on the page: the id the system knows it by and how many rows of the page it takes. */
data class HostedWidget(val id: Int, val rows: Int)

/** The widgets on the widget page, top to bottom. */
data class WidgetPage(val widgets: List<HostedWidget> = emptyList()) {
    val isEmpty: Boolean get() = widgets.isEmpty()

    val ids: Set<Int> get() = widgets.mapTo(mutableSetOf()) { it.id }

    fun add(widget: HostedWidget): WidgetPage = WidgetPage(widgets + widget)

    fun remove(id: Int): WidgetPage = WidgetPage(widgets.filter { it.id != id })
}

/** The height of one row of the widget page, in dp. */
const val WIDGET_ROW_HEIGHT_DP = 80

/**
 * The rows a widget takes when its provider asks for at least [minHeightDp]: whole rows of [rowHeightDp], at least one,
 * and no more than the [pageRows] the page shows at once.
 */
fun widgetRows(minHeightDp: Int, pageRows: Int, rowHeightDp: Int = WIDGET_ROW_HEIGHT_DP): Int =
    ceil(minHeightDp / rowHeightDp.toDouble()).toInt().coerceIn(1, pageRows.coerceAtLeast(1))

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
