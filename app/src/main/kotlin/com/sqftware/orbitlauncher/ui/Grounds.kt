package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.ui.theme.Ground
import com.sqftware.orbitlauncher.ui.theme.LocalBoxColors
import com.sqftware.orbitlauncher.ui.theme.LocalGround
import com.sqftware.orbitlauncher.ui.theme.LocalTonalEdge

// Every box and text button of the launcher is one of these, so each takes its look from the ground it is on (held by the
// launcher-surfaces and launcher-buttons hooks) and a ground's look changes for all of them at once.

/** [content] composed on [ground], for the boxes and buttons in it to take their look from. */
@Composable
fun OnGround(ground: Ground, content: @Composable () -> Unit) = CompositionLocalProvider(LocalGround provides ground, content = content)

/**
 * A box on the ground it is composed on: a card, a tile, a setting's group, a strip. Its fill is that ground's box fill,
 * or a [selected] one lit in the primary container, or a card's own [fill] in the user's colour. A [floating] one, over
 * other content, is a small pop-up instead: on the pop-ups' solid ground, with its content on that ground. A tap anywhere
 * calls [onClick].
 */
@Composable
fun Pane(
    modifier: Modifier = Modifier,
    floating: Boolean = false,
    selected: Boolean = false,
    fill: Color = if (selected) MaterialTheme.colorScheme.primaryContainer else LocalBoxColors.current.on(LocalGround.current),
    shape: Shape = MaterialTheme.shapes.large,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val ground = if (floating) modifier.solidGround(panelGround(), shape) else modifier
    val colour = if (floating) Color.Transparent else fill
    val inside: @Composable () -> Unit = if (floating) ({ OnGround(Ground.Popup, content) }) else content

    if (onClick != null) {
        Surface(onClick, ground, shape = shape, color = colour, contentColor = contentColor, border = border, content = inside)
    } else {
        Surface(ground, shape = shape, color = colour, contentColor = contentColor, border = border, content = inside)
    }
}

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
