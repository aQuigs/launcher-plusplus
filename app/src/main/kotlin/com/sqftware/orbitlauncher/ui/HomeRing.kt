package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.Bounds
import com.sqftware.orbitlauncher.domain.EMBLEM_FRACTION
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.ringLayout
import com.sqftware.orbitlauncher.domain.ringSlotOffset
import com.sqftware.orbitlauncher.ui.theme.LocalRingColors
import com.sqftware.orbitlauncher.ui.theme.RingInk
import com.sqftware.orbitlauncher.ui.theme.RingShade
import com.sqftware.orbitlauncher.ui.theme.RingSpark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object HomeRingTags {
    const val EMBLEM = "ring_emblem"

    fun slot(app: AppEntry) = "ring_${app.key}"

    fun folder(index: Int) = "ring_folder_$index"
}

/** The size of a ring icon while the ring has room, and of an icon being dragged onto it. */
internal val RING_ICON_SIZE = 56.dp

/** How far ring icons keep inside the ring's box: room for the unread badge's overhang, and a little air besides. */
internal val RING_EDGE_MARGIN = BADGE_OVERHANG + 4.dp

/** The launcher icon's sky, 108 wide, at 2.475 times the emblem's radius, which puts its dust between spark and edge. */
private const val EMBLEM_ART_PER_RADIUS = 2.475f
private const val EMBLEM_SPARK = 0.3f

/** How long the spark takes to turn once: slow enough to read as drift, not a spinner. */
private const val SPARK_TURN_MILLIS = 60_000

/** How long the sky takes to turn once: slower than the spark, so the dust seems farther off. */
private const val SKY_TURN_MILLIS = 600_000

/** How long an open folder's planet takes to reach the centre, and to go back to its slot. */
private const val SPREAD_MILLIS = 380
private const val FOLD_BACK_MILLIS = 320

/** How large an app is, against its size on the ring, while it is still inside its planet. */
private const val FOLDED_SCALE = 0.3f

/** How far round, in radians, an app spirals on its way out of its planet to its slot. */
private const val SPIRAL = 1.4f

/** How much farther out the ring drifts, as a share of its radius, while it steps aside for an open folder. */
private const val DRIFT = 0.5f

/** How much of the emblem's disc an open folder's planet takes in the centre. */
private const val CENTRE_PLANET = 0.62f

/** Fewer items make a point, a line or a triangle whose edges cut across the emblem, so they keep a circle. */
private const val MIN_CONSTELLATION = 4

/** Which of the ring's parts a child of its layout is, so each is measured and placed as that part. */
private sealed interface Part {
    data object Emblem : Part

    /** The open folder's planet, in the centre or on its way there or back. */
    data object Planet : Part

    /** What takes every touch while an opening folder's apps are still on their way out. */
    data object Shield : Part

    /** What is in slot [index] of the ring, or of the open folder. */
    data class Slot(val index: Int) : Part

    data object Held : Part

    /** The ring's item in slot [index], stepping aside as a folder opens. */
    data class Leaving(val index: Int) : Part

    /** App [index] of a folder going back into its planet. */
    data class Folding(val index: Int) : Part
}

/**
 * The [ring] of favourite apps and folders, joined like the stars of the launcher icon, round an emblem. Tap an
 * app to launch it or long-press it for its [menu]; tap a folder to open it or long-press it for its [folderMenu]; tap
 * the emblem to choose the favourites on the ring and in the dock. With [showHint] the emblem invites you to add apps
 * instead of showing its mark. While [highlighted], the disc the ring fills glows as the place an app being dragged would
 * land. A folder is a planet: an [openFolder] glides from its slot into the centre in the emblem's place, its apps
 * spiralling out round it into the slots, each with the [folderAppMenu], while the ring drifts outward and fades; a tap
 * on the planet calls [onCloseFolder], and it goes back the way it came. Each app wears its [unread] count, and a folder
 * the sum of its apps'. With [rearrange], a long press that moves on picks up what is in a slot to move it round the
 * ring, or round the open folder; while one is on the move, the slots show where everything would be if it were
 * dropped. The item at [foldTarget] is lit as the one an app let go now would fold into. [held] is an app dragged out of
 * a folder that has closed under the finger: its icon carries the gesture, so it stays composed, unseen, until the drag
 * ends or the ring makes way for it, showing where it would land as an app from another place does. The emblem's sky and
 * spark turn slowly while the ring is [inSight], and hold still otherwise. A folder opened or closed out of sight, or
 * closed because it changed or went, is in place at once; the [dock]'s items are where a dock folder that closes is
 * still found, and [dockSlot] where the dock shows a folder, in root coordinates, so its planet leaves from there and
 * goes back there.
 */
