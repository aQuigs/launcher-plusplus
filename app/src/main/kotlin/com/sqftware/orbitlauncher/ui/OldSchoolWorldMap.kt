package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MapView
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import com.sqftware.orbitlauncher.ui.theme.WorldMapInk
import com.sqftware.orbitlauncher.ui.theme.WorldMapVoid
import kotlin.math.roundToInt

/**
 * The game's "you are here" star turns counterclockwise a quarter turn at a time: it rests as a cross, then sweeps
 * steadily around to the next cross without stopping upright, easing only at the ends. Timed frame by frame from a
 * recording of the game.
 */
private const val YOU_ARE_HERE_REST_MS = 270
private const val YOU_ARE_HERE_SWING_MS = 530
private const val YOU_ARE_HERE_QUARTER_MS = YOU_ARE_HERE_REST_MS + YOU_ARE_HERE_SWING_MS
private val YouAreHereSwing = CubicBezierEasing(0.3f, 0.1f, 0.7f, 0.9f)

/** The old game's world map: its pixels crisp on black, its turning star where the user is, and its red cross. */
internal object OldSchoolWorldMap : WorldMapArt {
    override val void = WorldMapVoid
    override val ink = WorldMapInk

    // Its icons are in its ground.
    override fun DrawScope.drawPainting(painting: PaintedMap<ImageBitmap>, topLeft: IntOffset, scale: Float) =
        drawSpriteScaled(painting.ground, topLeft, scale)

    /**
     * The game's star on [at], the same size at every zoom, turning in the game's steps. Placed and turned in a layer of
     * its own, so neither a pan nor a turn redraws the map under it.
     */
    @Composable
    override fun YouAreHere(at: LatLon, view: () -> MapView) {
        val star = sprite(R.drawable.osrs_1539_0)
        val scale = maxOf(1, gamePixel / 2)
        val turn by youAreHereTurn()
        Spacer(
            Modifier
                .offset {
                    val spot = view().spot(at)
                    IntOffset((spot.x - star.width * scale / 2f).roundToInt(), (spot.y - star.height * scale / 2f).roundToInt())
                }
                .graphicsLayer { rotationZ = turn }
                .spriteSized(star, scale),
        )
    }

    /** The game's red cross that closes its world map, in its top right corner, lit while held. */
    @Composable
    override fun Close(onClose: () -> Unit, modifier: Modifier) {
        val press = remember { MutableInteractionSource() }
        val pressed by press.collectIsPressedAsState()
        val cross = sprite(if (pressed) R.drawable.osrs_540_0 else R.drawable.osrs_539_0)
        Spacer(
            modifier
                .clickable(press, indication = null, role = Role.Button, onClick = onClose)
                .semantics { contentDescription = "Close" }
                .spriteSized(cross, gamePixel),
        )
    }
}

@Composable
private fun youAreHereTurn(): State<Float> = rememberInfiniteTransition(label = "you are here").animateFloat(
    initialValue = 0f,
    targetValue = -360f,
    animationSpec = infiniteRepeatable(
        keyframes {
            durationMillis = 4 * YOU_ARE_HERE_QUARTER_MS
            for (quarter in 0 until 4) {
                val from = quarter * YOU_ARE_HERE_QUARTER_MS
                val cross = -90f * quarter
                cross at from using LinearEasing
                cross at from + YOU_ARE_HERE_REST_MS using YouAreHereSwing
            }
        },
    ),
    label = "turn",
)
