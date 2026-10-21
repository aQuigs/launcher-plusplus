package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.unit.dp

/** The launcher's spaces, so a page, a panel and the drawer keep their content the same way from the edges and apart. */
object Spacing {
    /** From a screen's sides to what is on it: a page's cards, a panel's title, the drawer's rows. */
    val Edge = 16.dp

    /** Between boxes side by side or one above another, such as cards and tiles. */
    val Gap = 12.dp

    /** Between a title and what it heads, and between the lines of one group. */
    val Tight = 8.dp

    /** Above a panel's title, under the status bar. */
    val Top = 24.dp
}
