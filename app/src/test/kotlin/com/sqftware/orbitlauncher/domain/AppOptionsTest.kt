package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOptionsTest {
    private val maps = app("Maps")
    private val onCards = AppOption.BuiltInCards(isOff = false)
    private val addTo = listOf(AppOption.AddTo(HomePlace.Ring), AppOption.AddTo(HomePlace.Dock))

    @Test
    fun `the drawer offers to add the app to the ring and the dock, then its settings, app info and uninstall`() {
        assertEquals(addTo + listOf(onCards, AppOption.AppInfo, AppOption.Uninstall), appOptions(maps, place = null))
    }

    @Test
    fun `the drawer offers no place the app already has a slot of its own in`() {
        val home = HomeApps(dock = ringOf(maps))
        assertEquals(AppOption.AddTo(HomePlace.Ring), appOptions(maps, place = null, home).first())
        assertEquals(onCards, appOptions(maps, place = null, home)[1])
    }

    @Test
    fun `a place on the home screen first offers to take the app out of it, and the ring and the dock a new folder`() {
        for (place in listOf(HomePlace.Ring, HomePlace.Dock)) {
            assertEquals(
                listOf(AppOption.Remove(place), AppOption.NewFolder, onCards, AppOption.AppInfo, AppOption.Uninstall),
                appOptions(maps, place),
            )
        }
        val folder = HomePlace.Folder(HomePlace.Dock, 2)
        assertEquals(listOf(AppOption.Remove(folder), onCards, AppOption.AppInfo, AppOption.Uninstall), appOptions(maps, folder))
    }

    @Test
    fun `the settings offer the way back once set, and the badge only while the badges are enabled`() {
        val settings = AppSettings().toggleBadge(maps).toggleBuiltInCards(maps)
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder, AppOption.BuiltInCards(isOff = true)),
            appOptions(maps, HomePlace.Ring, settings = settings).take(3),
        )
        assertEquals(
            listOf(AppOption.Badge(isOff = true), AppOption.BuiltInCards(isOff = true)),
            appOptions(maps, HomePlace.Ring, settings = settings, badgesEnabled = true).drop(2).take(2),
        )
    }

    @Test
    fun `a pinned shortcut offers neither setting`() {
        val shortcut = maps.copy(shortcutId = "home", canUninstall = false)
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder, AppOption.AppInfo),
            appOptions(shortcut, HomePlace.Ring, badgesEnabled = true),
        )
    }

    @Test
    fun `an app from the Play Store offers its page there, and one built into the system no uninstall`() {
        assertEquals(
            listOf(onCards, AppOption.PlayStore, AppOption.AppInfo),
            appOptions(maps.copy(fromPlayStore = true, canUninstall = false), HomePlace.Folder(HomePlace.Ring, 0)).drop(1),
        )
    }
}
