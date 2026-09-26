package com.sqftware.orbitlauncher.domain

import com.sqftware.orbitlauncher.domain.CollectionKind.MostUsed
import com.sqftware.orbitlauncher.domain.CollectionKind.NewApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionsTest {
    private val clock = app("Clock", "com.google.android.deskclock")
    private val mail = app("Gmail", "com.google.android.gm")
    private val maps = app("Maps", "com.google.android.apps.maps")
    private val chrome = app("Chrome", "com.android.chrome")
    private val tools = CollectionKind.Category(AppCategory.Tools)
    private val photos = CollectionKind.Category(AppCategory.Photos)

    @Test
    fun `a fresh page holds the two built-in cards, compact`() {
        assertEquals(listOf(CollectionCard(NewApps), CollectionCard(MostUsed)), CollectionsPage().cards)
        assertTrue(NewApps in CollectionsPage())
        assertFalse(tools in CollectionsPage())
    }

    @Test
    fun `a card joins at the bottom once and leaves with its apps`() {
        val page = CollectionsPage().add(tools, Favourites(listOf(clock.key)))

        assertEquals(listOf(NewApps, MostUsed, tools), page.cards.map { it.kind })
        assertEquals(page, page.add(tools))
        assertEquals(Favourites(listOf(clock.key)), page.card(tools)!!.apps)
        assertEquals(CollectionsPage(), page.remove(tools))
        assertEquals(Favourites(), page.remove(tools).add(tools).card(tools)!!.apps)
        assertEquals(page, page.remove(photos))
    }

    @Test
    fun `an app is added to a card once and taken off again`() {
        val page = CollectionsPage().add(tools)
        val one = page.addApp(tools, clock)

        assertEquals(Favourites(listOf(clock.key)), one.card(tools)!!.apps)
        assertEquals(one, one.addApp(tools, clock))
        assertEquals(page, one.removeApp(tools, clock))
        assertEquals(one, one.removeApp(tools, mail))
        assertEquals(page, page.addApp(photos, clock))
    }

    @Test
    fun `expanded flips one card`() {
        val page = CollectionsPage().toggleExpanded(MostUsed)

        assertFalse(page.card(NewApps)!!.expanded)
        assertTrue(page.card(MostUsed)!!.expanded)
        assertEquals(CollectionsPage(), page.toggleExpanded(MostUsed))
    }

    @Test
    fun `a card moves to another position and stays put for one off the page`() {
        val page = CollectionsPage().add(tools)

        assertEquals(listOf(tools, NewApps, MostUsed), page.move(2, 0).cards.map { it.kind })
        assertEquals(listOf(MostUsed, NewApps, tools), page.move(0, 1).cards.map { it.kind })
        assertEquals(page, page.move(1, 1))
        assertEquals(page, page.move(3, 0))
        assertEquals(page, page.move(0, -1))
    }

    @Test
    fun `a well-known app's category wins, else the package's own, else a telling word in its name, else nothing`() {
        assertEquals(AppCategory.Business, AppEntry("LinkedIn", "com.linkedin.android", "Main", category = AppCategory.Social).suggestedCategory)
        assertEquals(AppCategory.Games, AppEntry("Chess", "com.example.music", "Main", category = AppCategory.Games).suggestedCategory)

        val byPackage = mapOf(
            "com.google.android.deskclock" to AppCategory.Tools,
            "com.google.android.calculator" to AppCategory.Tools,
            "com.android.settings" to AppCategory.Tools,
            "com.google.android.apps.nbu.files" to AppCategory.Tools,
            "com.android.chrome" to AppCategory.Tools,
            "com.google.android.GoogleCamera" to AppCategory.Photos,
            "com.google.android.apps.photos" to AppCategory.Photos,
            "com.example.gallery" to AppCategory.Photos,
            "com.google.android.apps.messaging" to AppCategory.Communication,
            "com.example.messages" to AppCategory.Communication,
            "com.google.android.dialer" to AppCategory.Communication,
            "com.google.android.contacts" to AppCategory.Communication,
            "com.google.android.gm" to AppCategory.Communication,
            "com.google.android.apps.meetings" to AppCategory.Communication,
            "com.samsung.android.email.provider" to AppCategory.Communication,
            "com.whatsapp" to AppCategory.Communication,
            "org.thoughtcrime.securesms" to AppCategory.Communication,
            "com.Slack" to AppCategory.Business,
            "us.zoom.videomeetings" to AppCategory.Business,
            "com.google.android.apps.maps" to AppCategory.Transport,
            "com.google.android.calendar" to AppCategory.Productivity,
            "com.google.android.apps.docs" to AppCategory.Productivity,
            "com.example.drive" to AppCategory.Productivity,
            "com.google.android.keep" to AppCategory.Productivity,
            "com.google.android.apps.docs.editors.sheets" to AppCategory.Productivity,
            "com.google.android.apps.docs.editors.slides" to AppCategory.Productivity,
            "com.google.android.youtube" to AppCategory.Video,
            "com.google.android.apps.youtube.music" to AppCategory.Music,
        )
        byPackage.forEach { (packageName, category) -> assertEquals(packageName, category, app("X", packageName).suggestedCategory) }
        assertNull(app("Arc Launcher", "apptech.arc").suggestedCategory)
        assertNull(app("Sigma", "com.sigma.app").suggestedCategory)
    }

    @Test
    fun `a category is seeded with the apps that belong in it, in order`() {
        val camera = app("Camera", "com.google.android.GoogleCamera")

        assertEquals(Favourites(listOf(clock.key, chrome.key)), seedCategory(AppCategory.Tools, listOf(clock, camera, mail, chrome)))
        assertEquals(Favourites(listOf(camera.key)), seedCategory(AppCategory.Photos, listOf(clock, camera, mail, chrome)))
        assertEquals(Favourites(), seedCategory(AppCategory.Games, listOf(clock, mail)))
    }

    @Test
    fun `new apps are the newest first, one per package, ten at most`() {
        val apps = (1..12).map { n -> AppEntry("A$n", "pkg.a$n", "Main", installedAt = n.toLong()) }
        val twin = AppEntry("Twin", "pkg.a12", "Other", installedAt = 12L)

        assertEquals(apps.reversed().take(10), newApps(apps + twin))
        assertEquals(apps.reversed().take(3), newApps(apps, limit = 3))
        assertEquals(emptyList<AppEntry>(), newApps(emptyList()))
    }

    @Test
    fun `most used are those in front the longest, one per package, never one that never was`() {
        val time = ForegroundTime(mapOf(mail.packageName to 5_000L, clock.packageName to 100L, maps.packageName to 0L))
        val twin = AppEntry("Gmail too", mail.packageName, "Other")

        assertEquals(listOf(mail, clock), mostUsed(listOf(clock, maps, mail), time))
        assertEquals(listOf(mail), mostUsed(listOf(clock, maps, mail), time, limit = 1))
        assertEquals(listOf(mail), mostUsed(listOf(mail, twin), time))
        assertEquals(emptyList<AppEntry>(), mostUsed(listOf(clock), ForegroundTime()))
    }

    @Test
    fun `a page comes back as it went`() {
        val page = CollectionsPage().toggleExpanded(NewApps).add(tools, Favourites(listOf(clock.key, mail.key))).add(photos)

        assertEquals(
            "NewApps\t1\nMostUsed\t0\nTools\t0\t${clock.key}\t${mail.key}\nPhotos\t0",
            page.encode(),
        )
        assertEquals(page, decodeCollectionsPage(page.encode()))
    }

    @Test
    fun `a custom name keeps one space for any run of whitespace, loses invisible characters, and stops at 30`() {
        val page = CollectionsPage()

        assertEquals(CollectionKind.Custom("Utilities"), page.custom("  Utilities "))
        assertEquals(CollectionKind.Custom("Bills and tax"), page.custom("Bills\tand\r\n  tax"))
        assertEquals(CollectionKind.Custom("Finance"), page.custom("Fin​ance\u0000"))
        assertEquals(CollectionKind.Custom("a".repeat(30)), page.custom("a".repeat(40)))
        assertEquals(CollectionKind.Custom("a".repeat(29)), page.custom("a".repeat(29) + " b"))
    }

    @Test
    fun `a custom name must be new, whatever its case or spacing, and not blank`() {
        val page = CollectionsPage().add(CollectionKind.Custom("Finance"))

        listOf("", " \t\n​", "finance", "FIN ANCE", "TOOLS", "lifestyle", "new apps", "NewApps", "Most Used", "create your own")
            .forEach { assertNull(it, page.custom(it)) }
    }

    @Test
    fun `a custom card keeps its name and apps through the text`() {
        val finance = CollectionKind.Custom("Money: in & out")
        val page = CollectionsPage().add(tools).add(finance, Favourites(listOf(mail.key)))

        assertEquals("NewApps\t0\nMostUsed\t0\nTools\t0\nCustom:Money: in & out\t0\t${mail.key}", page.encode())
        assertEquals(page, decodeCollectionsPage(page.encode()))
    }

    @Test
    fun `a stored custom name is cleaned, and skipped if that leaves it blank or a built-in's`() {
        val text = "Custom:\t0\nCustom: \t0\nCustom:NewApps\t0\nCustom:Tools\t0\nCustom: Bills​ \t1\ta/A"

        assertEquals(
            listOf(CollectionCard(CollectionKind.Custom("Bills"), Favourites(listOf("a/A")), expanded = true)),
            decodeCollectionsPage(text).cards,
        )
    }

    @Test
    fun `a custom card taken off in the picker keeps its tile, its place and its apps, and a new one joins the end`() {
        val bills = CollectionCard(CollectionKind.Custom("Bills"), Favourites(listOf(mail.key)))
        val trips = CollectionCard(CollectionKind.Custom("Trips"))
        val page = CollectionsPage(listOf(CollectionCard(NewApps), bills, trips))
        val listed = page.customTiles(emptyList())
        val home = CollectionKind.Custom("Home")

        assertEquals(listOf(bills, trips), listed)
        assertEquals(listOf(bills, trips), page.remove(bills.kind).customTiles(listed))
        assertEquals(listOf(bills, trips, CollectionCard(home)), page.remove(bills.kind).add(home).customTiles(listed))
        assertEquals(listOf(bills), page.remove(trips.kind).customTiles(emptyList()))
    }

    @Test
    fun `an empty page is empty text, and empty text an empty page`() {
        assertEquals("", CollectionsPage(emptyList()).encode())
        assertEquals(CollectionsPage(emptyList()), decodeCollectionsPage(""))
    }

    @Test
    fun `a malformed line is skipped, and so is a second line for a kind`() {
        val text = "NewApps\t1\nBogus\t0\nMostUsed\nMostUsed\t2\nTools\t0\ta/A\t\nNewApps\t0\n\nMostUsed\t0\tb/B"

        assertEquals(
            listOf(CollectionCard(NewApps, expanded = true), CollectionCard(tools, Favourites(listOf("a/A"))), CollectionCard(MostUsed)),
            decodeCollectionsPage(text).cards,
        )
    }
}
