package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOptionsTest {
    private val maps = app("Maps")

    @Test
    fun `the drawer offers app info and uninstall`() {
        assertEquals(listOf(AppOption.AppInfo, AppOption.Uninstall), appOptions(maps, place = null))
    }

    @Test
    fun `a place on the home screen first offers to take the app out of it`() {
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder, AppOption.AppInfo, AppOption.Uninstall),
            appOptions(maps, HomePlace.Ring),
        )
        for (place in listOf(HomePlace.Dock, HomePlace.Folder(2))) {
            assertEquals(listOf(AppOption.Remove(place), AppOption.AppInfo, AppOption.Uninstall), appOptions(maps, place))
        }
    }

    @Test
    fun `an app built into the system offers no uninstall`() {
        assertEquals(listOf(AppOption.AppInfo), appOptions(maps.copy(canUninstall = false), place = null))
    }
}
