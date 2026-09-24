package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PageLayoutTest {
    @Test
    fun `default layout puts home in the middle`() {
        val layout = PageLayout()

        assertEquals(3, layout.pages.size)
        assertEquals(layout.pages.size / 2, layout.homeIndex)
    }

    @Test
    fun `home index follows the page order`() {
        val layout = PageLayout(listOf(LauncherPage.Home, LauncherPage.Collections))

        assertEquals(0, layout.homeIndex)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a layout without home is rejected`() {
        PageLayout(listOf(LauncherPage.Widgets, LauncherPage.Collections))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a repeated page is rejected`() {
        PageLayout(listOf(LauncherPage.Home, LauncherPage.Home))
    }
}
