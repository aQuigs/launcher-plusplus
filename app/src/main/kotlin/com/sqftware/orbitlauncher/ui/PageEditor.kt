package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.PageContents
import com.sqftware.orbitlauncher.domain.PageKind
import com.sqftware.orbitlauncher.domain.PageLayout
import com.sqftware.orbitlauncher.domain.WIDGET_COLUMNS
import com.sqftware.orbitlauncher.domain.edgePull

object PageEditorTags {
    const val EDITOR = "page_editor"
    const val DELETE_DIALOG = "page_delete_dialog"

    fun page(page: LauncherPage) = "page_card_${page.id}"

    fun delete(page: LauncherPage) = "page_delete_${page.id}"

    fun add(atStart: Boolean) = if (atStart) "page_add_start" else "page_add_end"
}

private val CARD_WIDTH = 88.dp
private val CARD_HEIGHT = 176.dp
private val CARD_GAP = 12.dp
private val CARD_RADIUS = 10.dp
private val CARD_SHAPE = RoundedCornerShape(CARD_RADIUS)

// A finger dragging a page this near either end of the strip scrolls it, and at the edge itself this fast, a second.
private val EDGE_ZONE = 48.dp
private val EDGE_SPEED = 600.dp

/**
 * The pages of [layout] as a strip of cards in swipe order, each drawn from what [contentsOf] says it holds. A tap on a
 * card hands its page to [onOpen]; a long press lifts it, to drag it along the strip to another place. A + at either end
 * adds a page of the kind picked in a dialog there, until the layout is full. Every page but home has an × that deletes
 * it, at once if it is empty, else once a dialog naming what it holds has asked. Each change is handed to [onLayoutChange].
 */
@Composable
fun PageEditor(
    layout: PageLayout,
    contentsOf: (LauncherPage) -> PageContents,
    onLayoutChange: (PageLayout) -> Unit,
    onOpen: (LauncherPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var addingAtStart by remember { mutableStateOf<Boolean?>(null) }
    var deleting by remember { mutableStateOf<LauncherPage?>(null) }
    val contents = remember(layout, contentsOf) { layout.pages.associateWith(contentsOf) }
    val latestLayout by rememberUpdatedState(layout)
    val latestOnLayoutChange by rememberUpdatedState(onLayoutChange)
    val density = LocalDensity.current
    val rightToLeft = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reorder = remember(density, rightToLeft) { ListReorder(with(density) { CARD_GAP.toPx() }, horizontal = true, mirrored = rightToLeft) }
    SideEffect { reorder.count = layout.pages.size }
    val scroll = rememberScrollState()
    var finger by remember { mutableFloatStateOf(Float.NaN) }

    // The strip scrolls under a finger holding a page near either end, and the page moves with it, so it stays held.
    LaunchedEffect(reorder.dragging != null) {
        if (reorder.dragging == null) return@LaunchedEffect
        val zone = with(density) { EDGE_ZONE.toPx() }
        val speed = with(density) { EDGE_SPEED.toPx() }
        val towardsEnd = if (rightToLeft) -1f else 1f
        scroll.scrollAtEdges(speed, pull = { towardsEnd * edgePull(finger, scroll.viewportSize.toFloat(), zone) }) { reorder.offset += it }
    }

    Panel(modifier.testTag(PageEditorTags.EDITOR)) {
        Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pages", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                "In the order you swipe through them. Hold a page and drag to move it; tap one to go to it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
                modifier = Modifier
                    .padding(top = 24.dp)
                    // Watched ahead of the cards, which take the touches they drag with, and before the scroll, so in
                    // the strip's own place on screen.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) finger = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.pressed }?.position?.x ?: Float.NaN
                        }
                    }
                    .horizontalScroll(scroll)
                    .padding(horizontal = 20.dp),
            ) {
                if (!layout.isFull) AddTile(atStart = true, onClick = { addingAtStart = true })
                layout.pages.forEachIndexed { index, page ->
                    key(page.id) {
                        val held = contents.getValue(page)
                        PageCard(
                            page = page,
                            label = layout.label(page),
                            contents = held,
                            lifted = reorder.dragging == index,
                            // A long press let go without moving reaches the card's tap first, and opens nothing.
                            onOpen = { if (reorder.dragging == null) onOpen(page) },
                            onDelete = { if (held.isEmpty) onLayoutChange(layout.remove(page)) else deleting = page },
                            modifier = Modifier
                                .reorderItem(reorder, index)
                                .pointerInput(index) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { reorder.dragging = index },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            reorder.moveBy(amount.x)
                                        },
                                        onDragEnd = {
                                            reorder.end()?.let { (from, to) ->
                                                if (from != to) latestOnLayoutChange(latestLayout.move(from, to))
                                            }
                                        },
                                        onDragCancel = { reorder.end() },
                                    )
                                },
                        )
                    }
                }
                if (!layout.isFull) AddTile(atStart = false, onClick = { addingAtStart = false })
            }
        }
    }

    addingAtStart?.let { atStart ->
        val side = if (atStart != rightToLeft) "left" else "right"
        ChoiceDialog(
            title = "Add a page on the $side",
            choices = PageKind.entries,
            chosen = null,
            label = { it.name },
            description = { it.blurb },
            onChoose = { kind ->
                addingAtStart = null
                onLayoutChange(layout.add(kind, atStart))
            },
            onDismiss = { addingAtStart = null },
        )
    }
    deleting?.let { page ->
        DeletePageDialog(
            label = layout.label(page),
            contents = contents.getValue(page),
            onDelete = {
                deleting = null
                onLayoutChange(layout.remove(page))
            },
            onDismiss = { deleting = null },
        )
    }
}

