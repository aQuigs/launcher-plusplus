package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestedHomeTest {
    private val phone = app("Phone")
    private val messages = app("Messages")
    private val chrome = app("Chrome")
    private val camera = app("Camera")
    private val gmail = app("Gmail")
    private val maps = app("Maps")
    private val photos = app("Photos")
    private val calendar = app("Calendar")
    private val store = app("Play Store")
    private val clock = app("Clock")
    private val spotify = app("Spotify")
    private val arc = app("Arc")
    private val settings = app("Settings")
    private val apps = listOf(phone, messages, chrome, camera, gmail, maps, photos, calendar, store, clock, spotify, arc, settings)

    private val defaults = DefaultApps(
        byRole = mapOf(
            AppRole.Phone to phone.key,
            AppRole.Messages to messages.key,
            AppRole.Browser to chrome.key,
            AppRole.Camera to camera.key,
            AppRole.Email to gmail.key,
            AppRole.Maps to maps.key,
            AppRole.Photos to photos.key,
            AppRole.Calendar to calendar.key,
            AppRole.Store to store.key,
            AppRole.Clock to clock.key,
        ),
        skips = setOf(arc.packageName, settings.packageName),
    )

    private fun setUp(
        apps: List<AppEntry>,
        defaults: DefaultApps = DefaultApps(),
        time: ForegroundTime? = null,
        page: CollectionsPage = CollectionsPage(),
    ) = suggestSetup(HomeApps(), page, apps, defaults, time)!!

    @Test
    fun `without usage the dock takes the calling and browsing defaults and the ring the everyday ones`() {
        val home = setUp(apps, defaults).home

        assertEquals(ringOf(phone, messages, chrome, camera), home.dock)
        assertEquals(ringOf(gmail, maps, photos, calendar, store, clock), home.ring)
    }

    @Test
    fun `the most used apps lead the ring, leaving out the dock's, skipped apps and repeats`() {
        val time = ForegroundTime(
            mapOf(arc.packageName to 900L, spotify.packageName to 500L, chrome.packageName to 400L, maps.packageName to 300L, settings.packageName to 200L),
        )

        val home = setUp(apps, defaults, time).home

        assertEquals(ringOf(spotify, maps, gmail, photos, calendar, store), home.ring)
    }

    @Test
    fun `a role with no installed app is left out, and one app filling two roles comes once`() {
        val sparse = DefaultApps(
            byRole = mapOf(AppRole.Phone to phone.key, AppRole.Messages to phone.key, AppRole.Camera to "gone.camera"),
        )

        val home = setUp(apps, sparse).home

        assertEquals(ringOf(phone), home.dock)
        assertEquals(Ring(), home.ring)
    }

    @Test
    fun `the fullest categories of two apps or more get cards, the fullest first`() {
        val games = (1..4).map { AppEntry("Game $it", "pkg.game$it", "Main", category = AppCategory.Games) }
        val music = (1..3).map { AppEntry("Music $it", "pkg.music$it", "Main", category = AppCategory.Music) }
        val social = (1..2).map { AppEntry("Social $it", "pkg.social$it", "Main", category = AppCategory.Social) }
        val shopping = (1..3).map { AppEntry("Shop $it", "pkg.shop$it", "Main", category = AppCategory.Shopping) }
        val tool = AppEntry("Tool", "pkg.tool", "Main", category = AppCategory.Tools)
        val launcher = AppEntry("Launcher", "pkg.launcher", "Main", category = AppCategory.Music)

        val page = setUp(games + music + social + shopping + tool + launcher, DefaultApps(skips = setOf(launcher.packageName))).collections

        assertEquals(
            listOf(
                CollectionKind.NewApps,
                CollectionKind.MostUsed,
                CollectionKind.Category(AppCategory.Games),
                CollectionKind.Category(AppCategory.Music),
                CollectionKind.Category(AppCategory.Shopping),
                CollectionKind.Category(AppCategory.Social),
            ),
            page.cards.map { it.kind },
        )
        assertEquals(Favourites(music.map { it.key }), page.card(CollectionKind.Category(AppCategory.Music))!!.apps)
    }

    @Test
    fun `a page with a hand-picked card is left alone`() {
        val page = CollectionsPage().add(CollectionKind.Custom("Work"))
        val games = (1..3).map { AppEntry("Game $it", "pkg.game$it", "Main", category = AppCategory.Games) }

        assertEquals(page, setUp(games, page = page).collections)
    }

    @Test
    fun `a home screen holding anything is the user's and gets no guess`() {
        assertNull(suggestSetup(HomeApps(dock = ringOf(phone)), CollectionsPage(), apps, defaults, time = null))
    }
}
