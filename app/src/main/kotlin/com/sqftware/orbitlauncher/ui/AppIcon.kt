package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.EntryKind
import com.sqftware.orbitlauncher.domain.Unread
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * One app as a round icon, named by its label for screen readers, with a badge for its [unread] notifications. A tap
 * launches it, a double tap [clears][onClearBadge] what its badge holds of the notifications dismissed unread, a long
 * press opens its [menu], and a long press that goes on becomes a [drag]; its parent decides the size. Given a
 * [nameWidth], its name hangs under that size, outside it, so the slot keeps its place and the name moves and fades
 * with it.
 */
@Composable
fun AppIcon(
    app: AppEntry,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    menu: AppMenu? = null,
    unread: Unread = Unread.None,
    drag: AppDrag? = null,
    onClearBadge: ((AppEntry) -> Unit)? = null,
    nameWidth: NameWidth? = null,
) {
    val presses = remember { MutableInteractionSource() }

    Box(
        modifier
            .launchable(app, onLaunch, menu, presses, onClearBadge.clearing(unread))
            .itemDrag(app, drag)
            .semantics { contentDescription = app.label.withUnread(unread.count) },
    ) {
        IconDisc(presses, Modifier.fillMaxSize()) { AppImage(app, icon, Modifier.fillMaxSize()) }
        menu?.content?.invoke(app)
        UnreadBadge(unread, Modifier.align(Alignment.TopEnd))
        nameWidth?.let { HangingName(app.label, it) }
    }
}

/** How wide a name hanging under an icon may be, given the icon's width, both in pixels: what room its neighbours leave. */
typealias NameWidth = (iconWidth: Int) -> Int

/** How far a hanging name reaches under its icon. */
internal val hangingNameRoom: Dp
    @Composable get() = APP_LABEL_GAP + with(LocalDensity.current) { appLabelStyle.lineHeight.toDp() }

/**
 * [label] as an [AppLabel] [width] wide, centred under its parent and outside it, taking no height. Not a target, and
 * hidden from screen readers, which hear the icon's own name.
 */
@Composable
internal fun HangingName(label: String, width: NameWidth) {
    AppLabel(
        label,
        Modifier
            .layout { measurable, constraints ->
                val name = measurable.measure(Constraints.fixedWidth(width(constraints.maxWidth)))
                // As wide as its parent, so it starts where the parent does either way round and centres the same.
                layout(constraints.maxWidth, 0) { name.place((constraints.maxWidth - name.width) / 2, constraints.maxHeight) }
            }
            .semantics { hideFromAccessibility() },
    )
}

/**
 * The round face of an icon: [content] on a tinted disc, clipped to it, rippling for the presses in [presses] when it
 * has any. The press handling, the badge and the name stay on the icon's own node outside, so the badge can overhang the
 * disc while a screen reader still meets one icon, and the ripple keeps to the disc all the same. The whole square is
 * the target, badge included, as on other launchers.
 */
@Composable
fun IconDisc(
    presses: InteractionSource? = null,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (presses != null) Modifier.indication(presses, ripple()) else Modifier),
        contentAlignment = contentAlignment,
        content = content,
    )
}

private val APP_LABEL_GAP = 4.dp

private val appLabelStyle @Composable get() = MaterialTheme.typography.labelMedium

/** An app's name under its icon, on one line. */
@Composable
fun AppLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = appLabelStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(top = APP_LABEL_GAP),
    )
}

/** An app's icon alone, with no name and nothing to tap, or a pair's; blank until it has loaded. */
@Composable
fun AppImage(app: AppEntry, icon: suspend (AppEntry) -> ImageBitmap?, modifier: Modifier = Modifier) {
    val kind = app.kind
    if (kind is EntryKind.AppPair) PairImage(kind, icon, modifier) else OwnImage(app, icon, modifier)
}

/** How much of a pair's square each of its apps' icons takes. */
private const val PAIR_ICON_FRACTION = 0.58f

/** How far the second of a pair's icons sits from the first, of the room the square leaves them, as far as keeps both in the disc. */
private const val PAIR_ICON_BIAS = 0.7f

/** How far the first of a pair's icons is cut back round the second, which parts them where they overlap. */
private val PAIR_GAP = 1.5.dp

/** How far, across and down, the second of a pair's icons, [side] wide, sits from the first. */
private fun secondIconOffset(side: Int): Int = ((side / PAIR_ICON_FRACTION - side) * PAIR_ICON_BIAS).roundToInt()

/**
 * A pair's two icons in one square, the first towards the top start and the second, over it, towards the bottom end. The
 * gap between them is cut out of the first, so whatever is behind shows through it, a disc or the sky of a folder.
 */
@Composable
private fun PairImage(pair: EntryKind.AppPair, icon: suspend (AppEntry) -> ImageBitmap?, modifier: Modifier) {
    Layout(
        content = {
            OwnImage(pair.first, icon, Modifier.clip(CircleShape).cutAroundSecond())
            OwnImage(pair.second, icon, Modifier.clip(CircleShape))
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val side = min(constraints.maxWidth, constraints.maxHeight)
        val iconSide = (side * PAIR_ICON_FRACTION).roundToInt()
        val (first, second) = measurables.map { it.measure(Constraints.fixed(iconSide, iconSide)) }
        val apart = secondIconOffset(iconSide)
        val inset = (side - iconSide - apart) / 2
        layout(side, side) {
            first.placeRelative(inset, inset)
            second.placeRelative(inset + apart, inset + apart)
        }
    }
}

// The icons are placed relative to the layout direction, so right to left the second is to the left of the first.
private fun Modifier.cutAroundSecond() = drawWithCache {
    val apart = secondIconOffset(size.width.roundToInt()).toFloat()
    val across = if (layoutDirection == LayoutDirection.Ltr) apart else -apart
    val cut = Path().apply { addOval(Rect(size.center + Offset(across, apart), size.width / 2 + PAIR_GAP.toPx())) }
    onDrawWithContent { clipPath(cut, ClipOp.Difference) { this@onDrawWithContent.drawContent() } }
}

@Composable
private fun OwnImage(app: AppEntry, icon: suspend (AppEntry) -> ImageBitmap?, modifier: Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, app.key) {
        // Handed another app, as a folder's preview is when an app leaves it, the cell must not keep showing the old one.
        value = null
        value = icon(app)
    }

    Box(modifier) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
