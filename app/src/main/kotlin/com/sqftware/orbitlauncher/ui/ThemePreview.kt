package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.Theme
import com.sqftware.orbitlauncher.ui.theme.LocalTonalEdge

object ThemePreviewTags {
    const val BAR = "theme_preview_bar"
    const val USE = "theme_preview_use"
    const val CLOSE = "theme_preview_close"
}

/**
 * The home [clock], or while a theme is tried the [bar] trying it in its place. The clock keeps its room, so the ring and
 * the dock below show where they will in the theme.
 */
@Composable
fun ClockOrThemePreview(bar: (@Composable () -> Unit)?, clock: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(if (bar != null) Modifier.measuredOnly() else Modifier) { clock() }
        bar?.invoke()
    }
}

// Measured for its room but never placed, so it neither draws nor takes touches; an unplaced node still has semantics.
private fun Modifier.measuredOnly() = clearAndSetSemantics {}.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {}
}

/**
 * The bar trying a theme on the user's own home: chips switch the [shown] theme ([onShow]) and pick its [folderLook]
 * ([onFolderLookChange]), saved as a pick from the menu is. [onUse] keeps the theme; [onClose] returns to the one in use.
 */
@Composable
fun ThemePreviewBar(
    shown: Theme,
    onShow: (Theme) -> Unit,
    folderLook: FolderLook,
    onFolderLookChange: (FolderLook) -> Unit,
    onUse: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().testTag(ThemePreviewTags.BAR),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, LocalTonalEdge.current),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Theme preview",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClose, Modifier.testTag(ThemePreviewTags.CLOSE)) { Text("Close") }
                TonalButton(onUse, Modifier.testTag(ThemePreviewTags.USE)) { Text("Use ${shown.label}") }
            }
            Chips(Theme.entries, shown, Theme::label, onShow)
            Chips(shown.folderLooks, folderLook, { "${it.label} folders" }, onFolderLookChange)
        }
    }
}

@Composable
private fun <T> Chips(choices: List<T>, chosen: T, label: (T) -> String, onChoose: (T) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        choices.forEach { choice ->
            FilterChip(selected = choice == chosen, onClick = { onChoose(choice) }, label = { Text(label(choice)) })
        }
    }
}
