package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.ui.theme.TonalEdge

/** A tonal button rimmed so it shows on any patch of wallpaper; the launcher's buttons use it, not [FilledTonalButton]. */
@Composable
fun TonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    FilledTonalButton(onClick, modifier, border = BorderStroke(1.dp, TonalEdge), content = content)
}
