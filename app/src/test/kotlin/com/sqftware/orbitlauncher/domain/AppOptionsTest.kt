package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOptionsTest {
    private val maps = app("Maps")
    private val split = AppOption.SplitWith
    private val onCards = AppOption.BuiltInCards(isOff = false)

    private fun options(
        app: AppEntry = maps,
        spot: AppSpot = AppSpot.Drawer,
        home: HomeApps = HomeApps(),
        settings: AppSettings = AppSettings(),
        badgesEnabled: Boolean = false,
        hasStorePage: Boolean = false,
    ) = appOptions(app, spot, home, settings, badgesEnabled, hasStorePage)

    @Test
    fun `the drawer adds the app to each home place it has no slot of its own in`() {
        assertEquals(
            listOf(AppOption.AddTo(HomePlace.Ring), AppOption.AddTo(HomePlace.Dock), split, onCards, AppOption.AppInfo, AppOption.Uninstall),
            options(),
        )
        assertEquals(
            listOf(AppOption.AddTo(HomePlace.Ring), split, onCards, AppOption.AppInfo, AppOption.Uninstall),
            options(home = HomeApps(dock = ringOf(maps))),
        )
    }

    @Test
    fun `a card offers what the drawer does, and a hand-picked one first takes the app off it`() {
        assertEquals(options(), options(spot = AppSpot.Card(CollectionKind.NewApps)))
        val tools = CollectionKind.Category(AppCategory.Tools)
        assertEquals(listOf(AppOption.RemoveFromCard(tools)) + options(), options(spot = AppSpot.Card(tools)))
    }

    @Test
    fun `a place on the home screen first offers to take the app out of it, and the ring and the dock a new folder`() {
        for (place in listOf(HomePlace.Ring, HomePlace.Dock)) {
            assertEquals(
                listOf(AppOption.Remove(place), AppOption.NewFolder, split, onCards, AppOption.AppInfo, AppOption.Uninstall),
                options(spot = AppSpot.Home(place)),
            )
        }
        val folder = HomePlace.Folder(HomePlace.Dock, 2)
        assertEquals(listOf(AppOption.Remove(folder), split, onCards, AppOption.AppInfo, AppOption.Uninstall), options(spot = AppSpot.Home(folder)))
    }

    @Test
    fun `the settings offer the way back once set, the badge only while the badges are enabled`() {
        val settings = AppSettings().toggleBadge(maps).toggleBuiltInCards(maps)
        val offCards = AppOption.BuiltInCards(isOff = true)
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder, split, offCards, AppOption.AppInfo, AppOption.Uninstall),
            options(spot = AppSpot.Home(HomePlace.Ring), settings = settings),
        )
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder, split, AppOption.Badge(isOff = true), offCards, AppOption.AppInfo, AppOption.Uninstall),
            options(spot = AppSpot.Home(HomePlace.Ring), settings = settings, badgesEnabled = true),
        )
    }

    @Test
    fun `a pinned shortcut offers neither setting nor a split, but its app's store page`() {
        val shortcut = maps.copy(kind = EntryKind.Shortcut("home"), canUninstall = false)
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder, AppOption.PlayStore, AppOption.AppInfo),
            options(shortcut, AppSpot.Home(HomePlace.Ring), badgesEnabled = true, hasStorePage = true),
        )
    }

    @Test
    fun `an app built into the system offers no uninstall`() {
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder, split, onCards, AppOption.AppInfo),
            options(maps.copy(canUninstall = false), AppSpot.Home(HomePlace.Ring)),
        )
    }

    @Test
    fun `a pair offers only what takes it off where it is, or puts it on the ring or the dock`() {
        val pair = pairOf(maps, app("Music"))!!
        assertEquals(
            listOf(AppOption.Remove(HomePlace.Ring), AppOption.NewFolder),
            options(pair, AppSpot.Home(HomePlace.Ring), badgesEnabled = true, hasStorePage = true),
        )
        val tools = CollectionKind.Category(AppCategory.Tools)
        assertEquals(
            listOf(AppOption.RemoveFromCard(tools), AppOption.AddTo(HomePlace.Ring), AppOption.AddTo(HomePlace.Dock)),
            options(pair, AppSpot.Card(tools), badgesEnabled = true, hasStorePage = true),
        )
    }
}
