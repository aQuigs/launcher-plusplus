package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * A full-screen veil of `surfaceDim` over the launcher. The launcher is laid out clear of the system bars, so the veil
 * carries on through the navigation bar, as the open drawer's does.
 */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val veil = MaterialTheme.colorScheme.surfaceDim
    val navigationBar = WindowInsets.navigationBars

    Surface(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(veil, Offset(0f, size.height), Size(size.width, navigationBar.getBottom(this).toFloat())) },
        color = veil,
        content = content,
    )
}
