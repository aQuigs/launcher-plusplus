package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.Bounds
import com.sqftware.orbitlauncher.domain.EMBLEM_FRACTION
import com.sqftware.orbitlauncher.domain.HangingNames
import com.sqftware.orbitlauncher.domain.NameRoom
import com.sqftware.orbitlauncher.domain.rooms
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.folderSlotOffset
import com.sqftware.orbitlauncher.domain.ringLayout
import com.sqftware.orbitlauncher.domain.ringSlotOffset
import com.sqftware.orbitlauncher.ui.theme.LocalRingColors
import kotlin.math.min
import kotlin.math.roundToInt

object HomeRingTags {
    const val EMBLEM = "ring_emblem"

    fun slot(app: AppEntry) = "ring_${app.key}"

    fun folder(index: Int) = "ring_folder_$index"
}

/** The size of a ring icon while the ring has room, and of an icon being dragged onto it. */
internal val RING_ICON_SIZE = 56.dp

/** How far ring icons keep inside the ring's box: room for the unread badge's overhang, and a little air besides. */
internal val RING_EDGE_MARGIN = BADGE_OVERHANG + 4.dp

/** Air a ring icon's name keeps from its neighbours and their names, more than from its own icon so it reads as that one's. */
private val RING_NAME_AIR = 6.dp

/**
 * How long what the emblem turns fast (Space's spark) takes to turn once: slow enough to read as drift, not a spinner.
 * It is the clock every emblem's motion keeps, so changing it changes them all.
 */
private const val FAST_TURN_MILLIS = 60_000

/** How long what it turns slowly (Space's sky) takes to turn once: slower, so it seems farther off. */
private const val SLOW_TURN_MILLIS = 600_000

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
 * The [ring] of favourite apps and folders round an emblem, both drawn as the theme's [LocalThemeArt] draws them. Tap an
 * app to launch it or long-press it for its [menu]; tap a folder to open it or long-press it for its [folderMenu]; tap
 * the emblem to [onSearch] the apps, or long-press it to [onEdit] the favourites on the ring and in the dock. With a
 * [hint] the emblem says what a tap there does instead of showing its mark, and a tap edits. While [highlighted], the disc the ring fills glows as the place an app being dragged would
 * land. A folder is a planet: an [openFolder] glides from its slot into the centre in the emblem's place, its apps
 * spiralling out round it into the slots, each with the [folderAppMenu], while the ring drifts outward and fades; a tap
 * on the planet calls [onCloseFolder], and it goes back the way it came, and a long press there calls [onAddToFolder].
 * Each app wears its [unread] count, and a folder the sum of its apps'. With [rearrange], a long press that moves on
 * picks up what is in a slot to move it round the ring, or round the open folder; while one is on the move, the slots
 * show where everything would be if it were dropped. The item at [foldTarget] is lit as the one an app let go now would fold into. [held] is an app dragged out of
 * a folder that has closed under the finger: its icon carries the gesture, so it stays composed, unseen, until the drag
 * ends or the ring makes way for it, showing where it would land as an app from another place does. What the emblem
 * turns, turns slowly while the ring [turns], and holds still otherwise; a theme that tells the time shows [minuteOfDay]. A folder opened or closed out of sight, or
 * closed because it changed or went, is in place at once; the [dock]'s items are where a dock folder that closes is
 * still found, and [dockSlot] where the dock shows a folder, in root coordinates, so its planet leaves from there and
 * goes back there. With [names], each app on the ring and in the open folder, and each folder the user named, has its
 * name under it, kept clear of its neighbours, the emblem and the ring's box. The ring shows turned as far as [spin]
 * says, and tells it where it is while it can spin: neither a folder open nor anything on the move.
 */
