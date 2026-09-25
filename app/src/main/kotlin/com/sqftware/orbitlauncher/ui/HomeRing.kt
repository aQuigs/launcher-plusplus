package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.EMBLEM_FRACTION
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.ringLayout
import com.sqftware.orbitlauncher.domain.ringSlotOffset
import com.sqftware.orbitlauncher.ui.theme.RingMark
import com.sqftware.orbitlauncher.ui.theme.RingSpark
import com.sqftware.orbitlauncher.ui.theme.RingStarLine
import kotlin.math.min
import kotlin.math.roundToInt

object HomeRingTags {
    const val EMBLEM = "ring_emblem"

    fun slot(app: AppEntry) = "ring_${app.key}"

    fun folder(index: Int) = "ring_folder_$index"
}

/** The size of a ring icon while the ring has room, and of an icon being dragged onto it. */
internal val RING_ICON_SIZE = 64.dp

/** How far ring icons keep inside the ring's box: room for the unread badge's overhang, and a little air besides. */
internal val RING_EDGE_MARGIN = BADGE_OVERHANG + 4.dp

/** The emblem's hint: the mark at its most present. */
private val Ink = RingMark.copy(alpha = 0.9f)

/** The launcher icon's sky, 108 wide, at 2.475 times the emblem's radius, which puts its dust between spark and edge. */
private const val EMBLEM_ART_PER_RADIUS = 2.475f
private const val EMBLEM_SPARK = 0.3f

/** How long the spark takes to turn once: slow enough to read as drift, not a spinner. */
private const val SPARK_TURN_MILLIS = 60_000

/** Fewer items make a point, a line or a triangle whose edges cut across the emblem, so they keep a circle. */
private const val MIN_CONSTELLATION = 4

/**
 * The [ring] of favourite apps and folders, joined like the stars of the launcher icon, round an emblem. Tap an
 * app to launch it or long-press it for its [menu]; tap a folder to open it or long-press it for its [folderMenu]; tap
 * the emblem to choose the favourites on the ring and in the dock. With [showHint] the emblem invites you to add apps
 * instead of showing its mark. While [highlighted], the disc the ring fills glows as the place an app being dragged would
 * land. An [openFolder] takes the ring over: its apps sit in the slots, each with the [folderAppMenu], and the emblem
 * gives way to a target that calls [onCloseFolder]. Each app wears its [unread] count, and a folder the sum of its apps'.
 * With [rearrange], a long press that moves on picks up what is in a slot to move it round the ring, or round the open
 * folder; while one is on the move, the slots show where everything would be if it were dropped. The item at
 * [foldTarget] is lit as the one an app let go now would fold into. [held] is an app dragged out of a folder that has
 * closed under the finger: its icon carries the gesture, so it stays composed, unseen, until the drag ends.
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
) {
    val slots = openFolder?.apps?.size ?: ring.size
    val glow by animateFloatAsState(if (highlighted) 1f else 0f, label = "ring_glow")
    // The one layout both the drawn lines and the icons follow.
    fun Density.layoutOn(side: Float) = ringLayout(RING_ICON_SIZE.toPx(), side, slots, RING_EDGE_MARGIN.toPx())

    Layout(
        content = {
            val moving = rearrange?.moving
            if (openFolder != null) CloseFolderTarget(onClick = onCloseFolder) else Emblem(showHint = showHint, onClick = onEdit)
            // One keyed list for the ring and an open folder, keyed outside the branches, so an item keeps its node, and a
            // gesture moving it, while it moves round and while the folder it is dragged out of closes under the finger.
            val items = if (openFolder != null) {
                moving.shown(openFolder.apps).map(RingItem::App)
            } else {
                moving.shown(ring) + listOfNotNull(held?.let(RingItem::App))
            }
            val appMenu = if (openFolder != null) folderAppMenu else menu
            items.forEachIndexed { index, item ->
                // By its tag, which names an app or a folder once on the ring.
                key(item.tag) {
                    // The held app is laid out apart, unseen, and is no position to drop on.
                    val slot = if (index < slots) {
                        Modifier.testTag(item.tag).reorderSlot(rearrange, index, moving?.at == index).foldTarget(index == foldTarget)
                    } else {
                        Modifier.alpha(0f)
                    }
                    val drag = rearrange?.drag(index)
                    when (item) {
                        is RingItem.App -> AppIcon(item.app, icon, onLaunch, slot, appMenu, unread[item.app], drag)
                        is RingItem.Folder -> FolderIcon(item, icon, onOpenFolder, slot, folderMenu, unread.sum(item.apps), drag)
                    }
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            // Cached, so the glow animating does not lay the ring out again every frame.
            .drawWithCache {
                val (radius, iconSize) = layoutOn(size.minDimension)
                val track = Stroke(1.dp.toPx())
                val lines = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round)
                val stars = List(slots) { index ->
                    val (dx, dy) = ringSlotOffset(index, slots)
                    size.center + Offset(dx, dy) * radius
                }
                val constellation = Path().apply {
                    stars.forEachIndexed { index, star -> if (index == 0) moveTo(star.x, star.y) else lineTo(star.x, star.y) }
                    close()
                }
                // An icon's disc can be glass the wallpaper shows through, a folder's always is, so the lines stop at its edge.
                val discs = Path().apply { stars.forEach { addOval(Rect(it, iconSize / 2)) } }
                onDrawBehind {
                    if (glow > 0f) drawCircle(RingMark.copy(alpha = 0.08f * glow), radius = size.minDimension / 2)
                    clipPath(discs, ClipOp.Difference) {
                        if (slots >= MIN_CONSTELLATION) {
                            drawPath(constellation, RingStarLine.copy(alpha = RingStarLine.alpha + 0.3f * glow), style = lines)
                        } else {
                            drawCircle(RingMark.copy(alpha = 0.22f + 0.48f * glow), radius = radius, style = track)
                        }
                    }
                }
            },
    ) { measurables, constraints ->
        val side = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val (radius, iconSize) = layoutOn(side)
        val centreSize = (side * EMBLEM_FRACTION).roundToInt()
        val centre = measurables.first().measure(Constraints.fixed(centreSize, centreSize))
        val iconPx = iconSize.roundToInt()
        val icons = measurables.subList(1, 1 + slots).map { it.measure(Constraints.fixed(iconPx, iconPx)) }
        val held = measurables.drop(1 + slots).map { it.measure(Constraints.fixed(0, 0)) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            held.forEach { it.place(0, 0) }
            fun Placeable.placeCentred(x: Float, y: Float) = place((x - width / 2f).roundToInt(), (y - height / 2f).roundToInt())

            val centreX = constraints.maxWidth / 2f
            val centreY = constraints.maxHeight / 2f
            centre.placeCentred(centreX, centreY)
            icons.forEachIndexed { index, placeable ->
                val (dx, dy) = ringSlotOffset(index, slots)
                placeable.placeCentred(centreX + radius * dx, centreY + radius * dy)
            }
        }
    }
}

/**
 * The ring's centre, the heart of the launcher icon's constellation whose stars are the apps round it: the icon's night
 * sky, half see-through so it darkens a bright wallpaper without hiding it, in a disc edged by a hairline, with the
 * icon's spark turning slowly in the middle, or the hint to add apps in its place. Quiet, so the icons stay the eye's
 * first stop.
 */
