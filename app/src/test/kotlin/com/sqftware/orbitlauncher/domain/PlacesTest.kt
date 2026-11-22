package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlacesTest {
    @Test
    fun `an OpenStreetMap place is named by its name, else its street address, with its postal address`() {
        val cafe = OsmPlace("Blue Bottle Coffee", "705", "Market Street", "San Francisco", "California", "94103", "United States", 37.78, -122.40)
        val house = OsmPlace(houseNumber = "2", street = "Elm Street", city = "Springfield")

        assertEquals(
            PlaceValue("Blue Bottle Coffee", "705 Market Street, San Francisco, California 94103, United States", 37.78, -122.40),
            cafe.toPlace(),
        )
        assertEquals(PlaceValue("2 Elm Street", "2 Elm Street, Springfield"), house.toPlace())
        assertNull(OsmPlace(country = "United States").toPlace())
    }

    @Test
    fun `combined searches leave out a place an earlier search found, in the searches' order`() {
        val ferry = PlaceValue("Ferry Building", "1 Ferry Building, San Francisco", 37.7955, -122.3937)
        val sameFerry = PlaceValue("ferry building", "Ferry Building, San Francisco, CA", 37.7954, -122.3936)
        val otherFerry = PlaceValue("Ferry Building", "Ferry Building, Oakland", 37.79, -122.28)
        val nextDoor = PlaceValue("Safeway", "1 Main St", 37.7700, -122.4000)
        val nextDoorToo = PlaceValue("Safeway", "3 Main St", 37.7701, -122.4000)
        val typed = PlaceValue("Elm St", "Elm St")

        assertEquals(
            listOf(ferry, otherFerry, nextDoor, nextDoorToo, typed),
            combinedPlaces(listOf(listOf(ferry, otherFerry, nextDoor, nextDoorToo), listOf(sameFerry, typed), listOf(typed.copy()))),
        )
    }

    @Test
    fun `an address written out is told from a name`() {
        assertEquals(true, isWrittenAddress("Blue Bottle Coffee, 705 Market St, San Francisco"))
        assertEquals(true, isWrittenAddress("705 Market St"))
        assertEquals(false, isWrittenAddress("blue bottle coffee"))
    }
}
