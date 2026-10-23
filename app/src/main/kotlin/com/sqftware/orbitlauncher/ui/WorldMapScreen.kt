package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.apps.WorldMap
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MapShift
import com.sqftware.orbitlauncher.domain.minimap.MapView
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import com.sqftware.orbitlauncher.domain.minimap.PaintedSpan
import com.sqftware.orbitlauncher.domain.minimap.ZoomSteps
import com.sqftware.orbitlauncher.domain.minimap.paintRetryDelay
import com.sqftware.orbitlauncher.ui.theme.Spacing
import com.sqftware.orbitlauncher.ui.theme.WorldMapInk
import com.sqftware.orbitlauncher.ui.theme.WorldMapVoid
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
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

object WorldMapTags {
    const val MAP = "world_map"
}

/**
 * The world map over the whole screen, as the game opens it from its globe: the ground round [here], where the minimap
 * last placed the user and the game's star turns, painted by [map] north up. A drag pans it and a pinch zooms it,
 * settling on the nearest of the game's zoom steps. A new stretch is painted round the view as it nears the edge of the
 * last, which stays until it comes. The game's red cross, or [onClose] otherwise, closes it, as it closes itself when
 * there is nowhere to open it round, as on a return to a launcher the system had ended.
 */
@Composable
fun WorldMapScreen(here: () -> LatLon?, map: WorldMap, onClose: () -> Unit) {
    val start = remember { here() }
    if (start == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val px = gamePixel
    val steps = remember(px) { ZoomSteps(px) }
    var view by remember { mutableStateOf(MapView(start, px.toDouble(), 0, 0)) }
    var painted by remember { mutableStateOf<PaintedMap<ImageBitmap>?>(null) }
    var unreachable by remember { mutableStateOf(false) }

    // A fold changes the game's pixel, and so the steps, without starting the activity again.
    LaunchedEffect(steps) { view = view.copy(scale = steps.nearest(view.scale).toDouble()) }

    fun span() = painted?.let { PaintedSpan(it.centre, it.ground.width, it.ground.height) }
    fun stale() = view.width > 0 && view.needsRepaint(span(), steps)

    // Each paint shows, however long the pinch or drag goes on, and the next starts from wherever the view has got to by
    // then. Only while the launcher is in sight, as a paint waiting on the network would otherwise ask on in the
    // background.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(map, lifecycle, steps) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            snapshotFlow { stale() }.filter { it }.collect {
                var retry = 0
                while (stale()) {
                    val next = map.paint(view.centre, view.nextPainting(span(), steps))
                    unreachable = next == null
                    if (next != null) painted = next else delay(paintRetryDelay(retry++))
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(WorldMapVoid)
            .onSizeChanged { view = view.copy(width = it.width, height = it.height) }
            .pointerInput(steps) {
                coroutineScope {
                    var settling: Job? = null
                    // Kept past the gesture, so a tap that stops a glide starts it again about the same spot.
                    var anchor = Offset.Zero
                    detectPinchPan(
                        onDown = { settling?.cancel() },
                        onMove = { zoom, pan, at ->
                            anchor = at
                            view = view.zoomedBy(zoom.toDouble(), anchor.x.toDouble(), anchor.y.toDouble(), steps)
                                .draggedBy(pan.x.toDouble(), pan.y.toDouble())
                        },
                        // Let go between steps, the map glides on to the nearest, about the same spot under the fingers.
                        onEnd = {
                            val target = steps.nearest(view.scale).toDouble()
                            if (target != view.scale) {
                                settling = launch {
                                    animate(view.scale.toFloat(), target.toFloat(), animationSpec = spring()) { value, _ ->
                                        view = view.zoomedBy(value / view.scale, anchor.x.toDouble(), anchor.y.toDouble(), steps)
                                    }
                                    view = view.copy(scale = target)
                                }
                            }
                        },
                    )
                }
            }
            .drawBehind {
                painted?.let { painting ->
                    // On whole screen pixels, so on a step every map pixel is crisp. A painting's centre is the middle of
                    // its middle pixel, and its icons are in its ground.
                    val ground = painting.ground
                    val middle = Offset(ground.width / 2 + 0.5f, ground.height / 2 + 0.5f)
                    val scale = view.scale.toFloat()
                    drawSpriteScaled(ground, (center + view.onScreen(painting.centre).toOffset() - middle * scale).round(), scale)
                }
            }
            .semantics { contentDescription = "Map of where you are" }
            .testTag(WorldMapTags.MAP),
    ) {
        here()?.let { YouAreHere(it, { view }, px) }
        // The bars are hidden while the map is up, so only a cutout takes room from it.
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
            if (painted == null) {
                val status = if (unreachable) "The map needs a connection." else "Loading - please wait."
                Text(status, color = WorldMapInk, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Center))
            }
            Text(
                MAP_CREDIT,
                color = WorldMapInk,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = Spacing.Tight),
            )
            CloseCross(px, onClose)
        }
    }
}

/**
 * The game's star on [at], the same size at every zoom, turning in the game's steps. Placed and turned in a layer of its
 * own, so neither a pan nor a turn redraws the map under it.
 */
@Composable
private fun YouAreHere(at: LatLon, view: () -> MapView, px: Int) {
    val star = sprite(R.drawable.osrs_1539_0)
    val scale = maxOf(1, px / 2)
    val turn by youAreHereTurn()
    Spacer(
        Modifier
            .offset {
                val shown = view()
                val spot = shown.onScreen(at)
                IntOffset(
                    (shown.width / 2.0 + spot.x - star.width * scale / 2.0).roundToInt(),
                    (shown.height / 2.0 + spot.y - star.height * scale / 2.0).roundToInt(),
                )
            }
            .graphicsLayer { rotationZ = turn }
            .spriteSized(star, scale),
    )
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

private fun MapShift.toOffset() = Offset(x.toFloat(), y.toFloat())

/** The game's red cross that closes its world map, in its top right corner, lit while held. */
@Composable
private fun BoxScope.CloseCross(px: Int, onClose: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val cross = sprite(if (pressed) R.drawable.osrs_540_0 else R.drawable.osrs_539_0)
    Spacer(
        Modifier
            .align(Alignment.TopEnd)
            .padding(Spacing.Tight)
            .clickable(press, indication = null, role = Role.Button, onClick = onClose)
            .semantics { contentDescription = "Close" }
            .spriteSized(cross, px),
    )
}