private val PageKind.blurb: String
    get() = when (this) {
        PageKind.Ring -> "Its own ring of apps and folders"
        PageKind.Collections -> "Cards of apps: new, most used, by category"
        PageKind.Widgets -> "A grid of widgets"
    }

@Composable
private fun PageCard(
    page: LauncherPage,
    label: String,
    contents: PageContents,
    lifted: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MaterialTheme.colorScheme
    val isHome = page == LauncherPage.Home
    val marked = isHome || lifted

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.testTag(PageEditorTags.page(page))) {
        Box(
            Modifier
                .size(CARD_WIDTH, CARD_HEIGHT)
                .graphicsLayer {
                    if (lifted) {
                        translationY = -8.dp.toPx()
                        rotationZ = -3f
                    }
                }
                .clip(CARD_SHAPE)
                .background(colours.surfaceContainer)
                .border(if (marked) 2.dp else 1.dp, if (marked) colours.primary else colours.outlineVariant, CARD_SHAPE)
                .clickable(onClick = onOpen),
        ) {
            PagePreview(contents, isHome, Modifier.matchParentSize())
            if (isHome) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(6.dp).size(20.dp).background(colours.primary, CircleShape),
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home page", tint = colours.onPrimary, modifier = Modifier.size(13.dp))
                }
            } else {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp).testTag(PageEditorTags.delete(page)),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete $label",
                        modifier = Modifier.size(20.dp).background(colours.surfaceContainerHighest, CircleShape).padding(3.dp),
                    )
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        Text(contents.summary() ?: "Empty", style = MaterialTheme.typography.labelSmall, color = colours.onSurfaceVariant)
    }
}

/** What [this] holds, in a few words, or null when it holds nothing. */
private fun PageContents.summary(): String? {
    if (isEmpty) return null
    return when (this) {
        is PageContents.Ring -> listOfNotNull(counted(apps, "app").takeIf { apps > 0 }, counted(folders, "folder").takeIf { folders > 0 }).joinToString(" and ")
        is PageContents.Cards -> counted(cards, "collection")
        is PageContents.Widgets -> counted(widgets.size, "widget")
    }
}

