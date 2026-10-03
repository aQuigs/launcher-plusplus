package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.Ring
import com.sqftware.orbitlauncher.domain.RingPages
import com.sqftware.orbitlauncher.domain.RingSlot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RingPagesStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = SharedPreferencesRingPagesStore(context)

    @After
    fun emptyHome() = store.save(RingPages())

    @Test
    fun eachRingAndTheDockComeBackApart() {
        val ring = Ring(listOf(RingSlot.App("a/A"), RingSlot.Folder(listOf("b/B", "c/C")), RingSlot.Folder(emptyList())))
        val pages = RingPages(
            rings = mapOf(LauncherPage.Home.id to ring, "ring-2" to Ring(listOf(RingSlot.App("d/D")))),
            dock = Ring(listOf(RingSlot.App("c/C"), RingSlot.Folder(listOf("a/A", "d/D")))),
        )

        store.save(pages)

        assertEquals(pages, SharedPreferencesRingPagesStore(context).load())
    }

    @Test
    fun aPageNoLongerSavedIsForgotten() {
        store.save(RingPages(rings = mapOf("ring-2" to Ring(listOf(RingSlot.App("d/D"))))))

        store.save(RingPages(rings = mapOf("ring-3" to Ring(listOf(RingSlot.App("e/E"))))))

        assertEquals(setOf("ring-3"), SharedPreferencesRingPagesStore(context).load().rings.keys)
    }

    @Test
    fun aRingStoredBeforeTheDockLoadsAsTheHomeRing() {
        // The format the ring shipped with: one "favourites" string of keys, one per line.
        context.getSharedPreferences("home", Context.MODE_PRIVATE).edit(commit = true) {
            clear()
            putString("favourites", "a/A\nb/B")
        }

        assertEquals(Ring(listOf(RingSlot.App("a/A"), RingSlot.App("b/B"))), store.load().on(LauncherPage.Home.id).ring)
    }
}
