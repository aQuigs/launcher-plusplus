package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.dockIconSize
import kotlin.math.roundToInt

object DockTags {
    const val DOCK = "dock"

    fun slot(app: AppEntry) = "dock_${app.key}"
}

private val FULL_DOCK_ICON_SIZE = 56.dp

/** The user's dock [apps] in one row, each in an equal share of the width. While the dock is empty it draws nothing. */
@Composable
fun Dock(apps: List<AppEntry>, icon: suspend (AppEntry) -> ImageBitmap?, onLaunch: (AppEntry) -> Unit, modifier: Modifier = Modifier) {
    if (apps.isEmpty()) return

    Layout(
        content = { apps.forEach { app -> key(app.key) { AppIcon(app, icon, onLaunch, Modifier.testTag(DockTags.slot(app))) } } },
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp).testTag(DockTags.DOCK),
    ) { measurables, constraints ->
        val slot = constraints.maxWidth.toFloat() / apps.size
        val size = dockIconSize(FULL_DOCK_ICON_SIZE.toPx(), constraints.maxWidth.toFloat(), apps.size).roundToInt()
        val icons = measurables.map { it.measure(Constraints.fixed(size, size)) }

        layout(constraints.maxWidth, size) {
            icons.forEachIndexed { index, placeable -> placeable.place((slot * index + (slot - size) / 2f).roundToInt(), 0) }
        }
    }
}
