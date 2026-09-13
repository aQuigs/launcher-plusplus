package com.aquigs.launcherplusplus.ui

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.LauncherPage

val clock = AppEntry("Clock", "com.example.clock", "com.example.clock.Main")
val mail = AppEntry("Mail", "com.example.mail", "com.example.mail.Main")

const val APPS_PER_LETTER = 3

/** Apps for every letter, enough rows to scroll on any phone, in the sorted order the repository delivers. */
val alphabet: List<AppEntry> = ('A'..'Z').flatMap { letter ->
    (1..APPS_PER_LETTER).map { n -> AppEntry("$letter$n", "com.example.${letter.lowercase()}$n", "Main") }
}

fun SemanticsNodeInteractionsProvider.pager() = onNodeWithTag(LauncherTags.PAGER)

fun SemanticsNodeInteractionsProvider.page(page: LauncherPage) = onNodeWithTag(LauncherTags.page(page))

fun SemanticsNodeInteractionsProvider.swipePager(swipe: TouchInjectionScope.() -> Unit) = pager().performTouchInput(swipe)

// The sheet merges the chevron into its own clickable drag-handle node, so the tag sits in the unmerged tree.
fun SemanticsNodeInteractionsProvider.drawerHandle() = onNodeWithTag(AppDrawerTags.HANDLE, useUnmergedTree = true)

fun SemanticsNodeInteractionsProvider.appList() = onNodeWithTag(AppDrawerTags.LIST)

fun SemanticsNodeInteractionsProvider.sectionHeader(initial: Char) = onNodeWithTag(AppDrawerTags.section(initial))

fun SemanticsNodeInteractionsProvider.railLetter(initial: Char) = onNodeWithTag(AppDrawerTags.letter(initial))

fun SemanticsNodeInteractionsProvider.pickHint() = onNodeWithTag(AppDrawerTags.PICK_HINT)

fun SemanticsNodeInteractionsProvider.emblem() = onNodeWithTag(HomeRingTags.EMBLEM)

fun SemanticsNodeInteractionsProvider.ringSlot(app: AppEntry) = onNodeWithTag(HomeRingTags.slot(app))
