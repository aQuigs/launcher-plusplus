package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.MAX_FOLDER_NAME
import com.sqftware.orbitlauncher.domain.NameRoom
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.Unread
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.folderSlotOffset
import kotlin.math.min
import kotlin.math.roundToInt

object FolderTags {
    const val MENU = "folder_options"
    const val PLANET_DIALOG = "folder_planet_dialog"
    const val NAME_DIALOG = "folder_name_dialog"
    const val NAME = "folder_name"

    fun preview(index: Int) = "folder_preview_$index"
}

/** A folder's long-press menu, opened and shown like an [AppMenu]. */
typealias FolderMenu = LongPressMenu<RingItem.Folder>

/** What a folder's long-press menu offers, in menu order. */
enum class FolderOption(val label: String, val icon: ImageVector) {
    AddApps("Add apps", Icons.Default.Add),
    Rename("Rename", Icons.Default.Edit),
    Planet("Change planet", PlanetGlyph),
    Remove("Remove folder", Icons.Default.Close),
}

/** Material's folder glyph, which the core icon set leaves out. */
val FolderGlyph: ImageVector = materialIcon("Folder") {
    materialPath {
        moveTo(10f, 4f)
        horizontalLineTo(4f)
        curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
        lineTo(2f, 18f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(16f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(8f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        horizontalLineToRelative(-8f)
        lineToRelative(-2f, -2f)
        close()
    }
}

internal const val PREVIEWS = 4

/**
 * The share of the item's side each preview takes, and how far out from its middle they sit. Four fill an app's disc as
 * in Arc, their outer edges just inside its edge.
 */
private const val PREVIEW_FRACTION = 0.39f
private const val PREVIEW_REACH = 0.3f

/**
 * A slot on the ring or in the dock holding [folder]: a planet the size of an app, in the look the user chose, wearing a
 * badge for the [unread] notifications of all its apps. A tap opens the folder, unless it is empty, a long
 * press opens its [menu], and a long press that goes on becomes a [drag]. Given a [nameRoom], the name the user gave
 * it, if any, hangs under it as an app's does.
 */
@Composable
fun FolderIcon(
    folder: RingItem.Folder,
    icon: IconLoader,
    onOpen: (RingItem.Folder) -> Unit,
    modifier: Modifier = Modifier,
    menu: FolderMenu? = null,
    unread: Unread = Unread.None,
    drag: ItemDrag<RingItem.Folder>? = null,
    nameRoom: (() -> NameRoom?)? = null,
) {
    val count = folder.apps.size
    val presses = remember { MutableInteractionSource() }
    val name = folderDescription(folder.name, count)

    Box(
        modifier
            .combinedClickable(
                interactionSource = presses,
                indication = null,
                onLongClickLabel = menu?.let { "Folder options" },
                onLongClick = menu?.let { m -> { m.onOpen(folder) } },
                onClick = { if (count > 0) onOpen(folder) },
            )
            .itemDrag(folder, drag)
            .semantics { contentDescription = name.withUnread(unread.count) },
    ) {
        FolderFace(folder, icon, Modifier.fillMaxSize().graphicsLayer(), presses)
        menu?.content?.invoke(folder)
        UnreadBadge(unread)
        if (folder.name.isNotEmpty()) nameRoom?.let { HangingName(folder.name, it) }
    }
}

/**
 * Asks what to call a folder, starting from its [name]: Save hands what was typed to [onRename], which takes a blank
 * one as no name; Cancel, Back and a tap outside call [onDismiss].
 */
@Composable
fun FolderNameDialog(name: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    NameDialog(
        title = "Folder name",
        verb = "Save",
        maxLength = MAX_FOLDER_NAME,
        onName = onRename,
        onDismiss = onDismiss,
        initial = name,
        blankAllowed = true,
        modifier = Modifier.testTag(FolderTags.NAME_DIALOG),
        fieldTag = FolderTags.NAME,
    )
}

/** What a folder named [name] is called where a name must show: the name, or "Folder" for one with none. */
fun folderTitle(name: String): String = name.ifEmpty { "Folder" }

/** How a folder named [name] holding [count] apps is read out. */
fun folderDescription(name: String, count: Int): String =
    (if (name.isEmpty()) "Folder" else "$name folder") + ", " + counted(count, "app")

/**
 * What a folder look wears: a [disc] of that share of the item, [behind] drawn under it on the whole item, which it may
 * reach past, and [over] drawn on the disc over the previews. Each is drawn on a layer of its own, never round the
 * previews, so none of them can move or size them.
 */
class FolderWear(val disc: Float = 1f, val behind: Modifier = Modifier, val over: Modifier = Modifier)

/**
 * [folder] in the look of [LocalFolderStyle], filling the item: up to four previews of its apps, laid out the same in
 * every theme and look, on the disc and in what the theme's [ThemeArt.folderWear] gives it. [inner] fades the previews,
 * as when the folder takes the ring's centre and its apps are round it instead; [presses] ripple its disc. Give it a
 * layer of its own, so what turns with the sky redraws only the folder, not the page round it.
 */
@Composable
fun FolderFace(
    folder: RingItem.Folder,
    icon: IconLoader,
    modifier: Modifier = Modifier,
    presses: InteractionSource? = null,
    inner: () -> Float = { 1f },
) {
    val style = LocalFolderStyle.current
    // Every app circles the planet as a moon, rather than four sitting on it: the look is that arrangement.
    if (style.look == FolderLook.Orbit) return OrbitFace(folder, icon, style.minutes, inner, modifier)

    val wear = LocalThemeArt.current.folderWear(folder)
    Box(modifier) {
        Spacer(Modifier.matchParentSize().then(wear.behind))
        IconDisc(presses, Modifier.matchParentSize(), wear.disc) {
            FolderPreviews(folder.apps, icon, Modifier.graphicsLayer { alpha = inner() })
            Spacer(Modifier.fillMaxSize(wear.disc).then(wear.over))
        }
    }
}

/**
 * Up to four of a folder's [apps]' icons, one alone in the middle, more where the open folder's ring puts them: two side by
 * side, three as a triangle, four as a square. Sized by shares of the item rather than fixed gaps, so a shrunken ring's
 * small folders fit them too.
 */
@Composable
internal fun FolderPreviews(apps: List<AppEntry>, icon: IconLoader, modifier: Modifier = Modifier) {
    val previews = apps.take(PREVIEWS)
    Layout(
        content = { previews.forEachIndexed { index, app -> AppImage(app, icon, Modifier.testTag(FolderTags.preview(index))) } },
        modifier = modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val side = min(constraints.maxWidth, constraints.maxHeight)
        val size = (side * PREVIEW_FRACTION).roundToInt()
        val reach = if (previews.size > 1) side * PREVIEW_REACH else 0f
        layout(constraints.maxWidth, constraints.maxHeight) {
            measurables.forEachIndexed { index, measurable ->
                val (dx, dy) = folderSlotOffset(index, previews.size)
                measurable.measure(Constraints.fixed(size, size)).place(
                    ((constraints.maxWidth - size) / 2f + dx * reach).roundToInt(),
                    ((constraints.maxHeight - size) / 2f + dy * reach).roundToInt(),
                )
            }
        }
    }
}

/**
 * What a slot on the ring or in the dock shows: [item] as an app, launched by a tap and wearing its [unread] count, or as
 * a folder, opened by a tap and wearing its apps' sum. A long press opens the app's [menu] or the folder's [folderMenu],
 * and one that goes on becomes a [drag]. Given a [nameRoom], an app's name, or the name the user gave a folder, hangs
 * under it.
 */
@Composable
internal fun SlotIcon(
    item: RingItem,
    icon: IconLoader,
    onLaunch: (AppEntry) -> Unit,
    onOpenFolder: (RingItem.Folder) -> Unit,
    modifier: Modifier,
    menu: AppMenu?,
    folderMenu: FolderMenu?,
    unread: UnreadCounts,
    drag: ItemDrag<Any?>?,
    onClearBadge: ((AppEntry) -> Unit)? = null,
    nameRoom: (() -> NameRoom?)? = null,
) {
    when (item) {
        is RingItem.App -> AppIcon(item.app, icon, onLaunch, modifier, menu, unread.badge(item.app), drag, onClearBadge, nameRoom)
        is RingItem.Folder -> FolderIcon(item, icon, onOpenFolder, modifier, folderMenu, unread.badge(item.apps), drag, nameRoom)
    }
}

/** An item lit as the one an app let go now would fold into: a little larger, ringed in [colour]. */
internal fun Modifier.foldTarget(lit: Boolean, colour: Color): Modifier = if (!lit) {
    this
} else {
    this
        .semantics { stateDescription = "Drop to put in a folder" }
        .graphicsLayer {
            scaleX = FOLD_TARGET_SCALE
            scaleY = FOLD_TARGET_SCALE
        }
        .drawBehind { drawCircle(colour, radius = size.minDimension / 2 + 3.dp.toPx(), style = Stroke(2.dp.toPx())) }
}

private const val FOLD_TARGET_SCALE = 1.12f

/**
 * A folder's long-press menu of [options]. Choosing an item dismisses the menu, then hands on the choice.
 */
@Composable
fun FolderOptionsMenu(
    expanded: Boolean,
    onOption: (FolderOption) -> Unit,
    onDismiss: () -> Unit,
    options: List<FolderOption> = FolderOption.entries,
) {
    GroundMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        groups = listOf(options.map { option -> MenuRow(option.label, icon = glyph(option.icon)) { onOption(option) } }),
        modifier = Modifier.testTag(FolderTags.MENU),
    )
}
