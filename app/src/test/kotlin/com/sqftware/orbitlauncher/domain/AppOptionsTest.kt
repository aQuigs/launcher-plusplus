package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOptionsTest {
    private val maps = app("Maps")

    @Test
    fun `the drawer offers app info and uninstall`() {
        assertEquals(listOf(AppOption.AppInfo, AppOption.Uninstall), appOptions(maps, place = null))
    }

    @Test
    fun `a place on the home screen first offers to take the app out of it, and the ring and the dock a new folder`() {
        for (place in listOf(HomePlace.Ring, HomePlace.Dock)) {
            assertEquals(
                listOf(AppOption.Remove(place), AppOption.NewFolder, AppOption.AppInfo, AppOption.Uninstall),
                appOptions(maps, place),
            )
        }
        val folder = HomePlace.Folder(HomePlace.Dock, 2)
        assertEquals(listOf(AppOption.Remove(folder), AppOption.AppInfo, AppOption.Uninstall), appOptions(maps, folder))
    }

    @Test
    fun `an app built into the system offers no uninstall`() {
        assertEquals(listOf(AppOption.AppInfo), appOptions(maps.copy(canUninstall = false), place = null))
    }
}
