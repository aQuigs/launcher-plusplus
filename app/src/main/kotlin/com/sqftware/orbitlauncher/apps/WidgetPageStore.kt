package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.WidgetPages
import com.sqftware.orbitlauncher.domain.decodeWidgetPage
import com.sqftware.orbitlauncher.domain.encode

/**
 * A widget pick the system has not answered yet: the id allocated for it, the [page] it goes on, the rows that page had
 * room for, how wide a column and how tall a row of it were, in dp, and whether it [lengthens] past those rows.
 */
data class WidgetPick(
    val id: Int,
    val page: String,
    val pageRows: Int,
    val columnWidthDp: Float,
    val rowHeightDp: Float,
    val lengthens: Boolean = true,
)

interface WidgetPageStore {
    fun load(): WidgetPages

    fun save(pages: WidgetPages)

    /** The pick in progress, kept so that its answer finds it when the activity has been recreated meanwhile. */
    fun loadPick(): WidgetPick?

    fun savePick(pick: WidgetPick?)
}

class SharedPreferencesWidgetPageStore(context: Context) : WidgetPageStore {
    private val prefs = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    override fun load() = WidgetPages(
        prefs.all.keys.mapNotNull { key -> pageOf(key)?.let { it to decodeWidgetPage(prefs.getString(key, null).orEmpty()) } }.toMap(),
    )

    override fun save(pages: WidgetPages) = prefs.edit {
        prefs.all.keys.filter { key -> pageOf(key)?.let { it !in pages.pages } == true }.forEach { remove(it) }
        pages.pages.forEach { (page, widgets) -> putString(keyOf(page), widgets.encode()) }
    }

    override fun loadPick(): WidgetPick? {
        // A pick stored before grid pages has no sixth field: its page lengthens unless it has a ring.
        val fields = prefs.getString(PICK_KEY, null)?.split(FIELD)?.takeIf { it.size in 5..6 } ?: return null
        val (id, page, pageRows, columnWidthDp, rowHeightDp) = fields
        return WidgetPick(
            id.toIntOrNull() ?: return null,
            page,
            pageRows.toIntOrNull() ?: return null,
            columnWidthDp.toFloatOrNull() ?: return null,
            rowHeightDp.toFloatOrNull() ?: return null,
            fields.getOrNull(5)?.toBooleanStrictOrNull() ?: true,
        )
    }

    override fun savePick(pick: WidgetPick?) = prefs.edit {
        if (pick == null) {
            remove(PICK_KEY)
        } else {
            putString(PICK_KEY, listOf(pick.id, pick.page, pick.pageRows, pick.columnWidthDp, pick.rowHeightDp, pick.lengthens).joinToString(FIELD))
        }
    }

    private companion object {
        // The first page's key predates the others; keeping its name keeps the widgets already placed.
        const val PAGE_KEY = "widgets"
        const val PAGE_PREFIX = "widgets:"
        const val PICK_KEY = "pick"
        const val FIELD = "\t"

        fun keyOf(page: String) = if (page == LauncherPage.Widgets.id) PAGE_KEY else PAGE_PREFIX + page

        fun pageOf(key: String) =
            if (key == PAGE_KEY) LauncherPage.Widgets.id else key.removePrefix(PAGE_PREFIX).takeIf { key.startsWith(PAGE_PREFIX) }
    }
}
