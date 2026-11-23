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
    private val waymoRide = IntentCatalog.forPackage("com.waymo.carapp").single()
    private val mapsDrive = IntentCatalog.byId("maps.drive")!!
    private val waze = IntentCatalog.byId("waze.drive")!!
    private val citymapper = IntentCatalog.byId("citymapper.directions")!!
    private val work = PlaceValue("Work | Office!", "1 Main St, Springfield", 37.5, -122.25)
    private val elm = PlaceValue("Elm St", "Elm St")

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
    fun `a pair of intent icons resolves by key and asks for each app's place on its own`() {
        val uberAsking = openedAs(uber, OpenChoice.Intent(ride, true))!!
        val lyftAsking = openedAs(lyft, OpenChoice.Intent(lyftRide, true))!!
        val pair = pairOf(uberAsking, lyftAsking)!!

        assertEquals(pair, AppsByKey(listOf(uber, lyft))[pair.key])
        assertEquals(listOf(InputQuestion(InputKind.Place, listOf(uberAsking)), InputQuestion(InputKind.Place, listOf(lyftAsking))), pair.asks)
        assertEquals(emptyList<InputQuestion>(), pairOf(uber, lyft)!!.asks)
    }

    @Test
    fun `one of a pair can share the other's prompt, only while that asks for the same kind of input itself`() {
        val uberAsking = openedAs(uber, OpenChoice.Intent(ride, true))!!
        val sharing = OpenChoice.Intent(lyftRide, asks = true, sharedWith = uber)
        val lyftSharing = openedAs(lyft, sharing)!!
        val pair = pairOf(uberAsking, lyftSharing)!!

        assertEquals("Ride to a place you pick for Uber", openChoiceLabel(openChoices(lyft, uberAsking).last()))
        assertEquals(sharing, openChoices(lyft, uberAsking).last())
        assertEquals(3, openChoices(lyft, uber).size)
        assertEquals(sharing, currentChoice(lyftSharing, uberAsking))
        assertEquals(pair, AppsByKey(listOf(uber, lyft))[pair.key])

        val question = pair.asks.single()
        assertEquals("Uber and Lyft", question.forWhom)
        assertEquals(
            pairOf(openedAs(uber, OpenChoice.Intent(ride, false), work)!!, openedAs(lyft, OpenChoice.Intent(lyftRide, false), work)!!),
            pair.answered(mapOf(question to work)),
        )
        assertEquals(listOf(InputQuestion(InputKind.Place, listOf(lyftSharing))), pairOf(uber, lyftSharing)!!.asks)
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
        val asking = openedAs(uber, OpenChoice.Intent(ride, true))!!
        val answered = asking.answered(mapOf(asking.asks.single() to work))!!

        assertNull((asking.kind as EntryKind.Intent).request())
        assertNull(asking.answered(emptyMap()))
        assertEquals(openedAs(uber, OpenChoice.Intent(ride, false), work), answered)
        assertEquals(
            IntentRequest(
                "com.ubercab",
                "uber://?action=setPickup&pickup=my_location&dropoff[latitude]=37.5&dropoff[longitude]=-122.25" +
                    "&dropoff[nickname]=Work%20%7C%20Office%21&dropoff[formatted_address]=1%20Main%20St%2C%20Springfield",
            ),
            (answered.kind as EntryKind.Intent).request(),
        )
        assertEquals(
            "lyft://ridetype?id=lyft&destination[address]=Elm%20St",
            lyftRide.request(elm).data,
        )
        assertEquals(
            IntentRequest(
                "com.waymo.carapp",
                "https://app.waymo.com/route/trip/create?pickup=my_location&dropoff_latitude=37.5&dropoff_longitude=-122.25",
            ),
            waymoRide.request(work),
        )
        assertEquals(
            "https://app.waymo.com/route/trip/create?pickup=my_location",
            waymoRide.request(elm).data,
        )
    }

    @Test
    fun `Maps directions go to the place's coordinates, or to its address without them`() {
        assertEquals(
            IntentRequest(
                "com.google.android.apps.maps",
                "https://www.google.com/maps/dir/?api=1&destination=37.5%2C-122.25&travelmode=driving&dir_action=navigate",
            ),
            mapsDrive.request(work),
        )
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=Elm%20St&travelmode=driving&dir_action=navigate",
            mapsDrive.request(elm).data,
        )
    }

    @Test
    fun `Waze navigates to the place's coordinates, or searches for its address without them`() {
        assertEquals("https://waze.com/ul?q=Work%20%7C%20Office%21&ll=37.5%2C-122.25&navigate=yes", waze.request(work).data)
        assertEquals("https://waze.com/ul?q=Elm%20St&navigate=yes", waze.request(elm).data)
    }

    @Test
    fun `Citymapper gets the place's coordinates, name and address`() {
        assertEquals(
            "citymapper://directions?endcoord=37.5%2C-122.25&endname=Work%20%7C%20Office%21&endaddress=1%20Main%20St%2C%20Springfield",
            citymapper.request(work).data,
        )
    }

    @Test
    fun `links write a coordinate near zero in plain decimals`() {
        val greenwich = PlaceValue("Greenwich", "Greenwich", 51.4778, -0.0001)

        assertEquals("https://waze.com/ul?q=Greenwich&ll=51.4778%2C-0.00010&navigate=yes", waze.request(greenwich).data)
        assertEquals(
            "https://app.waymo.com/route/trip/create?pickup=my_location&dropoff_latitude=51.4778&dropoff_longitude=-0.00010",
            waymoRide.request(greenwich).data,
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
