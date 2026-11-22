package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIntentsTest {
    private val uber = app("Uber", "com.ubercab")
    private val lyft = app("Lyft", "me.lyft.android")
    private val mail = app("Mail")
    private val ride = IntentCatalog.forPackage(uber.packageName).single()
    private val lyftRide = IntentCatalog.forPackage(lyft.packageName).single()
    private val work = PlaceValue("Work | Office!", "1 Main St, Springfield", 37.5, -122.25)

    @Test
    fun `an app with intents offers to open as usual or straight to each, picked now or each time`() {
        assertEquals(
            listOf("Open as usual", "Ride to a place you pick now", "Ride to a place you pick each time"),
            openChoices(uber).map(::openChoiceLabel),
        )
        assertEquals(emptyList<OpenChoice>(), openChoices(mail))
        assertEquals(emptyList<OpenChoice>(), openChoices(uber.copy(workProfile = 10L)))
    }

    @Test
    fun `an icon opened to an intent is named after it and its saved input, and marks the choice that made it`() {
        val asking = openedAs(uber, OpenChoice.Intent(ride, asks = true))!!
        val saved = openedAs(uber, OpenChoice.Intent(ride, asks = false), work)!!

        assertEquals("Uber ride", asking.label)
        assertEquals("Uber ride: Work | Office!", saved.label)
        assertEquals(OpenChoice.Intent(ride, asks = true), currentChoice(asking))
        assertNull(currentChoice(saved))
        assertEquals(uber, openedAs(saved, OpenChoice.Usual))
        assertNull(openedAs(uber, OpenChoice.Intent(ride, asks = false)))
        assertNull(openedAs(mail, OpenChoice.Intent(ride, asks = true)))
    }

    @Test
    fun `an intent icon's key is storable, is no other kind of key, and resolves while its app is installed`() {
        val saved = openedAs(uber, OpenChoice.Intent(ride, asks = false), work)!!
        val asking = openedAs(uber, OpenChoice.Intent(ride, asks = true))!!
        val pasted = openedAs(uber, OpenChoice.Intent(ride, asks = false), PlaceValue("Eiffel Tower\nParis", "Av. Gustave Eiffel\nParis"))!!
        val apps = AppsByKey(listOf(uber, mail))

        listOf(saved, asking, pasted).forEach {
            assertTrue(isStorable(it.key))
            assertNull(pairKeys(it.key))
            assertEquals(it, apps[it.key])
        }
        assertFalse(saved.key == asking.key)
        assertNull(AppsByKey(listOf(mail))[saved.key])
    }

    @Test
    fun `a pair of intent icons resolves by key and asks for a place once`() {
        val pair = pairOf(openedAs(uber, OpenChoice.Intent(ride, true))!!, openedAs(lyft, OpenChoice.Intent(lyftRide, true))!!)!!

        assertEquals(pair, AppsByKey(listOf(uber, lyft))[pair.key])
        assertEquals(listOf(InputKind.Place), pair.asks)
        assertEquals(emptyList<InputKind>(), pairOf(uber, lyft)!!.asks)
    }

    @Test
    fun `changing how one of a pair's apps opens keeps the other`() {
        val pair = pairOf(uber, lyft)!!
        val lyftAsking = openedAs(lyft, OpenChoice.Intent(lyftRide, true))!!

        assertEquals(pairOf(uber, lyftAsking), pair.withMember(lyft, lyftAsking))
        assertEquals(lyftAsking, lyft.withMember(lyft, lyftAsking))
    }

    @Test
    fun `the ride links carry the destination, encoded, and the asked place only when answered`() {
        val asking = openedAs(uber, OpenChoice.Intent(ride, true))!!.kind as EntryKind.Intent

        assertNull(asking.request(emptyMap()))
        assertEquals(
            IntentRequest(
                "com.ubercab",
                "uber://?action=setPickup&pickup=my_location&dropoff[latitude]=37.5&dropoff[longitude]=-122.25" +
                    "&dropoff[nickname]=Work%20%7C%20Office%21&dropoff[formatted_address]=1%20Main%20St%2C%20Springfield",
            ),
            asking.request(mapOf(InputKind.Place to work)),
        )
        assertEquals(
            "lyft://ridetype?id=lyft&destination[address]=Elm%20St",
            lyftRide.request(PlaceValue("Elm St", "Elm St")).data,
        )
    }

    @Test
    fun `a place shared from a maps app searches by its name and address, not its link`() {
        assertEquals(
            "Ferry Building, 1 Ferry Building, San Francisco, CA",
            placeQueryFromShare("Ferry Building\n1 Ferry Building, San Francisco, CA\nhttps://maps.app.goo.gl/abc"),
        )
        assertNull(placeQueryFromShare("https://maps.app.goo.gl/abc"))
    }

    @Test
    fun `a place picked goes to the top of the recent ones, once`() {
        val home = PlaceValue("Home", "2 Elm St")
        val recent = List(RECENT_PLACES) { PlaceValue("$it", "$it Oak St") }

        assertEquals(listOf(home) + recent.dropLast(1), recent.withRecent(home))
        assertEquals(listOf(home, work), listOf(work, home).withRecent(home))
    }
}
