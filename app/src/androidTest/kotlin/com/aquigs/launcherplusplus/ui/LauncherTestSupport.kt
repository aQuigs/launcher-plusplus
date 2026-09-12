package com.aquigs.launcherplusplus.ui

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.LauncherPage

val clock = AppEntry("Clock", "com.example.clock", "com.example.clock.Main")
val mail = AppEntry("Mail", "com.example.mail", "com.example.mail.Main")

/** Three apps per letter, enough rows to scroll on any phone, in the sorted order the repository delivers. */
val alphabet: List<AppEntry> = ('A'..'Z').flatMap { letter ->
    (1..3).map { n -> AppEntry("$letter$n", "com.example.${letter.lowercase()}$n", "Main") }
}

fun SemanticsNodeInteractionsProvider.pager() = onNodeWithTag(LauncherTags.PAGER)

fun SemanticsNodeInteractionsProvider.page(page: LauncherPage) = onNodeWithTag(LauncherTags.page(page))

fun SemanticsNodeInteractionsProvider.swipePager(swipe: TouchInjectionScope.() -> Unit) = pager().performTouchInput(swipe)

fun SemanticsNodeInteractionsProvider.drawerHandle() = onNodeWithTag(AppDrawerTags.HANDLE)

fun SemanticsNodeInteractionsProvider.appList() = onNodeWithTag(AppDrawerTags.LIST)

fun SemanticsNodeInteractionsProvider.sectionHeader(initial: Char) = onNodeWithTag(AppDrawerTags.section(initial))

fun SemanticsNodeInteractionsProvider.railLetter(initial: Char) = onNodeWithTag(AppDrawerTags.letter(initial))

/** Passes whether the text is off screen or, as with a lazy row that was never composed, absent altogether. */
fun SemanticsNodeInteractionsProvider.assertNotShown(text: String) {
    val matches = onAllNodesWithText(text)
    repeat(matches.fetchSemanticsNodes(atLeastOneRootRequired = false).size) { matches[it].assertIsNotDisplayed() }
}
