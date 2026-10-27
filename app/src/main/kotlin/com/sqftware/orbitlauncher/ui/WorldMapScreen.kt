package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
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
import com.sqftware.orbitlauncher.domain.minimap.MapView
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import com.sqftware.orbitlauncher.domain.minimap.PaintedSpan
import com.sqftware.orbitlauncher.domain.minimap.Trail
import com.sqftware.orbitlauncher.domain.minimap.Trails
import com.sqftware.orbitlauncher.domain.minimap.ZoomSteps
import com.sqftware.orbitlauncher.domain.minimap.paintRetryDelay
import com.sqftware.orbitlauncher.ui.theme.Spacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

object WorldMapTags {
    const val MAP = "world_map"
}

internal const val LOG_TRAIL = "Log a trail"
internal const val STOP_TRAIL = "Stop the trail"

/** How a theme's world map looks: the ground behind and over the painting, and the marks it lays on it. */
interface WorldMapArt {
    /** Behind the painted ground, where none has come yet. */
    val void: Color

    /** The map's own words: whether it is loading, and the credit for its data. */
    val ink: Color

    /** Draws [painting] [scale] screen pixels to each of its own, its top left at [topLeft], and its icons if they are apart. */
    fun DrawScope.drawPainting(painting: PaintedMap<ImageBitmap>, topLeft: IntOffset, scale: Float)

    /** Marks [at], where the user is, at its place in the [view]. */
    @Composable
    fun YouAreHere(at: LatLon, view: () -> MapView)

    /** Draws a trail the user logged along [path] on the screen, a game pixel being [gamePixel]. */
    fun DrawScope.drawTrail(path: Path, gamePixel: Int)

    /** A button on the map showing [glyph], read as [label], that [onClick]s. */
    @Composable
    fun MapButton(glyph: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier)

    /** What the theme lays over the map in the [view], still unless [moving]: by default nothing. */
    @Composable
    fun Over(view: () -> MapView, moving: Boolean) = Unit

    /** A line over the credit telling the user a tap on the map marks a place, where one does: by default none. */
    @Composable
    fun PickHint(modifier: Modifier) = Unit

    /** What closes the map with [onClose]. */
    @Composable
    fun Close(onClose: () -> Unit, modifier: Modifier)
}

private fun MapView.trailPath(trail: Trail) = Path().apply {
    trail.forEachIndexed { i, at -> spot(at).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
}

/** Where [at] shows on the screen, from its top left. */
internal fun MapView.spot(at: LatLon): Offset = onScreen(at).let { Offset((width / 2.0 + it.x).toFloat(), (height / 2.0 + it.y).toFloat()) }

/**
 * The world map over the whole screen, as a game opens it: the ground round [here], where the minimap last placed the
 * user, painted by [map] north up and drawn in the theme's [art], still unless [moving]. A drag pans it and a pinch
 * zooms it, settling on the nearest of the zoom steps, and, given [onPick], a tap on the ground hands it the place
 * tapped, as the [pickLabel] action does the middle of the map. A new stretch is painted round the view as it nears the
 * edge of the last, which stays until it comes. The art's close, or [onClose] otherwise, closes it, as it closes itself
 * when there is nowhere to open it round, as on a return to a launcher the system had ended. The [trails] the user
 * logged lie on the ground, and the art's button starts or stops one with [onLogTrail].
 */
@Composable
fun WorldMapScreen(
    here: () -> LatLon?,
    map: WorldMap,
    art: WorldMapArt,
    moving: Boolean,
    onPick: ((LatLon) -> Unit)?,
    pickLabel: String,
    trails: () -> Trails,
    onLogTrail: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
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
    // Read apart from the trails, so a fix redraws the map without composing it again.
    val logging by remember { derivedStateOf { trails().logging } }
    // A touch that only stops the zoom gliding to its step picks nothing.
    var stoppedGlide by remember { mutableStateOf(false) }

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
            .background(art.void)
            .onSizeChanged { view = view.copy(width = it.width, height = it.height) }
            .then(
                if (onPick == null) Modifier else Modifier.pointerInput(onPick) {
                    detectTapGestures { at ->
                        if (painted != null && !stoppedGlide) onPick(view.at(at.x - view.width / 2.0, at.y - view.height / 2.0))
                    }
                },
            )
            .pointerInput(steps) {
                coroutineScope {
                    var settling: Job? = null
                    // Kept past the gesture, so a tap that stops a glide starts it again about the same spot.
                    var anchor = Offset.Zero
                    detectPinchPan(
                        onDown = {
                            stoppedGlide = settling?.isActive == true
                            settling?.cancel()
                        },
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
                    // its middle pixel.
                    val ground = painting.ground
                    val middle = Offset(ground.width / 2 + 0.5f, ground.height / 2 + 0.5f)
                    val scale = view.scale.toFloat()
                    with(art) { drawPainting(painting, (view.spot(painting.centre) - middle * scale).round(), scale) }
                }
                trails().all.forEach { with(art) { drawTrail(view.trailPath(it), px) } }
            }
            .semantics {
                contentDescription = "Map of where you are"
                if (onPick != null) {
                    onClick(pickLabel) {
                        onPick(view.centre)
                        true
                    }
                }
            }
            .testTag(WorldMapTags.MAP),
    ) {
        here()?.let { art.YouAreHere(it) { view } }
        art.Over({ view }, moving)
        // The bars are hidden while the map is up, so only a cutout takes room from it.
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
            if (painted == null) {
                val status = if (unreachable) "The map needs a connection." else "Loading - please wait."
                Text(status, color = art.ink, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Center))
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = Spacing.Tight),
                verticalArrangement = Arrangement.spacedBy(Spacing.Tight),
            ) {
                if (onPick != null) art.PickHint(Modifier.fillMaxWidth())
                Text(MAP_CREDIT, color = art.ink, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            art.MapButton(
                glyph = if (logging) StopGlyph else ImageVector.vectorResource(R.drawable.ic_trail),
                label = if (logging) STOP_TRAIL else LOG_TRAIL,
                onClick = { onLogTrail(!logging) },
                modifier = Modifier.align(Alignment.TopStart).padding(Spacing.Tight),
            )
            art.Close(onClose, Modifier.align(Alignment.TopEnd).padding(Spacing.Tight))
        }
    }
}