@Composable
fun HomeRing(
    ring: List<RingItem>,
    showHint: Boolean,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    onOpenFolder: (RingItem.Folder) -> Unit,
    onCloseFolder: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    openFolder: RingItem.Folder? = null,
    menu: AppMenu? = null,
    folderMenu: FolderMenu? = null,
    folderAppMenu: AppMenu? = null,
    unread: UnreadCounts = UnreadCounts(),
    rearrange: Rearrange? = null,
    foldTarget: Int? = null,
    held: AppEntry? = null,
    inSight: Boolean = true,
    dock: List<RingItem> = emptyList(),
    dockSlot: (RingItem.Folder) -> Bounds? = { null },
) {
    val arrival = rearrange?.arriving
    val making = if (openFolder == null) arrival?.preview(ring) else null
    val slots = openFolder?.apps?.size ?: making?.size ?: ring.size
    val glow by animateFloatAsState(if (highlighted) 1f else 0f, label = "ring_glow")
    val marks = LocalRingColors.current
    // Here rather than in the emblem, which an open folder removes, so sky and spark keep their angles across one.
    val turning = inSight && !showHint && openFolder == null
    val spark = turnAngle(turning, SPARK_TURN_MILLIS)
    val skyTurn = turnAngle(turning, SKY_TURN_MILLIS)
    fun Density.layoutOn(side: Float, count: Int) = ringLayout(RING_ICON_SIZE.toPx(), side, count, RING_EDGE_MARGIN.toPx())

    // How far the open folder's planet has come from its slot: 0 there, 1 in the centre with its apps round it. The
    // [planet] is the folder open, or last open until it is back in its slot.
    val spread = remember { Animatable(0f) }
    var planet by remember { mutableStateOf<RingItem.Folder?>(null) }
    // Reopened on its way back, it turns round from where it is.
    val reopening = openFolder != null && planet?.at == openFolder.at
    SideEffect { if (openFolder != null) planet = openFolder }
    // An app dragged out closes its folder under the finger, and the drag carries on from there, not from the planet; and
    // a folder that changed or went has its apps somewhere else to be.
    val folding = planet?.takeIf { openFolder == null && held == null && inSight && it == folderAt(it.at, ring, dock) }
    LaunchedEffect(openFolder?.at) {
        if (openFolder != null) {
            if (!reopening) spread.snapTo(0f)
            if (inSight) spread.animateTo(1f, tween(SPREAD_MILLIS, easing = FastOutSlowInEasing)) else spread.snapTo(1f)
        } else {
            if (folding != null) spread.animateTo(0f, tween(FOLD_BACK_MILLIS, easing = FastOutSlowInEasing))
            spread.snapTo(0f)
            planet = null
        }
    }
    val centred = openFolder ?: folding
    // The ring stays until the folder's apps are all out, and they are only a tap away once they are, so a second tap on
    // the planet cannot launch one still bunched there.
    val spreadingOut by remember(openFolder != null) { derivedStateOf { openFolder != null && spread.value < 1f } }

    val moving = rearrange?.moving
    // One keyed list for the ring and an open folder, keyed outside the branches, so an item keeps its node, and a gesture
    // moving it, while it moves round and while the folder it is dragged out of closes under the finger.
    val items = when {
        openFolder != null -> moving.shown(openFolder.apps).map(RingItem::App)
        making != null -> making
        else -> moving.shown(ring) + listOfNotNull(held?.let(RingItem::App))
    }
    val landsAt = arrival?.to ?: moving?.at

    Layout(
        content = {
            if (openFolder == null || spreadingOut) {
                Emblem(showHint, { skyTurn.value }, { spark.value }, onEdit, Modifier.layoutId(Part.Emblem))
            }
            centred?.let { folder ->
                key(Part.Planet) {
                    CentrePlanet(folder, icon, onClose = onCloseFolder.takeIf { openFolder != null }, { 1f - spread.value }, Modifier.layoutId(Part.Planet))
                }
            }
            val appMenu = if (openFolder != null) folderAppMenu else menu
            items.forEachIndexed { index, item ->
                // By its tag, which names an app or a folder once on the ring.
                key(item.tag) {
                    // The held app is laid out apart, unseen, and is no position to drop on.
                    val slot = if (index < slots) {
                        Modifier.layoutId(Part.Slot(index)).testTag(item.tag).reorderSlot(rearrange, index, landsAt == index)
                            .foldTarget(index == foldTarget, marks.lit)
                    } else {
                        Modifier.layoutId(Part.Held).alpha(0f)
                    }
                    SlotIcon(item, icon, onLaunch, onOpenFolder, slot, appMenu, folderMenu, unread, rearrange?.drag(index))
                }
            }
            // Only pictures of what is going: the ring stepping aside for an opening folder, and a closed folder's apps
            // going back into its planet while the ring returns and takes the touches.
            if (spreadingOut) {
                ring.forEachIndexed { index, item ->
                    key(item.tag) {
                        SlotIcon(item, icon, onLaunch, onOpenFolder, Modifier.layoutId(Part.Leaving(index)).inert(), null, null, unread, null)
                    }
                }
            }
            folding?.apps?.forEachIndexed { index, app ->
                val item = RingItem.App(app)
                key(item.tag) {
                    SlotIcon(item, icon, onLaunch, onOpenFolder, Modifier.layoutId(Part.Folding(index)).inert(), null, null, unread, null)
                }
            }
            if (spreadingOut) Spacer(Modifier.layoutId(Part.Shield).inert())
        },
        modifier = modifier
            .fillMaxSize()
            // Cached, so the glow and the planet moving do not lay the ring out again every frame.
            .drawWithCache {
                val track = Stroke(1.dp.toPx())
                val lineStroke = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round)

                // The lines joining [count] slots, stopped at the edges of their discs: glass the wallpaper shows through.
                fun constellation(count: Int): Pair<Path, Path>? {
                    if (count < MIN_CONSTELLATION) return null
                    val (radius, iconSize) = layoutOn(size.minDimension, count)
                    val stars = List(count) { index ->
                        val (dx, dy) = ringSlotOffset(index, count)
                        size.center + Offset(dx, dy) * radius
                    }
                    val path = Path().apply {
                        stars.forEachIndexed { index, star -> if (index == 0) moveTo(star.x, star.y) else lineTo(star.x, star.y) }
                        close()
                    }
                    return path to Path().apply { stars.forEach { addOval(Rect(it, iconSize / 2)) } }
                }

                // An app on its way in from another place makes way for itself among the ring's.
                val ringCount = making?.size ?: ring.size
                val ringLines = constellation(ringCount)
                val ringRadius = layoutOn(size.minDimension, ringCount).radius
                val planetApps = centred?.apps?.size
                val folderLines = planetApps?.let(::constellation)
                val folderRadius = planetApps?.let { layoutOn(size.minDimension, it).radius }

                fun DrawScope.draw(lines: Pair<Path, Path>?, radius: Float, alpha: Float) {
                    if (alpha <= 0f) return
                    if (lines != null) {
                        clipPath(lines.second, ClipOp.Difference) {
                            drawPath(lines.first, marks.starLine.copy(alpha = (marks.starLine.alpha + 0.3f * glow) * alpha), style = lineStroke)
                        }
                    } else {
                        drawCircle(marks.mark.copy(alpha = (0.22f + 0.48f * glow) * alpha), radius = radius, style = track)
                    }
                }
                onDrawBehind {
                    val out = spread.value
                    if (glow > 0f) drawCircle(marks.mark.copy(alpha = 0.08f * glow), radius = size.minDimension / 2)
                    if (centred == null) {
                        draw(ringLines, ringRadius, 1f)
                    } else {
                        val drift = 1f + DRIFT * out
                        scale(drift, drift) { draw(ringLines, ringRadius, 1f - out) }
                        if (folderRadius != null) draw(folderLines, folderRadius, out * out)
                    }
                }
            },
    ) { measurables, constraints ->
        val side = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val (radius, iconSize) = layoutOn(side, slots)
        val (ringRadius, ringIconSize) = layoutOn(side, ring.size)
        val (folderRadius, folderIconSize) = layoutOn(side, centred?.apps?.size ?: 1)
        val centreSize = (side * EMBLEM_FRACTION).roundToInt()
        val placeables = measurables.map { measurable ->
            val part = measurable.layoutId as Part
            val size = when (part) {
                Part.Emblem, Part.Planet -> centreSize
                is Part.Slot -> iconSize.roundToInt()
                Part.Held -> 0
                is Part.Leaving -> ringIconSize.roundToInt()
                is Part.Folding -> folderIconSize.roundToInt()
                Part.Shield -> null
            }
            part to measurable.measure(size?.let { Constraints.fixed(it, it) } ?: Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
        }
        // Found by place, not by its stored slot, which counts apps that are missing from the ring.
        val planetIndex = centred?.let(ring::indexOf) ?: -1

        layout(constraints.maxWidth, constraints.maxHeight) {
            val middle = Offset(constraints.maxWidth / 2f, constraints.maxHeight / 2f)
            val out = spread.value
            // Where the planet's slot is, as an offset from the centre, and how large: on the ring, or in the dock, which
            // lies outside the ring's box and is read where the dock placed it.
            val dockBounds = centred?.takeIf { it.at.holder == HomePlace.Dock }?.let(dockSlot)
            val planetSlot = when {
                dockBounds != null -> coordinates?.let {
                    Offset((dockBounds.left + dockBounds.right) / 2, (dockBounds.top + dockBounds.bottom) / 2) - it.localToRoot(middle)
                } ?: Offset.Zero
                planetIndex >= 0 -> ringSlotOffset(planetIndex, ring.size).let { (dx, dy) -> Offset(dx, dy) * ringRadius }
                else -> Offset.Zero
            }
            val slotSize = dockBounds?.let { it.right - it.left } ?: ringIconSize
            val planetAt = middle + planetSlot * (1f - out)
            fun slotAt(index: Int, count: Int, radius: Float) = ringSlotOffset(index, count).let { (dx, dy) -> middle + Offset(dx, dy) * radius }

            fun Placeable.placeAt(at: Offset, layer: (GraphicsLayerScope.() -> Unit)? = null) {
                val x = (at.x - width / 2f).roundToInt()
                val y = (at.y - height / 2f).roundToInt()
                if (layer == null) place(x, y) else placeWithLayer(x, y, layerBlock = layer)
            }

            // On its way out of the planet, [out] of the way: at 0 small and unseen at its heart, at 1 in [slot] of [count].
            fun Placeable.placeSpiralling(index: Int, count: Int, radius: Float) {
                val angle = 2 * PI * index / count + (1f - out) * SPIRAL
                placeAt(planetAt + Offset(sin(angle).toFloat(), -cos(angle).toFloat()) * (radius * out)) {
                    scaleX = lerp(FOLDED_SCALE, 1f, out)
                    scaleY = scaleX
                    alpha = (out * 1.6f).coerceAtMost(1f)
                }
            }

            // The ring's item in [index], drifted outward and faded as far as the planet has come.
            fun Placeable.placeDrifting(index: Int) = placeAt(middle + (slotAt(index, ring.size, ringRadius) - middle) * (1f + DRIFT * out)) {
                alpha = if (index == planetIndex) 0f else 1f - out
            }

            placeables.forEach { (part, placeable) ->
                when (part) {
                    Part.Emblem -> if (centred == null) {
                        placeable.placeAt(middle)
                    } else {
                        placeable.placeAt(middle) {
                            scaleX = lerp(1f, 0.4f, out)
                            scaleY = scaleX
                            alpha = 1f - out
                        }
                    }
                    Part.Planet -> placeable.placeAt(planetAt) {
                        scaleX = lerp(slotSize / (centreSize * CENTRE_PLANET), 1f, out)
                        scaleY = scaleX
                    }
                    is Part.Slot -> when {
                        openFolder != null -> placeable.placeSpiralling(part.index, slots, radius)
                        folding != null -> placeable.placeDrifting(part.index)
                        else -> placeable.placeAt(slotAt(part.index, slots, radius))
                    }
                    Part.Held -> placeable.place(0, 0)
                    is Part.Leaving -> placeable.placeDrifting(part.index)
                    is Part.Folding -> placeable.placeSpiralling(part.index, centred?.apps?.size ?: 1, folderRadius)
                    Part.Shield -> placeable.place(0, 0)
                }
            }
        }
    }
}

