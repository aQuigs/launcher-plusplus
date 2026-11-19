package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import com.sqftware.orbitlauncher.domain.minimap.ICON_SPACING_PX
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MapView
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import com.sqftware.orbitlauncher.ui.theme.Spacing
import com.sqftware.orbitlauncher.ui.theme.legible
import kotlin.math.min
import kotlin.math.roundToInt

/** The player's arrow and the quest marker on the world map, from the middle of each to its tip. */
private val PLAYER_HALF = 14.dp
private val QUEST_HALF = 16.dp

/**
 * A place's icon on the world map, as wide as the compass shows it, and narrower when the map is zoomed out, to this
 * share of the room the painting keeps between icons, so neighbors never overlap.
 */
private val ICON_SIDE = 22.dp
private const val ICON_OF_SPACING = 0.85f

/**
 * A game's world map in [look]: the local map's ground seen from above, darkening toward the edges, the theme's
 * [drift] over it, the player's arrow turned the way they face and the quest marker.
 */
internal class HudWorldMap(private val look: HudLook, private val drift: Drift) : WorldMapArt {
    override val void = look.colors.fog
    override val ink = look.colors.ink

    // Smooth, as the local map is drawn, not in crisp pixels like the old game's, its icons upright and of one size.
    override fun DrawScope.drawPainting(painting: PaintedMap<ImageBitmap>, topLeft: IntOffset, scale: Float) {
        drawSpriteScaled(painting.ground, topLeft, scale, FilterQuality.Medium)
        val side = min(ICON_SIDE.toPx(), ICON_SPACING_PX.toFloat() * scale * ICON_OF_SPACING).roundToInt()
        val origin = topLeft.toOffset()
        painting.icons.forEach {
            val at = origin + Offset(it.x, it.y) * scale
            if (at.x > -side && at.y > -side && at.x < size.width + side && at.y < size.height + side) drawIcon(it.image, at, side)
        }
    }

    /** The player's arrow on [at], turned the way they face, placed and turned in a layer of its own so the map stays still under it. */
    @Composable
    override fun YouAreHere(at: LatLon, view: () -> MapView) {
        val heading = LocalHeading.current
        Spacer(
            Modifier
                .offset { (view().spot(at) - Offset(PLAYER_HALF.toPx(), PLAYER_HALF.toPx())).round() }
                .size(PLAYER_HALF * 2)
                .graphicsLayer { rotationZ = heading() ?: 0f }
                .drawWithCache {
                    val arrow = playerArrow(size.center, PLAYER_HALF.toPx())
                    val edge = Stroke(1.5.dp.toPx())
                    onDrawBehind {
                        drawPath(arrow, look.colors.ink)
                        drawPath(arrow, look.colors.arrowEdge, style = edge)
                    }
                },
        )
    }

    @Composable
    override fun Over(view: () -> MapView, moving: Boolean) {
        val quest = LocalQuest.current
        val turn = turnAngle(moving, WEATHER_CYCLE_S * 1_000)
        // Each in a layer of its own, so a frame of weather redraws only the weather, and a pan only the quest marker.
        DriftingWeather(drift, { turn.value }, Modifier.fillMaxSize().graphicsLayer())
        Spacer(
            Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                val over = with(look) { screenOver() }
                val fade = Brush.radialGradient(0.5f to Color.Transparent, 1f to look.colors.rim.outer, center = size.center, radius = size.maxDimension * 0.62f)
                onDrawBehind {
                    over()
                    drawRect(fade)
                }
            },
        )
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val marker = QuestMarker(look, QUEST_HALF.toPx())
                onDrawBehind {
                    // Its tip on the spot, as a pin's.
                    quest?.let { with(marker) { draw(view().spot(it) - Offset(0f, marker.half)) } }
                }
            },
        )
    }

    /** The hint as the games' maps lay their own along the foot of the screen, on smoke between hairlines. */
    @Composable
    override fun PickHint(modifier: Modifier) {
        Text(
            "Tap the map to mark your quest",
            style = MaterialTheme.typography.labelLarge.legible(look.colors.ink),
            color = look.colors.ink,
            textAlign = TextAlign.Center,
            modifier = modifier
                .drawWithCache {
                    val smoke = fadedAtEnds(look.colors.smoke, 0.2f)
                    val line = fadedAtEnds(look.colors.line, 0.2f)
                    val hairline = size.copy(height = 1.dp.toPx())
                    onDrawBehind {
                        drawRect(smoke)
                        drawRect(line, size = hairline)
                        drawRect(line, topLeft = Offset(0f, size.height - hairline.height), size = hairline)
                    }
                }
                .padding(vertical = Spacing.Tight),
        )
    }

    @Composable
    override fun Close(onClose: () -> Unit, modifier: Modifier) {
        IconButton(onClick = onClose, modifier = modifier.background(look.colors.smoke, CircleShape)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = look.colors.ink)
        }
    }
}
