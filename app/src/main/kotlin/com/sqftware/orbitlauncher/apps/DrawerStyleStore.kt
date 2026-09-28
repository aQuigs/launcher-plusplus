package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.DrawerLayout
import com.sqftware.orbitlauncher.domain.DrawerOrder
import com.sqftware.orbitlauncher.domain.DrawerStyle

interface DrawerStyleStore {
    /** How the drawer last showed the apps, the default style until the user changes it. */
    fun load(): DrawerStyle

    fun save(style: DrawerStyle)
}

class SharedPreferencesDrawerStyleStore(context: Context) : DrawerStyleStore {
    private val prefs = context.getSharedPreferences("drawer", Context.MODE_PRIVATE)

    override fun load(): DrawerStyle {
        val default = DrawerStyle()
        return DrawerStyle(
            layout = DrawerLayout.entries.find { it.name == prefs.getString(LAYOUT, null) } ?: default.layout,
            order = DrawerOrder.entries.find { it.name == prefs.getString(ORDER, null) } ?: default.order,
            mostUsedRow = prefs.getBoolean(MOST_USED_ROW, default.mostUsedRow),
        )
    }

    override fun save(style: DrawerStyle) = prefs.edit {
        putString(LAYOUT, style.layout.name)
        putString(ORDER, style.order.name)
        putBoolean(MOST_USED_ROW, style.mostUsedRow)
    }

    private companion object {
        const val LAYOUT = "layout"
        const val ORDER = "order"
        const val MOST_USED_ROW = "most_used_row"
    }
}