@Composable
fun HomeRing(
    ring: List<RingItem>,
    hint: String?,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    onOpenFolder: (RingItem.Folder) -> Unit,
    onCloseFolder: () -> Unit,
    onEdit: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    openFolder: RingItem.Folder? = null,
    onAddToFolder: (() -> Unit)? = null,
    menu: AppMenu? = null,
    folderMenu: FolderMenu? = null,
    folderAppMenu: AppMenu? = null,
    unread: UnreadCounts = UnreadCounts(),
    onClearBadge: ((AppEntry) -> Unit)? = null,
    rearrange: Rearrange? = null,
    foldTarget: Int? = null,
    held: AppEntry? = null,
    inSight: Boolean = true,
    turns: Boolean = true,
    minuteOfDay: () -> Int = { 0 },
    dock: List<RingItem> = emptyList(),
    dockSlot: (RingItem.Folder) -> Bounds? = { null },
    names: Boolean = false,
    spin: RingSpin? = null,
) {
    val arrival = rearrange?.arriving
    val making = if (openFolder == null) arrival?.preview(ring) else null
    val slots = openFolder?.apps?.size ?: making?.size ?: ring.size
    val glow by animateFloatAsState(if (highlighted) 1f else 0f, label = "ring_glow")
    val marks = LocalRingColors.current
    val art = LocalThemeArt.current
    // Here rather than in the emblem, which an open folder removes, so what it turns keeps its angle across one.
    val turning = turns && hint == null && openFolder == null
    val fastTurn = turnAngle(turning, FAST_TURN_MILLIS)
    val slowTurn = turnAngle(turning, SLOW_TURN_MILLIS)
    val nameLine = hangingNameLine
    val nameWidths = rememberNameWidths()
    // On a box [width] by [height], whose shorter side the ring takes, and whose longer reaches past it for the bottom name.
    fun Density.namesOn(width: Float, height: Float, items: List<RingItem>, at: (Int, Int) -> Pair<Float, Float>): HangingNames? {
        if (!names) return null
        val widths = items.map { it.hangingName?.let(nameWidths) }
        return HangingNames(APP_LABEL_GAP.toPx(), nameLine.toPx(), RING_NAME_AIR.toPx(), widths, at, (height - min(width, height)) / 2)
    }
    fun Density.layoutOn(width: Float, height: Float, items: List<RingItem>, at: (Int, Int) -> Pair<Float, Float> = ::ringSlotOffset) =
        ringLayout(RING_ICON_SIZE.toPx(), min(width, height), items.size, RING_EDGE_MARGIN.toPx(), namesOn(width, height, items, at))
    // The room each item's name has, by its tag, which only measuring the ring tells, so the names read it from there.
    var rooms by remember { mutableStateOf(emptyMap<String, NameRoom>()) }
    fun nameRoom(item: RingItem): (() -> NameRoom?)? = if (names) ({ rooms[item.tag] }) else null

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
                Emblem(hint, { slowTurn.value }, { fastTurn.value }, minuteOfDay, onSearch, onEdit, Modifier.layoutId(Part.Emblem))
            }
            centred?.let { folder ->
                key(Part.Planet) {
                    CentrePlanet(
                        folder,
                        icon,
                        onClose = onCloseFolder.takeIf { openFolder != null },
                        onAddApps = onAddToFolder.takeIf { openFolder != null },
                        { 1f - spread.value },
                        Modifier.layoutId(Part.Planet),
                    )
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
                    SlotIcon(item, icon, onLaunch, onOpenFolder, slot, appMenu, folderMenu, unread, rearrange?.drag(index), onClearBadge, nameRoom(item))
                }
            }
            // Only pictures of what is going: the ring stepping aside for an opening folder, and a closed folder's apps
            // going back into its planet while the ring returns and takes the touches.
            if (spreadingOut) {
                ring.forEachIndexed { index, item ->
                    key(item.tag) {
                        SlotIcon(item, icon, onLaunch, onOpenFolder, Modifier.layoutId(Part.Leaving(index)).inert(), null, null, unread, null, nameRoom = nameRoom(item))
                    }
                }
            }
            folding?.apps?.forEachIndexed { index, app ->
                val item = RingItem.App(app)
                key(item.tag) {
                    SlotIcon(item, icon, onLaunch, onOpenFolder, Modifier.layoutId(Part.Folding(index)).inert(), null, null, unread, null, nameRoom = nameRoom(item))
                }
            }
            if (spreadingOut) Spacer(Modifier.layoutId(Part.Shield).inert())
        },
        modifier = modifier
            .fillMaxSize()
            // Cached, so the glow and the planet moving do not lay the ring out again every frame.
            .drawWithCache {
                fun marksFor(items: List<RingItem>, slotOffset: (Int, Int) -> Pair<Float, Float>): DrawScope.(Float, Float) -> Unit {
                    val count = items.size
                    val (radius, iconSize) = layoutOn(size.width, size.height, items, slotOffset)
                    val slots = List(count) { index -> slotOffset(index, count).let { (dx, dy) -> size.center + Offset(dx, dy) * radius } }
                    return with(art) { ringMarks(size.center, slots, radius, iconSize, marks) }
                }

                // An app on its way in from another place makes way for itself among the ring's.
                val ringMarks = marksFor(making ?: ring) { index, count -> ringSlotOffset(index, count) }
                val folderMarks = centred?.apps?.let { marksFor(it.map(RingItem::App)) { index, count -> folderSlotOffset(index, count) } }
                onDrawBehind {
                    val out = spread.value
                    val turned = Math.toDegrees((spin?.turn ?: 0f).toDouble()).toFloat()
                    if (glow > 0f) drawCircle(marks.mark.copy(alpha = 0.08f * glow), radius = size.minDimension / 2)
                    if (centred == null) {
                        rotate(turned) { ringMarks(glow, 1f) }
                    } else {
                        val drift = 1f + DRIFT * out
                        scale(drift, drift) { rotate(turned) { ringMarks(glow, 1f - out) } }
                        folderMarks?.invoke(this, glow, out * out)
                    }
                }
            },
    ) { measurables, constraints ->
        val side = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = constraints.maxHeight.toFloat()
        val slotAt: (Int, Int) -> Pair<Float, Float> = { index, count -> if (openFolder != null) folderSlotOffset(index, count) else ringSlotOffset(index, count) }
        val folderAt: (Int, Int) -> Pair<Float, Float> = { index, count -> folderSlotOffset(index, count) }
        // The ring's size follows the order its items are kept in, so it holds still while one is dragged round; only the
        // names follow where each is shown.
        val laid = layoutOn(boxWidth, boxHeight, openFolder?.apps?.map(RingItem::App) ?: making ?: ring, slotAt)
        val ringLaid = layoutOn(boxWidth, boxHeight, ring)
        val folderApps = centred?.apps.orEmpty().map(RingItem::App)
        val folderLaid = layoutOn(boxWidth, boxHeight, folderApps, folderAt)
        if (names) {
            val shown = items.take(slots)
            val named = buildMap {
                ring.zip(ringLaid.names).forEach { (item, room) -> put(item.tag, room) }
                folderApps.zip(folderLaid.names).forEach { (item, room) -> put(item.tag, room) }
                val slotRooms = namesOn(boxWidth, boxHeight, shown, slotAt)?.rooms(laid.iconSize, laid.radius, side).orEmpty()
                shown.zip(slotRooms).forEach { (item, room) -> put(item.tag, room) }
            }
            // Read unobserved, so writing it does not measure the ring again.
            if (named != Snapshot.withoutReadObservation { rooms }) rooms = named
        }
        val (radius, iconSize) = laid
        val (ringRadius, ringIconSize) = ringLaid
        val (folderRadius, folderIconSize) = folderLaid
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
            val turn = (spin?.turn ?: 0f).toDouble()
            val still = centred == null && moving == null && arrival == null && held == null && hint == null && ring.isNotEmpty()
            spin?.place(coordinates.takeIf { still }, centreSize / 2f, ringRadius + ringIconSize / 2)
            // Where the planet's slot is, as an offset from the centre, and how large: on the ring, or in the dock, which
            // lies outside the ring's box and is read where the dock placed it.
            val dockBounds = centred?.takeIf { it.at.holder == HomePlace.Dock }?.let(dockSlot)
            val planetSlot = when {
                dockBounds != null -> coordinates?.let {
                    Offset((dockBounds.left + dockBounds.right) / 2, (dockBounds.top + dockBounds.bottom) / 2) - it.localToRoot(middle)
                } ?: Offset.Zero
                planetIndex >= 0 -> ringSlotOffset(planetIndex, ring.size, turn).let { (dx, dy) -> Offset(dx, dy) * ringRadius }
                else -> Offset.Zero
            }
            val slotSize = dockBounds?.let { it.right - it.left } ?: ringIconSize
            val planetAt = middle + planetSlot * (1f - out)
            fun slotAt(index: Int, count: Int, radius: Float) = ringSlotOffset(index, count, turn).let { (dx, dy) -> middle + Offset(dx, dy) * radius }

            fun Placeable.placeAt(at: Offset, layer: (GraphicsLayerScope.() -> Unit)? = null) {
                val x = (at.x - width / 2f).roundToInt()
                val y = (at.y - height / 2f).roundToInt()
                if (layer == null) place(x, y) else placeWithLayer(x, y, layerBlock = layer)
            }

            // On its way out of the planet, [out] of the way: at 0 small and unseen at its heart, at 1 in [slot] of [count].
            fun Placeable.placeSpiralling(index: Int, count: Int, radius: Float) {
                val (dx, dy) = folderSlotOffset(index, count, (1.0 - out) * SPIRAL)
                placeAt(planetAt + Offset(dx, dy) * (radius * out)) {
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

/** The name that hangs under [this] when the ring shows names: an app's label, or the name the user gave a folder, if any. */
private val RingItem.hangingName: String?
    get() = when (this) {
        is RingItem.App -> app.label
        is RingItem.Folder -> name.ifEmpty { null }
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
 * The ring's centre: the theme's emblem in a disc edged by a hairline, or the [hint] over it. Quiet, so the icons stay
 * the eye's first stop. A tap calls [onSearch] and a long press [onEdit], but with a hint a tap does what it says.
 */
@Composable
private fun Emblem(
    hint: String?,
    slowTurn: () -> Float,
    fastTurn: () -> Float,
    minuteOfDay: () -> Int,
    onSearch: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val art = LocalThemeArt.current
    val edgeMark = LocalRingColors.current.mark

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .combinedClickable(
                onClickLabel = hint ?: "Search apps",
                onLongClickLabel = "Choose the apps on the home screen".takeIf { hint == null },
                onLongClick = onEdit.takeIf { hint == null },
                onClick = if (hint == null) onSearch else onEdit,
            )
            .drawBehind { drawCircle(edgeMark.copy(alpha = 0.35f), radius = size.emblemRadius, style = Stroke(1.dp.toPx())) }
            .testTag(HomeRingTags.EMBLEM)
            .semantics { if (hint == null) contentDescription = "Favourites" },
    ) {
        art.EmblemFace(hint == null, slowTurn, fastTurn, minuteOfDay, Modifier.fillMaxSize())
        if (hint != null) {
            Text(
                text = hint,
                // Shadowed so it still reads where a light wallpaper shows through the disc.
                style = MaterialTheme.typography.labelLarge.copy(shadow = Shadow(art.hintShade, blurRadius = 6f)),
                color = art.hintInk,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // Inside the disc, clear of what the face draws near its edge, however large the font.
                modifier = Modifier.fillMaxWidth(0.55f),
            )
        }
    }
}

internal val Size.emblemRadius get() = minDimension / 2 * 0.96f

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

private val RingItem.tag: String
    get() = when (this) {
        is RingItem.App -> HomeRingTags.slot(app)
        is RingItem.Folder -> HomeRingTags.folder(at.index)
    }

/**
 * An open [folder] in the ring's centre, where the emblem was, drawn a little smaller than it; a tap there calls
 * [onClose] and a long press [onAddApps]. Without [onClose] it is only the planet going back to its slot. [inner] fades
 * its previews, as its apps are round it.
 */
@Composable
private fun CentrePlanet(
    folder: RingItem.Folder,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onClose: (() -> Unit)?,
    onAddApps: (() -> Unit)?,
    inner: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(CircleShape)
            .then(
                if (onClose != null) {
                    Modifier
                        .combinedClickable(onLongClickLabel = onAddApps?.let { "Add apps" }, onLongClick = onAddApps, onClick = onClose)
                        .semantics { contentDescription = "Close folder" }
                } else {
                    Modifier.clearAndSetSemantics {}
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        LocalThemeArt.current.FolderFace(folder, icon, Modifier.fillMaxSize(CENTRE_PLANET).graphicsLayer(), null, inner)
    }
}