@Composable
private fun Emblem(showHint: Boolean, onClick: () -> Unit) {
    val sky = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_launcher_background))
    val turn = rememberInfiniteTransition(label = "spark")
    val angle by turn.animateFloat(0f, 360f, infiniteRepeatable(tween(SPARK_TURN_MILLIS, easing = LinearEasing)), label = "spark")

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClickLabel = "Choose the apps on the home screen", onClick = onClick)
            .drawWithCache {
                val outer = size.minDimension / 2 * 0.96f
                val disc = Path().apply { addOval(Rect(size.center, outer)) }
                val edge = Stroke(1.dp.toPx())
                val side = outer * EMBLEM_ART_PER_RADIUS
                val art = Size(side, side)
                val inset = (size.minDimension - side) / 2
                val spark = sparkPath(size.center, outer * EMBLEM_SPARK)
                onDrawBehind {
                    clipPath(disc) { translate(inset, inset) { with(sky) { draw(art, alpha = 0.5f) } } }
                    drawCircle(RingMark.copy(alpha = 0.35f), radius = outer, style = edge)
                    if (!showHint) rotate(angle) { drawPath(spark, RingSpark) }
                }
            }
            .testTag(HomeRingTags.EMBLEM)
            .semantics { if (!showHint) contentDescription = "Favourites" },
    ) {
        if (showHint) {
            Text(
                text = "Add apps",
                // Shadowed so it still reads where a light wallpaper shows through the disc.
                style = MaterialTheme.typography.labelLarge.copy(shadow = Shadow(MaterialTheme.colorScheme.scrim, blurRadius = 6f)),
                color = Ink,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // Inside the disc, clear of the sky's dust, however large the font.
                modifier = Modifier.fillMaxWidth(0.55f),
            )
        }
    }
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
        is RingItem.Folder -> HomeRingTags.folder(index)
    }

/** An item lit as the one an app let go now would fold into: a little larger, ringed with the spark's colour. */
private fun Modifier.foldTarget(lit: Boolean): Modifier = if (!lit) {
    this
} else {
    this
        .semantics { stateDescription = "Drop to put in a folder" }
        .graphicsLayer {
            scaleX = FOLD_TARGET_SCALE
            scaleY = FOLD_TARGET_SCALE
        }
        .drawBehind { drawCircle(RingSpark, radius = size.minDimension / 2 + 3.dp.toPx(), style = Stroke(2.dp.toPx())) }
}

private const val FOLD_TARGET_SCALE = 1.12f

/** The emblem's place while a folder is open: nothing to see, as in Arc, but a tap there closes the folder. */
@Composable
private fun CloseFolderTarget(onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Close folder" },
    )
}