/** A sketch of a page: the ring and its slots, with home's clock and dock, the cards, or the widgets where they sit. */
@Composable
private fun PagePreview(contents: PageContents, isHome: Boolean, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.outline
    val mark = MaterialTheme.colorScheme.primary

    Canvas(modifier) {
        when (contents) {
            is PageContents.Ring -> {
                if (isHome) drawRoundRect(line, Offset(size.width * 0.3f, size.height * 0.1f), Size(size.width * 0.4f, 6.dp.toPx()), CornerRadius(3.dp.toPx()))
                val centre = Offset(size.width / 2, size.height * if (isHome) 0.48f else 0.45f)
                val radius = size.width * 0.34f
                drawCircle(line, radius, centre, style = Stroke(1.dp.toPx()))
                drawCircle(line, radius * 0.4f, centre, style = Stroke(1.dp.toPx()))
                repeat(contents.slots) { slot ->
                    val angle = Math.toRadians(-90.0 + 360.0 * slot / contents.slots)
                    drawCircle(mark, 4.dp.toPx(), centre + Offset((radius * Math.cos(angle)).toFloat(), (radius * Math.sin(angle)).toFloat()))
                }
                if (contents.dock > 0) {
                    val step = size.width / (contents.dock + 1)
                    repeat(contents.dock) { drawCircle(mark, 4.dp.toPx(), Offset(step * (it + 1), size.height * 0.88f)) }
                }
            }
            is PageContents.Cards -> repeat(minOf(contents.cards, 6)) { card ->
                val top = 10.dp.toPx() + card * 26.dp.toPx()
                drawRoundRect(line, Offset(7.dp.toPx(), top), Size(size.width - 14.dp.toPx(), 20.dp.toPx()), CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx()))
                repeat(4) { drawCircle(mark, 3.dp.toPx(), Offset(16.dp.toPx() + it * 14.dp.toPx(), top + 10.dp.toPx())) }
            }
            is PageContents.Widgets -> {
                val inset = 6.dp.toPx()
                val cell = (size.width - inset * 2) / WIDGET_COLUMNS
                contents.widgets.forEach { widget ->
                    drawRoundRect(
                        mark,
                        Offset(inset + widget.column * cell + 1.dp.toPx(), inset + widget.row * cell + 1.dp.toPx()),
                        Size(widget.columns * cell - 2.dp.toPx(), widget.rows * cell - 2.dp.toPx()),
                        CornerRadius(3.dp.toPx()),
                        style = Stroke(1.5.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTile(atStart: Boolean, onClick: () -> Unit) {
    val edge = MaterialTheme.colorScheme.outline

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp, CARD_HEIGHT)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    edge,
                    Offset(stroke / 2, stroke / 2),
                    Size(size.width - stroke, size.height - stroke),
                    CornerRadius(CARD_RADIUS.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clip(CARD_SHAPE)
            .clickable(onClick = onClick)
            .testTag(PageEditorTags.add(atStart)),
    ) {
        Icon(Icons.Default.Add, contentDescription = if (atStart) "Add a page at the start" else "Add a page at the end", tint = MaterialTheme.colorScheme.primary)
    }
}

/** Asks before deleting the page [label] names, saying what goes with it. Back and a tap outside call [onDismiss]. */
@Composable
private fun DeletePageDialog(label: String, contents: PageContents, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val what = contents.summary().orEmpty()

    GroundDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Delete $label?") },
        text = {
            Text(
                when (contents) {
                    is PageContents.Ring -> "This takes $what off the home screen. The apps stay installed and in the drawer."
                    is PageContents.Cards -> "This deletes $what. The apps stay installed."
                    is PageContents.Widgets -> "This removes $what."
                },
            )
        },
        modifier = Modifier.testTag(PageEditorTags.DELETE_DIALOG),
    )
}