/** The folder at [at] as [ring] or [dock] shows it now, if it is there. */
private fun folderAt(at: HomePlace.Folder, ring: List<RingItem>, dock: List<RingItem>) =
    (if (at.holder == HomePlace.Ring) ring else dock).find { it is RingItem.Folder && it.at == at }

/**
 * Takes every touch on the item, before anything in it, so what it covers cannot be pressed, and hides it from
 * accessibility: it only pictures something on its way, which is found where it lands.
 */
private fun Modifier.inert(): Modifier = clearAndSetSemantics {}.pointerInput(Unit) {
    awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
}

/**
 * The ring's centre, the heart of the launcher icon's constellation whose stars are the apps round it: the icon's night
 * sky, half see-through so it darkens a bright wallpaper without hiding it, in a disc edged by a hairline, with the
 * icon's spark in the middle, the sky turned to [skyAngle] and the spark to [sparkAngle], or the hint to add apps in its
 * place. Quiet, so the icons stay the eye's first stop.
 */
@Composable
private fun Emblem(showHint: Boolean, skyAngle: () -> Float, sparkAngle: () -> Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sky = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_launcher_background))
    val edgeMark = LocalRingColors.current.mark

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClickLabel = "Choose the apps on the home screen", onClick = onClick)
            .drawBehind { drawCircle(edgeMark.copy(alpha = 0.35f), radius = size.emblemRadius, style = Stroke(1.dp.toPx())) }
            .testTag(HomeRingTags.EMBLEM)
            .semantics { if (!showHint) contentDescription = "Favourites" },
    ) {
        // Sky and spark each on a layer of their own, so turning them changes a property of the layer and nothing is drawn
        // again.
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = skyAngle() }
                .drawWithCache {
                    val outer = size.emblemRadius
                    val disc = Path().apply { addOval(Rect(size.center, outer)) }
                    val side = outer * EMBLEM_ART_PER_RADIUS
                    val art = Size(side, side)
                    val inset = (size.minDimension - side) / 2
                    onDrawBehind { clipPath(disc) { translate(inset, inset) { with(sky) { draw(art, alpha = 0.5f) } } } }
                },
        )
        if (!showHint) {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = sparkAngle() }
                    .drawWithCache {
                        val spark = sparkPath(size.center, size.emblemRadius * EMBLEM_SPARK)
                        onDrawBehind { drawPath(spark, RingSpark) }
                    },
            )
        } else {
            Text(
                text = "Add apps",
                // Shadowed so it still reads where a light wallpaper shows through the disc.
                style = MaterialTheme.typography.labelLarge.copy(shadow = Shadow(RingShade, blurRadius = 6f)),
                color = RingInk,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // Inside the disc, clear of the sky's dust, however large the font.
                modifier = Modifier.fillMaxWidth(0.55f),
            )
        }
    }
}

