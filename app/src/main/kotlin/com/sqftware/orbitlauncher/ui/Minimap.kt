package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.Journal
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hands [draw] each of [painted]'s icons and where it lies once the map's ground is drawn [scale] times over round
 * [center] and turned with the phone's [heading]: placed by hand rather than turned back on a turned canvas, so the
 * icon can be drawn upright, as the games keep them.
 */
internal inline fun forEachUprightIcon(
    painted: PaintedMap<ImageBitmap>,
    center: Offset,
    scale: Float,
    heading: Float,
    draw: (ImageBitmap, Offset) -> Unit,
) {
    val turn = Math.toRadians(-heading.toDouble())
    val cos = cos(turn).toFloat()
    val sin = sin(turn).toFloat()
    val middle = painted.ground.width / 2
    painted.icons.forEach { icon ->
        val dx = (icon.x - middle) * scale
        val dy = (icon.y - middle) * scale
        draw(icon.image, center + Offset(dx * cos - dy * sin, dx * sin + dy * cos))
    }
}

/** The credit the map data's licences ask to show with the map. */
internal const val MAP_CREDIT = "© OpenMapTiles © OpenStreetMap"
private const val CREDIT_MILLIS = 5_000L
private const val CREDIT_FADE_MILLIS = 1_000

/**
 * The alpha of the map's credit over the map: whole each time a map of where the user is comes to be [shown], fading
 * five seconds later, as OpenStreetMap's attribution rules allow; the launcher's menu keeps it a tap away.
 */
@Composable
internal fun mapCreditAlpha(shown: Boolean): Animatable<Float, AnimationVector1D> {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(shown) {
        if (shown) {
            alpha.snapTo(1f)
            delay(CREDIT_MILLIS)
            alpha.animateTo(0f, tween(CREDIT_FADE_MILLIS))
        }
    }
    return alpha
}

/**
 * The minimap of where the user is: a 152 px square holding an opaque disc on clear corners, and the icons on it, meant
 * to be scaled up by a whole number with FilterQuality.None so their pixels stay crisp. Null while "Show where I am" is off, location access
 * is denied, or no map has been drawn yet. It changes as the user moves, so it is read where it is drawn.
 */
val LocalMinimap = staticCompositionLocalOf<() -> PaintedMap<ImageBitmap>?> { { null } }

/**
 * The minimap of a place that is only on the map, drawn as [LocalMinimap] is, for while that has no map of where the user
 * is. Null while it has, or no map has been drawn yet. It changes as the player walks, so it is read where it is drawn.
 */
val LocalMadeUpMap = staticCompositionLocalOf<() -> PaintedMap<ImageBitmap>?> { { null } }

/**
 * Which way the phone faces, in degrees clockwise from north, or null without a compass or while the map is off. Read
 * where it is drawn, so its many small changes redraw only what turns with it.
 */
val LocalHeading = staticCompositionLocalOf<() -> Float?> { { null } }

/** Opens the world map over the launcher, round where the minimap last placed the user. */
val LocalOpenWorldMap = staticCompositionLocalOf<() -> Unit> { {} }

/** The user's [journal] of their travels, and where a change to it goes. */
class KeptJournal(val journal: Journal, val onChange: (Journal) -> Unit)

/** The journal a theme that marks quests and discoveries reads and writes; it changes seldom, so only its readers recompose. */
val LocalJournal = compositionLocalOf { KeptJournal(Journal()) {} }
