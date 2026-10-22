package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.ui.theme.Ground
import com.sqftware.orbitlauncher.ui.theme.LocalBoxColors
import com.sqftware.orbitlauncher.ui.theme.LocalGround
import com.sqftware.orbitlauncher.ui.theme.LocalTonalEdge

/** [content] composed on [ground], for the boxes and buttons in it to take their look from. */
@Composable
fun OnGround(ground: Ground, content: @Composable () -> Unit) = CompositionLocalProvider(LocalGround provides ground, content = content)

/**
 * A box on the ground it is composed on: a card, a tile, a setting's group, a strip. Its fill is that ground's, the lit one
 * when [selected], or a card's own [fill] in the user's colour; a [marked] one, such as a drop target, is outlined. A tap
 * anywhere calls [onClick].
 */
@Composable
fun Pane(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    marked: Boolean = false,
    fill: Color = LocalBoxColors.current.let { if (selected) it.lit else it.on(LocalGround.current) },
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val boxes = LocalBoxColors.current
    val contentColor = if (selected) boxes.onLit else MaterialTheme.colorScheme.onSurface
    val border = if (marked) markedEdge() else null

    // A chosen box is ticked as well as lit, as its fill alone may sit close to an unchosen one's.
    val shown: @Composable () -> Unit = {
        Box(propagateMinConstraints = true) {
            content()
            if (selected) PickedMark(Modifier.align(Alignment.TopEnd).wrapContentSize(Alignment.TopEnd).padding(8.dp))
        }
    }
    if (onClick != null) {
        Surface(onClick, modifier, shape = shape, color = fill, contentColor = contentColor, border = border, content = shown)
    } else {
        Surface(modifier, shape = shape, color = fill, contentColor = contentColor, border = border, content = shown)
    }
}

/**
 * A box that floats over other content, such as a bar over the clock, as a small pop-up: on the pop-ups' solid ground and
 * rim, with its content on that ground; a [marked] one is outlined as a marked [Pane] is.
 */
@Composable
fun FloatingPane(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.large, marked: Boolean = false, content: @Composable () -> Unit) {
    Surface(
        modifier.solidGround(panelGround(), shape),
        shape = shape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (marked) markedEdge() else popupRim(),
    ) {
        OnGround(Ground.Popup, content)
    }
}

/** The rim of whatever floats on the pop-ups' ground, which holds its edge over the launcher's own. */
@Composable
fun popupRim() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun markedEdge() = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)

/** How much a [LauncherButton] stands out: as its ground's buttons do, as a main action, or in the error colour. */
enum class Emphasis { Normal, Strong, Destructive }

/**
 * A text button as its ground has them. On the wallpaper and on a panel over it, it is tonal and rimmed so it shows on any
 * patch of wallpaper; on a pop-up's solid ground it is bare text, as a dialog's are, or filled in the primary colour for an
 * [Emphasis.Strong] main action, since a tonal tint would not show there. An [Emphasis.Destructive] one, which erases what
 * the user keeps, is in the error colour.
 */
@Composable
fun LauncherButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: Emphasis = Emphasis.Normal,
    content: @Composable RowScope.() -> Unit,
) {
    val error = MaterialTheme.colorScheme.error

    when {
        LocalGround.current != Ground.Popup -> {
            val colors = if (emphasis == Emphasis.Destructive) {
                ButtonDefaults.filledTonalButtonColors(contentColor = error)
            } else {
                ButtonDefaults.filledTonalButtonColors()
            }
            FilledTonalButton(onClick, modifier, enabled, colors = colors, border = BorderStroke(1.dp, LocalTonalEdge.current), content = content)
        }
        emphasis == Emphasis.Strong -> Button(onClick, modifier, enabled, content = content)
        else -> {
            val colors = if (emphasis == Emphasis.Destructive) ButtonDefaults.textButtonColors(contentColor = error) else ButtonDefaults.textButtonColors()
            TextButton(onClick, modifier, enabled, colors = colors, content = content)
        }
    }
}
