package com.aquigs.launcherplusplus.ui

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.HomePlace
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.Ring
import com.aquigs.launcherplusplus.domain.RingItem
import com.aquigs.launcherplusplus.domain.RingSlot

val clock = AppEntry("Clock", "com.example.clock", "com.example.clock.Main")
val mail = AppEntry("Mail", "com.example.mail", "com.example.mail.Main")

const val APPS_PER_LETTER = 3

/** Apps for every letter, enough rows to scroll on any phone, in the sorted order the repository delivers. */
val alphabet: List<AppEntry> = ('A'..'Z').flatMap { letter ->
    (1..APPS_PER_LETTER).map { n -> AppEntry("$letter$n", "com.example.${letter.lowercase()}$n", "Main") }
}

fun ringOf(vararg apps: AppEntry) = Ring(apps.map { RingSlot.App(it.key) })

fun folderOf(name: String, vararg apps: AppEntry) = RingSlot.Folder(name, apps.map { it.key })

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

fun SemanticsNodeInteractionsProvider.folderPopup() = onNodeWithTag(FolderTags.POPUP)

// By text within the popup: the drawer, always composed, lists the same app under the same label.
fun SemanticsNodeInteractionsProvider.folderApp(app: AppEntry) = onNode(hasText(app.label) and hasAnyAncestor(hasTestTag(FolderTags.POPUP)))

fun SemanticsNodeInteractionsProvider.folderOptionsMenu() = onNodeWithTag(FolderTags.MENU)

fun SemanticsNodeInteractionsProvider.renameDialog() = onNodeWithTag(FolderTags.RENAME)

fun SemanticsNodeInteractionsProvider.folderNameField() = onNodeWithTag(FolderTags.NAME)

fun SemanticsNodeInteractionsProvider.dock() = onNodeWithTag(DockTags.DOCK)

fun SemanticsNodeInteractionsProvider.dockSlot(app: AppEntry) = onNodeWithTag(DockTags.slot(app))

fun SemanticsNodeInteractionsProvider.appOptionsMenu() = onNodeWithTag(AppOptionsTags.MENU)

fun SemanticsNodeInteractionsProvider.homeAppCard() = onNodeWithTag(HomeAppCardTags.CARD)

fun SemanticsNodeInteractionsProvider.becomeHomeAppButton() = onNodeWithTag(HomeAppCardTags.BUTTON)
