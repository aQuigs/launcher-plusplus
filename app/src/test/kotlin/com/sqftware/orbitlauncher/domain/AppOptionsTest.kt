package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOptionsTest {
    private val maps = app("Maps")
    private val onCards = AppOption.BuiltInCards(isOff = false)

    private fun options(
        app: AppEntry = maps,
        place: HomePlace? = null,
        card: CollectionKind? = null,
        home: HomeApps = HomeApps(),
        settings: AppSettings = AppSettings(),
        badgesEnabled: Boolean = false,
        hasStorePage: Boolean = false,
    ) = appOptions(app, place, card, home, settings, badgesEnabled, hasStorePage)

    @Test
    fun `the drawer adds the app to each home place it has no slot of its own in`() {
        assertEquals(
            listOf(AppOption.AddTo(HomePlace.Ring), AppOption.AddTo(HomePlace.Dock), onCards, AppOption.AppInfo, AppOption.Uninstall),
            options(),
        )
        assertEquals(
            listOf(AppOption.AddTo(HomePlace.Ring), onCards, AppOption.AppInfo, AppOption.Uninstall),
            options(home = HomeApps(dock = ringOf(maps))),
        )
    }

    @Test
    fun `a card offers what the drawer does, and a hand-picked one first takes the app off it`() {
        assertEquals(options(), options(card = CollectionKind.NewApps))
        val tools = CollectionKind.Category(AppCategory.Tools)
        assertEquals(listOf(AppOption.RemoveFromCard(tools)) + options(), options(card = tools))
    }

    @Test
    fun `a place on the home screen first offers to take the app out of it, and the ring and the dock a new folder`() {
        for (place in listOf(HomePlace.Ring, HomePlace.Dock)) {
            assertEquals(
                listOf(AppOption.Remove(place), AppOption.NewFolder, onCards, AppOption.AppInfo, AppOption.Uninstall),
                options(place = place),
            )
        }
        val folder = HomePlace.Folder(HomePlace.Dock, 2)
        assertEquals(listOf(AppOption.Remove(folder), onCards, AppOption.AppInfo, AppOption.Uninstall), options(place = folder))
    }

    @Test
    fun `the settings offer the way back once set, the badge only while the badges are enabled`() {
        val settings = AppSettings().toggleBadge(maps).toggleBuiltInCards(maps)
        val offCards = AppOption.BuiltInCards(isOff = true)
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder, offCards, AppOption.AppInfo, AppOption.Uninstall),
            options(place = HomePlace.Ring, settings = settings),
        )
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder, AppOption.Badge(isOff = true), offCards, AppOption.AppInfo, AppOption.Uninstall),
            options(place = HomePlace.Ring, settings = settings, badgesEnabled = true),
        )
    }

    @Test
    fun `a pinned shortcut offers neither setting, but its app's store page`() {
        val shortcut = maps.copy(shortcutId = "home", canUninstall = false)
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder, AppOption.PlayStore, AppOption.AppInfo),
            options(shortcut, HomePlace.Ring, badgesEnabled = true, hasStorePage = true),
        )
    }

    @Test
    fun `an app built into the system offers no uninstall`() {
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder, onCards, AppOption.AppInfo),
            options(maps.copy(canUninstall = false), HomePlace.Ring),
        )
    }
}
