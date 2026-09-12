package com.aquigs.launcherplusplus.ui

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.LauncherPage

val clock = AppEntry("Clock", "com.example.clock", "com.example.clock.Main")
val mail = AppEntry("Mail", "com.example.mail", "com.example.mail.Main")

fun SemanticsNodeInteractionsProvider.pager() = onNodeWithTag(LauncherTags.PAGER)

fun SemanticsNodeInteractionsProvider.page(page: LauncherPage) = onNodeWithTag(LauncherTags.page(page))

fun SemanticsNodeInteractionsProvider.swipePager(swipe: TouchInjectionScope.() -> Unit) = pager().performTouchInput(swipe)