private val Size.emblemRadius get() = minDimension / 2 * 0.96f

/**
 * An angle in degrees, a turn every [millis] while [turning]. Otherwise no animation asks for frames, so a home page
 * nobody sees does not redraw the window, and the angle holds, so what it turns never jumps.
 */
@Composable
internal fun turnAngle(turning: Boolean, millis: Int): State<Float> {
    val rest = remember { mutableFloatStateOf(0f) }
    if (!turning) return rest

    val from = rest.floatValue
    val turn = rememberInfiniteTransition(label = "turn")
        .animateFloat(from, from + 360f, infiniteRepeatable(tween(millis, easing = LinearEasing)), label = "turn")
    DisposableEffect(Unit) { onDispose { rest.floatValue = turn.value % 360f } }
    return turn
}

/** The launcher icon's four-point spark, [half] from its [centre] to each tip, its sides bowed in as on the icon. */
private fun sparkPath(centre: Offset, half: Float): Path {
    val bow = half * 0.22f
    val (x, y) = centre
    return Path().apply {
        moveTo(x, y - half)
        quadraticTo(x + bow, y - bow, x + half, y)
        quadraticTo(x + bow, y + bow, x, y + half)
        quadraticTo(x - bow, y + bow, x - half, y)
        quadraticTo(x - bow, y - bow, x, y - half)
        close()
    }
}

private val RingItem.tag: String
    get() = when (this) {
        is RingItem.App -> HomeRingTags.slot(app)
        is RingItem.Folder -> HomeRingTags.folder(at.index)
    }

/**
 * An open [folder]'s planet in the ring's centre, where the emblem was, drawn a little smaller than it; a tap there calls
 * [onClose]. Without [onClose] it is only the planet going back to its slot. [inner] fades its previews, as its apps are
 * round it.
 */
@Composable
private fun CentrePlanet(
    folder: RingItem.Folder,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onClose: (() -> Unit)?,
    inner: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(CircleShape)
            .then(if (onClose != null) Modifier.clickable(onClick = onClose).semantics { contentDescription = "Close folder" } else Modifier.clearAndSetSemantics {}),
        contentAlignment = Alignment.Center,
    ) {
        PlanetFace(folder, icon, Modifier.fillMaxSize(CENTRE_PLANET), inner = inner)
    }
}
