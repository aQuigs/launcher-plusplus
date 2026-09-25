package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.WidgetPage
import com.sqftware.orbitlauncher.domain.decodeWidgetPage
import com.sqftware.orbitlauncher.domain.encode

/**
 * A widget pick the system has not answered yet: the id allocated for it, the rows the page had room for, and how wide
 * a column of the page was, in dp.
 */
data class WidgetPick(val id: Int, val pageRows: Int, val columnWidthDp: Int)

interface WidgetPageStore {
    fun load(): WidgetPage

    fun save(page: WidgetPage)

    /** The pick in progress, kept so that its answer finds it when the activity has been recreated meanwhile. */
    fun loadPick(): WidgetPick?

    fun savePick(pick: WidgetPick?)
}

class SharedPreferencesWidgetPageStore(context: Context) : WidgetPageStore {
    private val prefs = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    override fun load() = decodeWidgetPage(prefs.getString(PAGE_KEY, null).orEmpty())

    override fun save(page: WidgetPage) = prefs.edit { putString(PAGE_KEY, page.encode()) }

    override fun loadPick(): WidgetPick? {
        val fields = prefs.getString(PICK_KEY, null)?.split(FIELD)?.map(String::toIntOrNull) ?: return null
        return if (fields.size == 3 && null !in fields) WidgetPick(fields[0]!!, fields[1]!!, fields[2]!!) else null
    }

    override fun savePick(pick: WidgetPick?) = prefs.edit {
        if (pick == null) remove(PICK_KEY) else putString(PICK_KEY, "${pick.id}$FIELD${pick.pageRows}$FIELD${pick.columnWidthDp}")
    }

    private companion object {
        const val PAGE_KEY = "widgets"
        const val PICK_KEY = "pick"
        const val FIELD = "\t"
    }
}
