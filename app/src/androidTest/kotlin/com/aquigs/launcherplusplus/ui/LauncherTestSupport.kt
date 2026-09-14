package com.aquigs.launcherplusplus.ui

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.aquigs.launcherplusplus.domain.AppCategory
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.CollectionKind
import com.aquigs.launcherplusplus.domain.HomePlace
import com.aquigs.launcherplusplus.domain.HostedWidget
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.Ring
import com.aquigs.launcherplusplus.domain.RingItem
import com.aquigs.launcherplusplus.domain.RingSlot

val clock = AppEntry("Clock", "com.example.clock", "com.example.clock.Main")
val mail = AppEntry("Mail", "com.example.mail", "com.example.mail.Main")

val tools = CollectionKind.Category(AppCategory.Tools)

const val APPS_PER_LETTER = 3

/** Apps for every letter, enough rows to scroll on any phone, in the sorted order the repository delivers. */
val alphabet: List<AppEntry> = ('A'..'Z').flatMap { letter ->
    (1..APPS_PER_LETTER).map { n -> AppEntry("$letter$n", "com.example.${letter.lowercase()}$n", "Main") }
}

fun ringOf(vararg apps: AppEntry) = Ring(apps.map { RingSlot.App(it.key) })

fun folderOf(vararg apps: AppEntry) = RingSlot.Folder(apps.map { it.key })

fun List<AppEntry>.asRingItems() = map(RingItem::App)

fun SemanticsNodeInteractionsProvider.pager() = onNodeWithTag(LauncherTags.PAGER)

fun SemanticsNodeInteractionsProvider.page(page: LauncherPage) = onNodeWithTag(LauncherTags.page(page))

fun SemanticsNodeInteractionsProvider.swipePager(swipe: TouchInjectionScope.() -> Unit) = pager().performTouchInput(swipe)

// The sheet merges the chevron into its own clickable drag-handle node, so the tag sits in the unmerged tree.
fun SemanticsNodeInteractionsProvider.drawerHandle() = onNodeWithTag(AppDrawerTags.HANDLE, useUnmergedTree = true)

fun SemanticsNodeInteractionsProvider.appList() = onNodeWithTag(AppDrawerTags.LIST)

fun SemanticsNodeInteractionsProvider.searchField() = onNodeWithTag(AppDrawerTags.SEARCH)

fun SemanticsNodeInteractionsProvider.sectionHeader(initial: Char) = onNodeWithTag(AppDrawerTags.section(initial))

fun SemanticsNodeInteractionsProvider.railLetter(initial: Char) = onNodeWithTag(AppDrawerTags.letter(initial))

fun SemanticsNodeInteractionsProvider.placePicker() = onNodeWithTag(PlacePickerTags.PICKER)

fun SemanticsNodeInteractionsProvider.placeOption(place: HomePlace) = onNodeWithTag(PlacePickerTags.place(place))

fun SemanticsNodeInteractionsProvider.clockTime() = onNodeWithTag(HomeClockTags.TIME)

fun SemanticsNodeInteractionsProvider.clockDate() = onNodeWithTag(HomeClockTags.DATE)

fun SemanticsNodeInteractionsProvider.emblem() = onNodeWithTag(HomeRingTags.EMBLEM)

fun SemanticsNodeInteractionsProvider.ringSlot(app: AppEntry) = onNodeWithTag(HomeRingTags.slot(app))

fun SemanticsNodeInteractionsProvider.folderSlot(index: Int) = onNodeWithTag(HomeRingTags.folder(index))

fun SemanticsNodeInteractionsProvider.closeFolder() = onNodeWithContentDescription("Close folder")

fun SemanticsNodeInteractionsProvider.folderOptionsMenu() = onNodeWithTag(FolderTags.MENU)

fun SemanticsNodeInteractionsProvider.dock() = onNodeWithTag(DockTags.DOCK)

fun SemanticsNodeInteractionsProvider.dockSlot(app: AppEntry) = onNodeWithTag(DockTags.slot(app))

fun SemanticsNodeInteractionsProvider.appOptionsMenu() = onNodeWithTag(AppOptionsTags.MENU)

fun SemanticsNodeInteractionsProvider.dragGhost() = onNodeWithTag(DragTags.GHOST)

fun SemanticsNodeInteractionsProvider.homeAppCard() = onNodeWithTag(HomeAppCardTags.CARD)

fun SemanticsNodeInteractionsProvider.becomeHomeAppButton() = onNodeWithTag(HomeAppCardTags.BUTTON)

fun SemanticsNodeInteractionsProvider.widget(widget: HostedWidget) = onNodeWithTag(WidgetTags.widget(widget.id))

fun SemanticsNodeInteractionsProvider.addWidgetButton() = onNodeWithTag(WidgetTags.ADD)

fun SemanticsNodeInteractionsProvider.widgetOptionsMenu() = onNodeWithTag(WidgetTags.MENU)

fun SemanticsNodeInteractionsProvider.launcherMenu() = onNodeWithTag(LauncherMenuTags.MENU)

// The badge is merged into its icon's node, so it is found in the unmerged tree, under the icon tagged [tag].
fun SemanticsNodeInteractionsProvider.badgeOn(tag: String) =
    onNodeWithTag(tag, useUnmergedTree = true).onChildren().filterToOne(hasTestTag(BadgeTags.BUBBLE))

fun SemanticsNodeInteractionsProvider.collectionCard(kind: CollectionKind) = onNodeWithTag(CollectionTags.card(kind))

fun SemanticsNodeInteractionsProvider.collectionApp(kind: CollectionKind, app: AppEntry) = onNodeWithTag(CollectionTags.app(kind, app))

fun SemanticsNodeInteractionsProvider.collectionChevron(kind: CollectionKind) = onNodeWithTag(CollectionTags.chevron(kind))

fun SemanticsNodeInteractionsProvider.collectionEditButton(kind: CollectionKind) = onNodeWithTag(CollectionTags.edit(kind))

fun SemanticsNodeInteractionsProvider.collectionHandle(kind: CollectionKind) = onNodeWithTag(CollectionTags.handle(kind))

fun SemanticsNodeInteractionsProvider.collectionTile(kind: CollectionKind) = onNodeWithTag(CollectionTags.tile(kind))

fun SemanticsNodeInteractionsProvider.addCollectionButton() = onNodeWithTag(CollectionTags.ADD)

fun SemanticsNodeInteractionsProvider.collectionBin() = onNodeWithTag(CollectionTags.BIN)

fun SemanticsNodeInteractionsProvider.collectionPicker() = onNodeWithTag(CollectionTags.PICKER)

fun SemanticsNodeInteractionsProvider.collectionEditor() = onNodeWithTag(CollectionTags.EDITOR)

// The drawer's list is composed under the editor too, so a row is told apart by the screen it is on.
fun SemanticsNodeInteractionsProvider.editorRow(label: String) =
    onNode(hasText(label) and hasAnyAncestor(hasTestTag(CollectionTags.EDITOR)))
