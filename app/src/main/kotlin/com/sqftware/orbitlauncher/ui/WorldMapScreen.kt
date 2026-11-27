package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sqftware.orbitlauncher.apps.WorldMap
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MapView
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import com.sqftware.orbitlauncher.domain.minimap.PaintedSpan
import com.sqftware.orbitlauncher.domain.minimap.ZoomSteps
import com.sqftware.orbitlauncher.domain.minimap.paintRetryDelay
import com.sqftware.orbitlauncher.ui.theme.Spacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/** How near its step, as a share of it, a glide ends: closer than shows at the screen's edge, however far out the step. */
private const val SETTLED_WITHIN = 0.001

object WorldMapTags {
    const val MAP = "world_map"
}

/** How a theme's world map looks: the ground behind and over the painting, and the marks it lays on it. */
interface WorldMapArt {
    /** Behind the painted ground, where none has come yet. */
    val void: Color

    /** The map's own words: whether it is loading, and the credit for its data. */
    val ink: Color

    /** How many steps the map zooms out past the old game's farthest, each to half the last's scale: by default none. */
    val farSteps: Int get() = 0

    /** Draws [painting] [scale] screen pixels to each of its own, its top left at [topLeft], and its icons if they are apart. */
    fun DrawScope.drawPainting(painting: PaintedMap<ImageBitmap>, topLeft: IntOffset, scale: Float)

    /** What the theme lays on the ground in the [view], under the user's mark: by default nothing. */
    @Composable
    fun OnGround(view: () -> MapView) = Unit

    /** Marks [at], where the user is, at its place in the [view]. */
    @Composable
    fun YouAreHere(at: LatLon, view: () -> MapView)

    /** What the theme lays over the map in the [view], still unless [moving]: by default nothing. */
    @Composable
    fun Over(view: () -> MapView, moving: Boolean) = Unit

    /** Lays [text], telling the user what a tap on the map does, over the credit: by default not at all. */
    @Composable
    fun PickHint(text: String, modifier: Modifier) = Unit

    /** What closes the map with [onClose]. */
    @Composable
    fun Close(onClose: () -> Unit, modifier: Modifier)
}

/** Where [at] shows on the screen, from its top left. */
internal fun MapView.spot(at: LatLon): Offset = onScreen(at).let { Offset((width / 2.0 + it.x).toFloat(), (height / 2.0 + it.y).toFloat()) }

/**
 * What a tap on the world map does: [onPick] takes the place tapped, or, given a [clearLabel] as there is a pick to
 * clear, null as soon as a press has been held without panning, the finger still down. The map shows the [hint], and
 * offers both to accessibility as [pickLabel], for the middle of the map, and [clearLabel].
 */
data class MapPick(val hint: String, val pickLabel: String, val clearLabel: String?, val onPick: (LatLon?) -> Unit)

/**
 * The world map over the whole screen, as a game opens it: the ground round [here], where the minimap last placed the
 * user, painted by [map] north up and drawn in the theme's [art], still unless [moving]. A drag pans it and a pinch
 * zooms it, settling on the nearest of the zoom steps, and, given a [pick], a tap or a hold picks. A new stretch is
 * painted round the view as it nears the edge of the last, which stays until it comes. The art's close, or [onClose]
 * otherwise, closes it, as it closes itself when there is nowhere to open it round, as on a return to a launcher the
 * system had ended.
 */
@Composable
fun WorldMapScreen(
    here: () -> LatLon?,
    map: WorldMap,
    art: WorldMapArt,
    moving: Boolean,
    pick: MapPick?,
    onClose: () -> Unit,
) {
    val start = remember { here() }
    if (start == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val px = gamePixel
    val steps = remember(px, art) { ZoomSteps(px, art.farSteps) }
    var view by remember { mutableStateOf(MapView(start, px.toDouble(), 0, 0)) }
    var painted by remember { mutableStateOf<PaintedMap<ImageBitmap>?>(null) }
    var unreachable by remember { mutableStateOf(false) }
    val latestPick by rememberUpdatedState(pick)
    val haptics = LocalHapticFeedback.current

    // A fold changes the game's pixel, and so the steps, without starting the activity again.
    LaunchedEffect(steps) { view = view.copy(scale = steps.nearest(view.scale)) }

    fun span() = painted?.let { PaintedSpan(it.centre, it.ground.width, it.ground.height, it.span) }
    fun stale() = view.width > 0 && view.needsRepaint(span(), steps)

    // Whether the view shows ground that is not painted: at first, or where the next cannot be had. Derived, so a pan
    // that changes nothing of it recomposes nothing.
    val unserved by remember(steps) { derivedStateOf { span()?.let { unreachable && view.reachesPast(it, steps) } ?: true } }

    // Whether, within [wait], the view zooms to ground painted from other tiles, which may well be at hand.
    suspend fun zoomedWithin(wait: Duration) = withTimeoutOrNull(wait) { snapshotFlow { steps.spanAt(view.scale) }.drop(1).first() } != null

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
                    if (next != null) {
                        painted = next
                    } else if (zoomedWithin(paintRetryDelay(retry++))) {
                        retry = 0
                    }
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
            .pointerInput(steps) {
                coroutineScope {
                    var settling: Job? = null
                    // A touch that only stops the zoom gliding to its step picks nothing.
                    var stoppedGlide = false
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
                            val target = steps.nearest(view.scale)
                            if (target != view.scale) {
                                settling = launch {
                                    animate(view.scale.toFloat(), target.toFloat(), animationSpec = spring(visibilityThreshold = (target * SETTLED_WITHIN).toFloat())) { value, _ ->
                                        view = view.zoomedBy(value / view.scale, anchor.x.toDouble(), anchor.y.toDouble(), steps)
                                    }
                                    view = view.copy(scale = target)
                                }
                            }
                        },
                        onTap = { at ->
                            val pick = latestPick
                            if (pick != null && !stoppedGlide && painted != null) {
                                pick.onPick(view.at(at.x - view.width / 2.0, at.y - view.height / 2.0))
                            }
                        },
                        onHold = {
                            val pick = latestPick
                            if (pick?.clearLabel != null && !stoppedGlide) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                pick.onPick(null)
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
                    val scale = (view.scale * painting.span).toFloat()
                    with(art) { drawPainting(painting, (view.spot(painting.centre) - middle * scale).round(), scale) }
                }
            }
            .semantics {
                contentDescription = "Map of where you are"
                pick?.let {
                    onClick(it.pickLabel) {
                        it.onPick(view.centre)
                        true
                    }
                    it.clearLabel?.let { label ->
                        onLongClick(label) {
                            it.onPick(null)
                            true
                        }
                    }
                }
            }
            .testTag(WorldMapTags.MAP),
    ) {
        art.OnGround { view }
        here()?.let { art.YouAreHere(it) { view } }
        art.Over({ view }, moving)
        // The bars are hidden while the map is up, so only a cutout takes room from it.
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
            if (unserved) {
                val status = if (unreachable) "The map needs a connection." else "Loading - please wait."
                Text(status, color = art.ink, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Center))
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = Spacing.Tight),
                verticalArrangement = Arrangement.spacedBy(Spacing.Tight),
            ) {
                pick?.let { art.PickHint(it.hint, Modifier.fillMaxWidth()) }
                Text(MAP_CREDIT, color = art.ink, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            art.Close(onClose, Modifier.align(Alignment.TopEnd).padding(Spacing.Tight))
        }
    }
}
