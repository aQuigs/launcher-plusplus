package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.sqftware.orbitlauncher.ui.theme.LocalPanelMark

/**
 * The ground of every full-screen view over the launcher, the drawer's and each [Panel]'s alike: the veil of
 * `surfaceDim`, and the theme's faint marks over it. Views draw it from here rather than picking colours of their own,
 * so a theme reaches all of them at once.
 */
class PanelGround(private val veil: Color, private val mark: Color, private val art: ThemeArt) {
    /** The veil at [alpha] of its own, over [size] from [topLeft]. */
    fun DrawScope.drawVeil(alpha: Float = 1f, topLeft: Offset = Offset.Zero, size: Size = this.size) =
        drawRect(veil.copy(alpha = veil.alpha * alpha), topLeft, size)

    /** The theme's marks at [alpha] of their brightest, drifted by how far a list has [scrolled]. */
    fun DrawScope.drawMarks(alpha: Float = 1f, scrolled: Float = 0f) =
        with(art) { drawBackdrop(mark.copy(alpha = mark.alpha * alpha), scrolled) }
}

@Composable
fun panelGround(): PanelGround {
    val veil = MaterialTheme.colorScheme.surfaceDim
    val mark = LocalPanelMark.current
    val art = LocalThemeArt.current
    // The same ground across recompositions, so the draw lambdas holding it are kept and do not redraw for nothing.
    return remember(veil, mark, art) { PanelGround(veil, mark, art) }
}

/**
 * A full-screen view over the launcher on the [PanelGround]. The launcher is laid out clear of the system bars, so the
 * veil carries on through the navigation bar, as the open drawer's does.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val ground = panelGround()
    // As the launcher's, held whether or not the bars show.
    val navigationBar = WindowInsets.navigationBarsIgnoringVisibility

    Surface(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                with(ground) {
                    drawVeil(size = Size(size.width, size.height + navigationBar.getBottom(this@drawBehind)))
                    drawMarks()
                }
            },
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content,
    )
}
